import java.util.Random;

public class Zobrist 
{
    private static final Random rng = new Random(123456789);

    public static final long[][] zobristPieceKeys = new long[12][64];
    public static final long zobristWhiteTurn = rng.nextLong();
    public static final long[] zobristCanCastle = new long[4];

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
                else zobristHash ^= zobristPieceKeys[scanPiece.pieceToIndex()][(row * 8) + col];

                if (!whiteTurn) zobristHash ^= zobristWhiteTurn;
            }
        }

        // whiet castling rights
        if (!board.getPiece(Board.findKing(board, true)).hasMoved())
        {
            // long castle
            Piece scanPiece = board.getPiece(7, 0);
            if (scanPiece.isWhite() && scanPiece.getPiece() == 'R' && !scanPiece.hasMoved()) zobristHash ^=  zobristCanCastle[0];
            // short castle
            scanPiece = board.getPiece(7, 7);
            if (scanPiece.isWhite() && scanPiece.getPiece() == 'R' && !scanPiece.hasMoved()) zobristHash ^=  zobristCanCastle[1];
        }

        // black castling rights
        if (!board.getPiece(Board.findKing(board, false)).hasMoved())
        {
            // long castle
            Piece scanPiece = board.getPiece(0, 0);
            if (!scanPiece.isWhite() && scanPiece.getPiece() == 'R' && !scanPiece.hasMoved()) zobristHash ^= zobristCanCastle[2];
            // short castle
            scanPiece = board.getPiece(0, 7);
            if (!scanPiece.isWhite() && scanPiece.getPiece() == 'R' && !scanPiece.hasMoved()) zobristHash ^= zobristCanCastle[3];
        }

        return zobristHash;
    }
}
