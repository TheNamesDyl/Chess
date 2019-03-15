package chess.bots;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class JamboreeSearcherWithSort<M extends Move<M>, B extends Board<M, B>> extends
        AbstractSearcher<M, B> {
    /**
     * Used to determine what % of moves should be handled in a sequential manner
     * (gets a rough but functional alpha value).
     */
    private static final double PERCENTAGE_SEQUENTIAL = 0.5;
    private static final int DIVIDE_CUTOFF = 3;
    private static final ForkJoinPool POOL = new ForkJoinPool();

    /**
     *
     * @param board
     *            the current board position.
     * @param myTime
     *            the remaining time on your clock.
     * @param opTime
     *            the remaining time on your opponent's clock.
     * @return
     *            The best move to make on the current board.
     */
    public M getBestMove(B board, int myTime, int opTime) {
        List<M> m = board.generateMoves();
        return POOL.invoke(new BestMoveTask<M, B>(board, m, evaluator, this.ply, 0, m.size(), false, false,
                    -evaluator.infty(), evaluator.infty())).move;

    }

    /** Recursive task to find the best move **/
    private class BestMoveTask<M extends  Move<M>, B extends  Board<M, B>> extends RecursiveTask<BestMove<M>> {

        B board;
        List<M> moves;
        Evaluator<B> evaluator;
        int levelsToGo;
        int lo, hi;
        boolean makeMove, makeCopy;
        int alpha, beta;

        /**
         * Creates a new BestMoveTask and finds the best move on the specified board.
         *
         * @param board
         *            The board to examine.
         * @param moves
         *            All available moves to make on the current board.
         * @param evaluator
         *            An object which returns the value of a board.
         * @param levelsToGo
         *            How much further to "look ahead" in the current game.
         * @param lo
         *            The (inclusive) beginning to inspect of moves.
         * @param hi
         *            The (exclusive) end to inspect of moves.
         * @param makeMove
         *            A flag for this task to make a move if {@code makeMove == true}.
         * @param alpha
         *            The value of the best move for the maximizer.
         * @param beta
         *            The value of the best move for the minimizer.
         */
        public BestMoveTask (B board, List<M> moves, Evaluator<B> evaluator, int levelsToGo, int lo, int hi,
                             boolean makeMove, boolean makeCopy, int alpha, int beta) {
            this.board      = board;
            this.moves      = moves;
            this.evaluator  = evaluator;
            this.levelsToGo = levelsToGo;
            this.lo         = lo;
            this.hi         = hi;
            this.makeMove   = makeMove;
            this.alpha      = alpha;
            this.beta       = beta;
            this.makeCopy   = makeCopy;
        }

        /**
         * "Makes" the move located at {@code this.moves.get(lo)}.
         */
        private void makeMove() {
            if (makeCopy) {
                this.board = board.copy();
            }
            board.applyMove(moves.get(lo));
            this.moves = board.generateMoves();
            MoveSorter.sortMoves(moves, board, evaluator);
            this.lo = 0;
            this.hi = moves.size();
        }

        /**
         * @return the proper value according to the type of check the board is in.
         */
        private BestMove<M> handleCheck() {
            if (board.inCheck()) {
                return new BestMove<M>(-evaluator.mate() - levelsToGo);
            } else {
                return new BestMove<M>(-evaluator.stalemate());
            }
        }

        /**
         * @return
         *            The best move to make on this thread's board.
         */
        @Override
        protected BestMove<M> compute() {
            if (makeMove) { // If this child thread was told to make a move, do so
                makeMove();
            }
            int moveSize = moves.size();
            if (moveSize == 0) { return  handleCheck(); } // if in check, return

            BestMove<M> bestMove = new BestMove<>(alpha);
            if (this.levelsToGo <= cutoff) { // base case
                BestMove val = AlphaBetaSearcher.alphabeta(alpha, beta, evaluator, board, levelsToGo);
                return val;
            }

            if (lo == 0) { // always do the first portion sequentially
                bestMove = percentSequentialCompute(bestMove, moveSize);
                lo = (int)(PERCENTAGE_SEQUENTIAL * moveSize);
            }


            if ((hi - lo) <= DIVIDE_CUTOFF) { // fork sequentially
                BestMove forkedValue = forkSequentially(moveSize);
                BestMove val = bestMove.value >= forkedValue.value ? bestMove : forkedValue;
                return val;
            } else { // divide and conquer
                BestMove conquerValue = divideAndConquer();
                BestMove val = bestMove.value >= conquerValue.value ? bestMove : conquerValue;
                return val;
            }
        }

        /**
         * Splits the work from [lo, hi) in half and makes new threads to do the work.
         *
         * @return
         */
        private BestMove<M> divideAndConquer() {
            int mid = lo + ((hi - lo) / 2);

            BestMoveTask<M, B> left  = new BestMoveTask<>
                    (board, moves, evaluator, levelsToGo, lo, mid, false, false, alpha, beta);
            BestMoveTask<M, B> right = new BestMoveTask<>
                    (board, moves, evaluator, levelsToGo, mid, hi, false, false, alpha, beta);


            left.fork();

            BestMove<M> rightBest = right.compute();
            BestMove<M> leftBest  = left.join();

            return (leftBest.value >= rightBest.value) ? leftBest : rightBest;
        }

        /**
         * Makes new threads and forks them sequentially.
         *
         * @param moveSize
         *            The size of the moveset being passed to the child.
         * @return
         *            The best move to make amongst the forked threads.
         */
        private BestMove<M> forkSequentially(int moveSize) {
            BestMoveTask<M, B>[] tasks = new BestMoveTask[hi - lo];

            for (int i = lo; i < hi; i++) {
                tasks[i - lo] = new BestMoveTask(board, moves, evaluator, levelsToGo - 1, i, moveSize,
                                                true, true, -beta, -alpha);

                if (i < hi - 1) {
                    tasks[i - lo].fork();
                }
            }

            BestMove<M> b = tasks[hi - lo - 1].compute().negate();
            b.move = moves.get(hi - 1);

            for (int i = 0; i < tasks.length - 1; i++) {
                BestMove<M> tmp = tasks[i].join().negate();
                if (tmp.value > b.value) {
                    b.value = tmp.value;
                    b.move = moves.get(i + lo);
                }
            }

            return b;
        }

        /**
         * Handles the PERCENTAGE_SEQUENTIAL portion of the work.
         * @param bestMove
         *            The current best move in the range.
         * @param moveSize
         *            The size of the moveset currently available.
         */
        private BestMove<M> percentSequentialCompute(BestMove<M> bestMove, int moveSize) {
            // The (exclusive) max range to sequentially evaluate to/
            int sequentialComputeRange = (int)(PERCENTAGE_SEQUENTIAL * moveSize);

            // Copy is an expensive operation
            B board = this.board.copy();
            for (int i = 0; i < sequentialComputeRange; i++) {
                BestMove move = null;
                BestMoveTask<M, B> task = new BestMoveTask<M, B>(board, moves, evaluator, levelsToGo - 1, i, moveSize,
                        true, false, -beta, -alpha);
                move = task.compute();
                board.undoMove();
                move.negate();

                if (move.value > alpha) {
                    bestMove.value = move.value;
                    bestMove.move = moves.get(i);
                    alpha = bestMove.value;
                }

                if (alpha >= beta) { // Pruning
                    return bestMove;
                }
            }

            return bestMove;
        }
    }
}