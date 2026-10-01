/* 
Half A: Zobrist hashing

The idea: give every (piece, square) combination a random 64-bit number. 
A position's hash is all of the numbers for what's on the board XOR'd together. 
Two different positions almost never end up with the same hash, and comparing two 
longs is way faster than comparing 70-character strings.

1. The random tables (static in Bot, or in a new Zobrist class if you want it tidy):

- long[12][64]: 12 piece kinds (6 types × 2 colors) × 64 squares
- one long for "black to move"
- long[4] for the castling rights (white short, white long, black short, black long)

Fill them once using java.util.Random with a fixed seed, like new Random(12345), and .nextLong(). 
A fixed seed means the same hashes every run, which makes debugging way easier.

You also need a small helper that turns a piece into its index 0-11, like white P=0, N=1, B=2, R=3, Q=4, K=5, 
then black 6-11. Squares are row * 8 + col, the same as the history table.

2. computeHash(board, whiteTurn): basically your makePositionKey, but with ^= instead of append:

- for each occupied square, hash ^= pieceKeys[index][square]
- if it's black's turn, XOR in the side key
- for each castling right that's available, XOR in its key, using exactly the same castling logic you already wrote for 
the string key

3. Swap it into repetition detection:

- Game.positionKeys becomes ArrayList<Long>
- plyPathKeys becomes long[]
- compare with == now (they're primitives, so that works, unlike arrays and Strings)
*/

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
