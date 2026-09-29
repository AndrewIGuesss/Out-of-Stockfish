import java.util.*;

public class Moves 
{
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
        
        int[] diagonalCaptures = new int[]{-1, 1};
        
        for (int diagonal : diagonalCaptures)
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
        boolean[][] moves = new boolean[8][8];
        Piece rook = board.getPiece(square);
        
        int[] newSquare = new int[2];
        int row = square[0];
        int col = square[1];
        
        int[] rDir = new int[]{1, -1, 0, 0};
        int[] cDir = new int[]{0, 0, -1, 1};
        
        for(int i = 0; i < 4; i++)
        {
            int j = 0;
            while (true)
            {
                j++;
                newSquare[0] = row + rDir[i] * j;
                newSquare[1] = col + cDir[i] * j;
                
                if (board.inBounds(newSquare[0], newSquare[1]))
                {
                    Piece target = board.getPiece(newSquare);
                    if (target.isOccupied())
                    {
                        if (target.isWhite() != rook.isWhite()) moves[newSquare[0]][newSquare[1]] = true;
                        break;
                    }
                    else moves[newSquare[0]][newSquare[1]] = true;
                }
                else break;
            }
        }
        return moves;
    }
    
    
    public static boolean[][] bishopMoves(Board board, int[] square)
    {
        boolean[][] moves = new boolean[8][8];
        Piece bishop = board.getPiece(square);
        
        int[] newSquare = new int[2];
        int row = square[0];
        int col = square[1];
        
        int[] rDir = new int[]{1, -1, 1, -1};
        int[] cDir = new int[]{1, -1, -1, 1};
        
        for (int i = 0; i < 4; i++)
        {
            int j = 0;
            while (true)
            {
                j++;
                newSquare[0] = row + rDir[i] * j;
                newSquare[1] = col + cDir[i] * j;
                
                if (board.inBounds(newSquare[0], newSquare[1]))
                {
                    Piece target = board.getPiece(newSquare);
                    if (target.isOccupied())
                    {
                        if (target.isWhite() != bishop.isWhite()) { moves[newSquare[0]][newSquare[1]] = true; break; }
                        else break;
                    }
                    else moves[newSquare[0]][newSquare[1]] = true;
                }
                else break;
            }
        }
        return moves;
    }
    
    
    public static boolean[][] queenMoves(Board board, int[] square)
    {
        boolean[][] moves = rookMoves(board, square);
        boolean[][] bishopMoves = bishopMoves(board, square);
        
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++)
                if (bishopMoves[row][col]) moves[row][col] = true;

        return moves;
    }
    
    
    public static boolean[][] kingMoves(Board board, int[] square)
    {
        boolean[][] moves = new boolean[8][8];
        Piece king = board.getPiece(square);
        
        int[] newSquare = new int[2];
        int row = square[0];
        int col = square[1];
        
        int[] rDir = new int[]{1, -1, 1, -1, 1, -1, 0, 0};
        int[] cDir = new int[]{1, -1, -1, 1, 0, 0, 1, -1};
        
        for(int i = 0; i < 8; i++)
        {
            newSquare[0] = row + rDir[i];
            newSquare[1] = col + cDir[i];
            if (board.inBounds(newSquare[0], newSquare[1]))
            {
                Piece target = board.getPiece(newSquare);
                if (target.isOccupied())
                {
                    if (target.isWhite() != king.isWhite())
                    {
                        if (!inCheck(board, newSquare, king.isWhite())) moves[row + rDir[i]][col + cDir[i]] = true;
                    }
                }
                else if (!inCheck(board, newSquare, king.isWhite())) moves[row + rDir[i]][col + cDir[i]] = true;
            }
        }
        
        // castling 
        
        if (!inCheck(board, square, king.isWhite()))
        {
            if (king.isWhite())
            {
                // short castle
                if (!board.getPiece(new int[]{7,7}).hasMoved() && !board.getPiece(new int[]{7,4}).hasMoved())
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
                // long castle
                if(!board.getPiece(new int[]{7,0}).hasMoved() && !board.getPiece(new int[]{7,4}).hasMoved())
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
                // short castle
                if (!board.getPiece(new int[]{0,7}).hasMoved() && !board.getPiece(new int[]{0,4}).hasMoved())
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
                // long castle
                if(!board.getPiece(new int[]{0,0}).hasMoved() && !board.getPiece(new int[]{0,4}).hasMoved())
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
        int row = square[0];
        int col = square[1];
        int[] newSquare = new int[2];
    
        // diagonal check       
        // bishop/queen
        int[] diagR = new int[]{1, -1, 1, -1};
        int[] diagC = new int[]{1, -1, -1, 1};
    
        for (int i = 0; i < 4; i++)
        {
            int j = 0;
            while (true)
            {
                j++;
                newSquare[0] = row + diagR[i] * j;
                newSquare[1] = col + diagC[i] * j;
    
                if (board.inBounds(newSquare[0], newSquare[1]))
                {
                    Piece target = board.getPiece(newSquare);
                    if (target.isOccupied())
                    {
                        if (target.getPiece() == 'B' || target.getPiece() == 'Q')
                        {
                            if (target.isWhite() != isWhite) return true;
                            else break;
                        }
                        else break;
                    }
                }
                else break;
            }
        }
    
        // vertical/horizontal
        // rook/queen
        int[] straightR = new int[]{0, 0, 1, -1};
        int[] straightC = new int[]{1, -1, 0, 0};
    
        for (int i = 0; i < 4; i++)
        {
            int j = 0;
            while (true)
            {
                j++;
                newSquare[0] = row + straightR[i] * j;
                newSquare[1] = col + straightC[i] * j;
    
                if (board.inBounds(newSquare[0], newSquare[1]))
                {
                    Piece target = board.getPiece(newSquare);
                    if (target.isOccupied())
                    {
                        if (target.getPiece() == 'R' || target.getPiece() == 'Q')
                        {
                            if (target.isWhite() != isWhite) return true;
                            else break;
                        }
                        else break;
                    }
                }
                else break;
            }
        }
        
        // pawn check
        int pawnR = isWhite? -1 : 1;
        int[] pawnC = new int[]{-1, 1};
        
        for (int dir : pawnC)
        {
            newSquare[0] = row + pawnR;
            newSquare[1] = col + dir;
            
            if (board.inBounds(newSquare[0], newSquare[1]))
            {
                Piece target = board.getPiece(newSquare);
                
                if (target.isOccupied() && target.getPiece() == 'P')
                {
                    if (target.isWhite() != isWhite) return true;
                }
            }
        }
        
        // knight check
        int[] knightR = new int[]{1, -1, 2, -2, 2, -2, -1, 1};
        int[] knightC = new int[]{-2, -2,-1, -1, 1, 1,  2, 2};
        
        for (int i = 0; i < knightC.length; i++)
        {
            newSquare[0] = row + knightR[i];
            newSquare[1] = col + knightC[i];
            
            if (board.inBounds(newSquare[0], newSquare[1]))
            {
                Piece target = board.getPiece(newSquare);
                
                if (target.isOccupied() && target.getPiece() == 'N')
                {
                    if (target.isWhite() != isWhite) return true;
                }
            }
        }
        
        // king check
        int[] kingR = new int[]{1, -1, 1, -1, 1, -1, 0, 0};
        int[] kingC = new int[]{1, -1, -1, 1, 0, 0, 1, -1};
        
        for (int i = 0; i < kingC.length; i++)
        {
            newSquare[0] = row + kingR[i];
            newSquare[1] = col + kingC[i];
            
            if (board.inBounds(newSquare[0], newSquare[1]))
            {
                Piece target = board.getPiece(newSquare);
                
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

    public static ArrayList<int[]> allMoves(Board board, boolean white)
    {
        ArrayList<int[]> moves = new ArrayList<int[]>();
        
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
    
    public static ArrayList<int[]> pseudoMoves(Board board, boolean white)
    {
        ArrayList<int[]> moves = new ArrayList<int[]>();
        
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