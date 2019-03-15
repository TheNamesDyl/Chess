package chess.bots;

import cse332.chess.interfaces.*;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;


public class ParallelSearcher<M extends Move<M>, B extends Board<M, B>> extends
        AbstractSearcher<M, B> {
    private static final ForkJoinPool POOL = new ForkJoinPool();

    public M getBestMove(B board, int myTime, int opTime) {
        /* Calculate the best move
         * note: use of this.cutoff in place of this.ply */
        List<M> m = board.generateMoves();
        return POOL.invoke(new BestMoveTask<M, B>(board, m, evaluator, this.ply, 0, m.size(), false)).move;
    }

    /** Recursive task to find the best move **/
    private class BestMoveTask<M extends  Move<M>, B extends  Board<M, B>> extends RecursiveTask<BestMove<M>> {

        /** Determines when to sequentially fork. **/
        public static final int DIVIDE_CUTOFF = 3;

        B board;
        List<M> moves;
        Evaluator<B> evaluator;
        int levelsToGo;
        int lo, hi;
        boolean makeMove;



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
         */
        public BestMoveTask (B board, List<M> moves, Evaluator<B> evaluator, int levelsToGo, int lo, int hi, boolean makeMove) {
            this.board = board;
            this.moves = moves;
            this.evaluator = evaluator;
            this.levelsToGo = levelsToGo;
            this.lo = lo;
            this.hi = hi;
            this.makeMove = makeMove;
        }

        /**
         * @return
         *            The best move to make on this thread's board.
         */
        @Override
        protected BestMove<M> compute() {

            if (makeMove) {
                this.board = board.copy();
                board.applyMove(moves.get(lo));
                this.moves = board.generateMoves();
                this.lo = 0;
                this.hi = moves.size();
            }
            int moveSize = moves.size();
            if (moveSize == 0) { // checkmate
                if (board.inCheck()) {
                    return new BestMove<M>(-evaluator.mate() - levelsToGo);
                } else {
                    return new BestMove<M>(-evaluator.stalemate());
                }
            }

            if (this.levelsToGo <= cutoff) { /** evaluate sequentially **/
                return SimpleSearcher.minimax(evaluator, board, this.levelsToGo);
            } else if ((hi - lo) <= DIVIDE_CUTOFF) { /** fork sequentially **/

                BestMoveTask<M, B>[] tasks = new BestMoveTask[hi - lo];

                for (int i = lo; i < hi; i++) {
                    tasks[i - lo] = new BestMoveTask(board, moves, evaluator, levelsToGo - 1, i, moveSize, true);

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
            } else { /** divide and conquer **/
                int mid = lo + ((hi - lo) / 2);

                BestMoveTask<M, B> left  = new BestMoveTask<>
                        (board, moves, evaluator, levelsToGo, lo, mid, false);
                BestMoveTask<M, B> right = new BestMoveTask<>
                        (board, moves, evaluator, levelsToGo, mid, hi, false);

                left.fork();

                BestMove<M> rightBest = right.compute();
                BestMove<M> leftBest  = left.join();

                return (leftBest.value >= rightBest.value) ? leftBest : rightBest;
            }
        }
    }
}