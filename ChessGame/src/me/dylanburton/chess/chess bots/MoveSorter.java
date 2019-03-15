package chess.bots;

import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class MoveSorter {
    public static<M extends Move<M>, B extends Board<M, B>> List<M> sortMoves(List<M> moves, B board, Evaluator<B> evaluator) {
        Collections.sort(moves, (M m1, M m2) -> Boolean.compare(m2.isCapture(), m1.isCapture()));
        return moves;
    }
}
