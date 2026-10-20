import java.util.Random;

public class Zobrist 
{
    private static final Random rng = new Random(123456789);

    public static final long[][] zobristPieceKeys = new long[12][64];
    public static final long zobristWhiteTurn = rng.nextLong();
    public static final long[] zobristCanCastle = new long[4];
    public static final long[] zobristEnPassantCol = new long[8];

    static
    {
        for (int i = 0; i < zobristPieceKeys.length; i++)
        {
            for (int j = 0; j < zobristPieceKeys[i].length; j++)
            {
                zobristPieceKeys[i][j] = rng.nextLong();
            }
        }
        for (int i = 0; i < zobristCanCastle.length; i++)
        {
            zobristCanCastle[i] = rng.nextLong();
        }
        for (int i = 0; i < zobristEnPassantCol.length; i++)
        {
            zobristEnPassantCol[i] = rng.nextLong();
        }
    }

    public static long makeZobristHash (Board board, boolean whiteTurn)
    {
        long zobristHash = 0;
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                Piece scanPiece = board.getPiece(row, col);
                if (!scanPiece.isOccupied()) continue;
                else zobristHash ^= zobristPieceKeys[scanPiece.ordinal()][(row * 8) + col];
            }
        }

        if (!whiteTurn) zobristHash ^= zobristWhiteTurn;

        if (board.whiteCanCastleLong) zobristHash ^=  zobristCanCastle[0];
        if (board.whiteCanCastleShort) zobristHash ^=  zobristCanCastle[1];
        if (board.blackCanCastleShort) zobristHash ^= zobristCanCastle[2];
        if (board.blackCanCastleLong) zobristHash ^= zobristCanCastle[3];

        int[] enPassantSquare = board.getEnPassantSquare();
        if (enPassantSquare != null) zobristHash ^= zobristEnPassantCol[enPassantSquare[1]];
        return zobristHash;
    }
}
