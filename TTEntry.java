public class TTEntry 
{
    public static final int EXACT = 1;
    public static final int LOWER = 2;
    public static final int UPPER = 3;
    public static final int TT_SIZE = 1 << 20;

    public long key;
    public int depth;
    public int score;
    public int flag;
    public int[] bestmove;

    public static TTEntry[] ttTable = new TTEntry[TT_SIZE];
    
    public TTEntry(long key, int depth, int score, int flag, int[] bestmove)
    {
        this.key = key; 
        this.depth = depth;
        this.score = score;
        this.flag = flag;
        this.bestmove = bestmove;
    }
    
    
}
