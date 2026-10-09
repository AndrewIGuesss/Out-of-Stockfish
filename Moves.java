import java.util.*;

public class Moves 
{
    private static final int[] STRAIGHT_R = {1, -1,  0, 0};
    private static final int[] STRAIGHT_C = {0,  0, -1, 1};

    private static final int[] DIAG_R = {1, -1,  1, -1};
    private static final int[] DIAG_C = {1, -1, -1,  1};

    private static final int[] QUEEN_R = {1, -1,  0, 0, 1, -1,  1, -1};
    private static final int[] QUEEN_C = {0,  0, -1, 1, 1, -1, -1,  1};

    // single step directions
    private static final int[] KNIGHT_R = { 1, -1,  2, -2, 2, -2, -1, 1};
    private static final int[] KNIGHT_C = {-2, -2, -1, -1, 1,  1,  2, 2};

    private static final int[] KING_R = {1, -1,  1, -1, 1, -1, 0,  0};
    private static final int[] KING_C = {1, -1, -1,  1, 0,  0, 1, -1};

    private static final int[] PAWN_CAPTURE_C = {-1, 1};

    public static boolean[][] dirMoveScan(Board board, int[] square, int[] rDir, int[] cDir)
    {   
        boolean[][] moves = new boolean[8][8];
        
        int r = square[0];
        int c = square[1];
        
        int forward = 1;
        for (int i = 0; i < rDir.length; i++)
        {
            while (true)
            {
                forward++;
                int row = r + rDir[i] * forward;
                int col = c + cDir[i] * forward;
                
                if (board.inBounds(row, col))
                {
                    if (board.getPiece(row, col).isOccupied())
                    {
                        if (board.getPiece(row, col).isWhite() != board.getPiece(square).isWhite()) moves[row][col] = true;
                        break;
                    }
                    else moves[row][col] = true;
                }
                else break;
            }
        }
        return moves;
    }

    public static Piece dirPieceScan(Board board, int[] square, int rDir, int cDir)
    {   
        int r = square[0];
        int c = square[1];
        
        int forward = 1;
        while (true)
        {
            forward++;
            int row = r + rDir * forward;
            int col = c + cDir * forward;
                
            if (board.inBounds(row, col))
            {
                if (board.getPiece(row, col).isOccupied())
                {
                    return board.getPiece(row, col);
                }
            }
            else break;
        }
        return Piece.EMPTY;
    }

