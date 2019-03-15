package chess.bots;

import java.util.List;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Move;

/**
 * An example searcher to get you started.
 */
public class LazySearcher<M extends Move<M>, B extends Board<M,B>> extends AbstractSearcher<M,B> {
    
    public M getBestMove(B board, int myTime, int opTime) {
        List<M> moves = board.generateMoves();
        
        BestMove<M> best = null;

        for (M move : moves) {   
            reportNewBestMove(move);
            best = new BestMove<M>(move, evaluator.infty());
            break; /* Choose the first move we look at!  We're lucky... */
        }

        if (best == null) {
            return null;
        }
        return best.move;
    }
}
