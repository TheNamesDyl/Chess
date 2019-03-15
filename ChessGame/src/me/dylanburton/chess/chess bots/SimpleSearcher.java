package chess.bots;

import cse332.chess.interfaces.*;

import java.util.List;

/**
 * This class should implement the minimax algorithm as described in the
 * assignment handouts.
 */
public class SimpleSearcher<M extends Move<M>, B extends Board<M, B>> extends
        AbstractSearcher<M, B> {

    public M getBestMove(B board, int myTime, int opTime) {
        /* Calculate the best move */
        return minimax(this.evaluator, board, this.ply).move;
    }

    static <M extends Move<M>, B extends Board<M, B>> BestMove<M> minimax(Evaluator<B> evaluator, B board, int levelsToGo) {
        if (levelsToGo == 0) {
            return new BestMove<>(evaluator.eval(board));
        }
        List<M> moves = board.generateMoves();
        BestMove<M> bestMove = new BestMove(-evaluator.infty());
        if (moves.isEmpty()) {
            if (board.inCheck()) {
                return new BestMove<>(-evaluator.mate() - levelsToGo);
            } else {
                return new BestMove<>(-evaluator.stalemate());
            }
        } else {
            for (M move : moves) {
                board.applyMove(move);
                BestMove value = minimax(evaluator, board, levelsToGo - 1).negate();
                board.undoMove();
                if (value.value > bestMove.value) {
                    bestMove = value;
                    bestMove.move = move;
                }
            }
        }
        return bestMove;
    }

}