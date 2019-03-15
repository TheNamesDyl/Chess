package chess.bots;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;

import java.util.List;

public class AlphaBetaSearcherWithSort<M extends Move<M>, B extends Board<M, B>> extends AbstractSearcher<M, B> {

    public M getBestMove(B board, int myTime, int opTime) {
        /* Calculate the best move */
        return alphabeta(-evaluator.infty(), evaluator.infty(), this.evaluator, board, this.ply).move;
    }

    static <M extends Move<M>, B extends Board<M, B>> BestMove<M> alphabeta(int alpha, int beta, Evaluator<B> evaluator, B board, int levelsToGo) {
        if (levelsToGo == 0) {
            return new BestMove<M>(evaluator.eval(board));
        }
        List<M> moves = board.generateMoves();
        BestMove<M> bestMove = new BestMove<M>(alpha);
        if (moves.isEmpty()) {
            if (board.inCheck()) {
                return new BestMove<M>(- evaluator.mate() - levelsToGo);
            } else {
                return new BestMove<M>(- evaluator.stalemate());
            }
        } else {
            MoveSorter.sortMoves(moves, board, evaluator);

            for (M move : moves) {
                board.applyMove(move);
                BestMove<M> value = alphabeta(-beta, -alpha, evaluator, board, levelsToGo - 1).negate();
                board.undoMove();

                if (value.value > bestMove.value) {
                    bestMove.value = value.value;
                    bestMove.move = move;
                }

                if (bestMove.value > alpha) {
                    alpha = bestMove.value;
                }

                if (beta <= alpha) {
                    return bestMove;
                }
            }
        }
        return bestMove;
    }
}