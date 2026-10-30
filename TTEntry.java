import java.util.Arrays;

public record TTEntry(long key, int depth, int score, int flag, int bestmove) 
{
    public static final int EXACT = 1;
    public static final int LOWER = 2;
    public static final int UPPER = 3;
    public static final int TT_SIZE = 1 << 20;

    public static TTEntry[] ttTable = new TTEntry[TT_SIZE];

    public static void clear()
    {
        Arrays.fill(ttTable, null);
    }
}