    public static boolean[][] pawnMoves(Board board, int[] square)
    {
        boolean[][] moves = new boolean[8][8];
        Piece pawn = board.getPiece(square);
        
        int dir = pawn.isWhite()? -1 : 1;
        int startRow = pawn.isWhite()? 6 : 1;
    
        int row = square[0];
        int col = square[1];
    
        int[] newSquare = new int[2];
        newSquare[0] = row + dir;
        newSquare[1] = col;
        
        if (board.inBounds(row + dir, col) && !board.getPiece(newSquare).isOccupied())
        {
            moves[row + dir][col] = true;
            
            newSquare[0] = row + (2 * dir);
            
            if (row == startRow && !board.getPiece(newSquare).isOccupied()) moves[newSquare[0]][newSquare[1]] = true;
        }
        
        for (int diagonal : PAWN_CAPTURE_C)
        {
            if (board.inBounds(row + dir, col + diagonal))
            {
                newSquare[0] = row + dir;
                newSquare[1] = col + diagonal;
            
                Piece target = board.getPiece(newSquare);
                
                // diagonal capture
                if (target.isOccupied() && target.isWhite() != pawn.isWhite()) moves[newSquare[0]][newSquare[1]] = true;
                
                // en passant capturing
                int[] ep = board.getEnPassantSquare(); // en passant square
                if (ep != null && ep[0] == newSquare[0] && ep[1] == newSquare[1]) moves[newSquare[0]][newSquare[1]] = true;
            }   
        }   
        
        return moves;
    }
    
    
    public static boolean[][] knightMoves(Board board, int[] square)
    {
        boolean[][] moves = new boolean[8][8];
        Piece knight = board.getPiece(square);
        
        int[] rDir = new int[]{1, -1, 2, -2, 2, -2, -1, 1};
        int[] cDir = new int[]{-2, -2,-1, -1, 1, 1,  2, 2};
        
        int[] newSquare = new int[2];
        int row = square[0];
        int col = square[1];
        
        for (int i = 0; i < cDir.length; i++)
        {
            newSquare[0] = row + rDir[i];
            newSquare[1] = col + cDir[i];
            
            if (board.inBounds(newSquare[0], newSquare[1]))
            {
                Piece target = board.getPiece(newSquare);
                if (target.isOccupied())
                {
                    if (target.isWhite() != knight.isWhite()) moves[newSquare[0]][newSquare[1]] = true;
                }
                else moves[newSquare[0]][newSquare[1]] = true;
            }
        }
        return moves;
    }
    
    
    public static boolean[][] rookMoves(Board board, int[] square)
    {
        return dirMoveScan(board, square, STRAIGHT_R, STRAIGHT_C);
    }
    
    
    public static boolean[][] bishopMoves(Board board, int[] square)
    {
        return dirMoveScan(board, square, DIAG_R, DIAG_C);
    }
    
    
    public static boolean[][] queenMoves(Board board, int[] square)
    {
        return dirMoveScan(board, square, QUEEN_R, QUEEN_C);
    }
    
    
    public static boolean[][] kingMoves(Board board, int[] square)
    {
        boolean[][] moves = new boolean[8][8];
        Piece king = board.getPiece(square);
        
        int[] newSquare = new int[2];
        int row = square[0];
        int col = square[1];
        
        for(int i = 0; i < 8; i++)
        {
            newSquare[0] = row + KING_R[i];
            newSquare[1] = col + KING_C[i];
            if (board.inBounds(newSquare[0], newSquare[1]))
            {
                Piece target = board.getPiece(newSquare);
                if (target.isOccupied())
                {
                    if (target.isWhite() != king.isWhite())
                    {
                        if (!inCheck(board, newSquare, king.isWhite())) moves[row + KING_R[i]][col + KING_C[i]] = true;
                    }
                }
                else if (!inCheck(board, newSquare, king.isWhite())) moves[row + KING_R[i]][col + KING_C[i]] = true;
            }
        }
        
        // castling 
        
        if (!inCheck(board, square, king.isWhite()))
        {
            if (king.isWhite())
            {
                if (board.whiteCanCastleShort)
                {
                    // peice in the way check
                    if (!board.getPiece(new int[]{7,5}).isOccupied() && !board.getPiece(new int[]{7,6}).isOccupied())
                    {
                        if (!inCheck(board, new int[]{7,5}, true) && !inCheck(board, new int[]{7,6}, true))
                        {
                            if (board.getPiece(new int[]{7,4}).getPiece() == 'K' && board.getPiece(new int[]{7,7}).getPiece() == 'R') moves [7][6] = true;
                        }
                    }
                }
                if(board.whiteCanCastleLong)
                {
                    // peice in the way check
                    if (!board.getPiece(new int[]{7,1}).isOccupied() && !board.getPiece(new int[]{7,2}).isOccupied() && !board.getPiece(new int[]{7,3}).isOccupied())
                    {
                        if (!inCheck(board, new int[]{7,2}, true) && !inCheck(board, new int[]{7,3}, true))
                        {
                            if (board.getPiece(new int[]{7,4}).getPiece() == 'K' && board.getPiece(new int[]{7,0}).getPiece() == 'R') moves [7][2] = true;
                        }
                    }
                }
            }
            else
            {
                if (board.blackCanCastleShort)
                {
                    // peice in the way check
                    if (!board.getPiece(new int[]{0,5}).isOccupied() && !board.getPiece(new int[]{0,6}).isOccupied())
                    {
                        if (!inCheck(board, new int[]{0,5}, false) && !inCheck(board, new int[]{0,6}, false))
                        {
                            if (board.getPiece(new int[]{0,4}).getPiece() == 'K' && board.getPiece(new int[]{0,7}).getPiece() == 'R') moves [0][6] = true;
                        }
                    }
                }
                if (board.blackCanCastleLong)
                {
                    // peice in the way check
                    if (!board.getPiece(new int[]{0,1}).isOccupied() && !board.getPiece(new int[]{0,2}).isOccupied() && !board.getPiece(new int[]{0,3}).isOccupied())
                    {
                        if (!inCheck(board, new int[]{0,2}, false) && !inCheck(board, new int[]{0,3}, false))
                        {
                            if (board.getPiece(new int[]{0,4}).getPiece() == 'K' && board.getPiece(new int[]{0,0}).getPiece() == 'R') moves [0][2] = true;
                        }
                    }
                }
            }
        }
        return moves;
    }
    
    
    public static boolean inCheck(Board board, int[] square, boolean isWhite)
    {
    
        // diagonal check       
        // bishop/queen
        for (int i = 0; i < DIAG_C.length; i++)
        {
            Piece scanPiece = dirPieceScan(board, square, DIAG_R[i], DIAG_C[i]);
            if (scanPiece.isWhite() != isWhite && (scanPiece.getPiece() == 'Q' || scanPiece.getPiece() == 'B')) return true;
        }
    
        // vertical/horizontal
        // rook/queen
        for (int i = 0; i < STRAIGHT_C.length; i++)
        {
            Piece scanPiece = dirPieceScan(board, square, STRAIGHT_R[i], STRAIGHT_C[i]);
            if (scanPiece.isWhite() != isWhite && (scanPiece.getPiece() == 'Q' || scanPiece.getPiece() == 'B')) return true;
        }
        
        // pawn check
        int pawnR = isWhite? -1 : 1;
        
        for (int dir : PAWN_CAPTURE_C)
        {
            int r = square[0] + pawnR;
            int c = square[1] + dir;
            
            if (board.inBounds(r, c))
            {
                Piece target = board.getPiece(r, c);
                
                if (target.isOccupied() && target.getPiece() == 'P')
                {
                    if (target.isWhite() != isWhite) return true;
                }
            }
        }
        
        // knight check
        for (int i = 0; i < KNIGHT_C.length; i++)
        {
            int r = square[0] + KNIGHT_R[i];
            int c = square[1] + KNIGHT_C[i];
            
            if (board.inBounds(r, c))
            {
                Piece target = board.getPiece(r, c);
                
                if (target.isOccupied() && target.getPiece() == 'N')
                {
                    if (target.isWhite() != isWhite) return true;
                }
            }
        }
        
        // king check
        for (int i = 0; i < KING_C.length; i++)
        {
            int r = square[0] + KING_R[i];
            int c = square[1] + KING_C[i];
            
            if (board.inBounds(r, c))
            {
                Piece target = board.getPiece(r, c);
                
                if (target.isOccupied() && target.getPiece() == 'K')
                {
                    if (target.isWhite() != isWhite) return true;
                }
            }
        }
        return false;
    }
    
