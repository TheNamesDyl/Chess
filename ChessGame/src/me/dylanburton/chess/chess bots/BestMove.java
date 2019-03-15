package chess.bots;

/** 
 * This class represents a move and its value.  It will
 * be necessary, because we need to return both the value
 * of a move and the actual move itself.
 */
public class BestMove<M> {
    public M move;
    public int value;
    
    public BestMove(int value) {
        this.value = value;
    }

    public BestMove(M move, int value) {
        this.move = move;
        this.value = value;
    }
    
    public BestMove<M> negate() {
        this.value = -this.value;
        return this;
    }
}