    public static boolean hasNoLegalMoves(Board board, boolean white)
    {
        for (int row = 0; row < 8; row++) // finds every piece
        {
            for (int col = 0; col < 8; col++)
            {
                if (board.getPiece(row, col).isOccupied() && board.getPiece(row, col).isWhite() == white)
                {   
                    int[] square = new int[]{row, col};
                    boolean[][] tempBoard = board.validMoves(square, true);
                    
                    for (int i = 0; i < 8; i++)
                    {
                        for (int j = 0; j < 8; j++)
                        {
                            if (tempBoard[i][j]) return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static List<int[]> allMoves(Board board, boolean white)
    {
        List<int[]> moves = new ArrayList<int[]>();
        
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                Piece p = board.getPiece(row, col);
                if (p.isOccupied() && p.isWhite() == white)
                {
                    boolean[][] legal = board.validMoves(new int[]{row, col}, true);
                    
                    for (int endRow = 0; endRow < 8; endRow++)
                    {
                        for (int endCol = 0; endCol < 8; endCol++)
                        {
                            if (legal[endRow][endCol]) moves.add(new int[]{row, col, endRow, endCol});
                        }
                    }
                }
            }
        }
        return moves;
    }
    
    public static List<int[]> pseudoMoves(Board board, boolean white)
    {
        List<int[]> moves = new ArrayList<int[]>();
        
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                Piece p = board.getPiece(row, col);
                if (p.isOccupied() && p.isWhite() == white)
                {
                    boolean[][] legal = board.preValidMoves(new int[]{row, col}, true);
                    
                    for (int endRow = 0; endRow < 8; endRow++)
                    {
                        for (int endCol = 0; endCol < 8; endCol++)
                        {
                            if (legal[endRow][endCol]) moves.add(new int[]{row, col, endRow, endCol});
                        }
                    }
                }
            }
        }
        return moves;
    }
}