import java.util.*;

public class Moves 
{
    public static final int[] STRAIGHT_R = {1, -1,  0, 0};
    public static final int[] STRAIGHT_C = {0,  0, -1, 1};

    public static final int[] DIAG_R = {1, -1,  1, -1};
    public static final int[] DIAG_C = {1, -1, -1,  1};

    public static final int[] QUEEN_R = {1, -1,  0, 0, 1, -1,  1, -1};
    public static final int[] QUEEN_C = {0,  0, -1, 1, 1, -1, -1,  1};

    // single step directions
    public static final int[] KNIGHT_R = { 1, -1,  2, -2, 2, -2, -1, 1};
    public static final int[] KNIGHT_C = {-2, -2, -1, -1, 1,  1,  2, 2};

    public static final int[] KING_R = {1, -1,  1, -1, 1, -1, 0,  0};
    public static final int[] KING_C = {1, -1, -1,  1, 0,  0, 1, -1};

    public static final int[] PAWN_CAPTURE_C = {-1, 1};


    // squares are 0-63 (row * 8 + col): start square * 100 + end square
    public static final int NO_MOVE = 0;
    public static final int MAX_MOVES = 256;

    // row col int[] per square so search doesnt need a new one each time
    public static final int[][] SQUARES = new int[64][];
    static
    {
        for (int square = 0; square < 64; square++) SQUARES[square] = new int[]{square / 8, square % 8};
    }

    public static int coords(int row, int col) { return (row * 8) + col; }
    public static int coords(int startRow, int startCol, int endRow, int endCol) { return (coords(startRow, startCol) * 100) + coords(endRow, endCol); }
    public static int startSquare(int move) { return move / 100; }
    public static int endSquare(int move) { return move % 100; }

    public static int[] coordsEnd (int num) { return SQUARES[num % 100]; }
    public static int[] coordsStart (int num) { return SQUARES[num / 100]; }
    public static int[] coordsStartEnd (int num)
    {
        int start = num / 100;
        int end = num % 100;
        return (new int[]{start / 8, start % 8, end / 8, end % 8});
    }

    public static int dirMoveScan(Board board, int row, int col, int[] rDir, int[] cDir, int[] moves, int count)
    {   
        boolean isWhite = board.getPiece(row, col).isWhite();
        
        int forward;
        for (int i = 0; i < rDir.length; i++)
        {
            forward = 0;
            while (true)
            {
                forward++;
                int newRow = row + rDir[i] * forward;
                int newCol = col + cDir[i] * forward;
                
                if (board.inBounds(newRow, newCol))
                {
                    Piece target = board.getPiece(newRow, newCol);
                    if (target.isOccupied())
                    {
                        if (target.isWhite() != isWhite) moves[count++] = coords(row, col, newRow, newCol);
                        break;
                    }
                    else moves[count++] = coords(row, col, newRow, newCol);
                }
                else break;
            }
        }
        return count;
    }

    public static Piece dirPieceScan(Board board, int[] square, int rDir, int cDir)
    {   
        int r = square[0];
        int c = square[1];
        
        int forward = 0;
        while (true)
        {
            forward++;
            int row = r + rDir * forward;
            int col = c + cDir * forward;
                
            if (board.inBounds(row, col))
            {
                Piece target = board.getPiece(row, col);
                if (target.isOccupied())
                {
                    return target;
                }
            }
            else break;
        }
        return Piece.EMPTY;
    }

    public static int pieceMoves(Board board, int row, int col, int[] moves, int count)
    {
        switch (board.getPiece(row, col).getPiece())
        {
            case 'P': return pawnMoves(board, row, col, moves, count);
            case 'N': return knightMoves(board, row, col, moves, count);
            case 'B': return dirMoveScan(board, row, col, DIAG_R, DIAG_C, moves, count);
            case 'R': return dirMoveScan(board, row, col, STRAIGHT_R, STRAIGHT_C, moves, count);
            case 'Q': return dirMoveScan(board, row, col, QUEEN_R, QUEEN_C, moves, count);
            case 'K': return kingMoves(board, row, col, moves, count);
        }
        return count;
    }

    public static int pawnMoves(Board board, int row, int col, int[] moves, int count)
    {
        Piece pawn = board.getPiece(row, col);
        
        int dir = pawn.isWhite()? -1 : 1;
        int startRow = pawn.isWhite()? 6 : 1;
    
        int newRow = row + dir;
        
        if (board.inBounds(newRow, col) && !board.getPiece(newRow, col).isOccupied())
        {
            moves[count++] = coords(row, col, newRow, col);
            
            newRow = row + (2 * dir);
            
            if (row == startRow && !board.getPiece(newRow, col).isOccupied()) moves[count++] = coords(row, col, newRow, col);
        }
        
        // captures
        int newCol;
        int[] ep = board.getEnPassantSquare();
        for (int diagonal : PAWN_CAPTURE_C)
        {
            if (board.inBounds(row + dir, col + diagonal))
            {
                newRow = row + dir;
                newCol = col + diagonal;
            
                Piece target = board.getPiece(newRow, newCol);
                
                // diagonal
                if (target.isOccupied() && target.isWhite() != pawn.isWhite()) moves[count++] = coords(row, col, newRow, newCol);
                
                // en passant
                if (ep != null && ep[0] == newRow && ep[1] == newCol) moves[count++] = coords(row, col, newRow, newCol);
            }   
        }   
        
        return count;
    }
    
    
    public static int knightMoves(Board board, int row, int col, int[] moves, int count)
    {
        Piece knight = board.getPiece(row, col);

        int newRow = 0;
        int newCol = 0;
        
        for (int i = 0; i < KNIGHT_C.length; i++)
        {
            newRow = row + KNIGHT_R[i];
            newCol = col + KNIGHT_C[i];
            
            if (board.inBounds(newRow, newCol))
            {
                Piece target = board.getPiece(newRow, newCol);
                if (target.isOccupied())
                {
                    if (target.isWhite() != knight.isWhite()) moves[count++] = coords(row, col, newRow, newCol);
                }
                else moves[count++] = coords(row, col, newRow, newCol);
            }
        }
        return count;
    }
    
    
    public static int kingMoves(Board board, int row, int col, int[] moves, int count)
    {
        Piece king = board.getPiece(row, col);
        
        int newRow = 0;
        int newCol = 0;

        for(int i = 0; i < 8; i++)
        {
            newRow = row + KING_R[i];
            newCol = col + KING_C[i];
            if (board.inBounds(newRow, newCol))
            {
                Piece target = board.getPiece(newRow, newCol);
                if (target.isOccupied())
                {
                    if (target.isWhite() != king.isWhite())
                    {
                        if (!inCheck(board, SQUARES[coords(newRow, newCol)], king.isWhite())) moves[count++] = coords(row, col, newRow, newCol);
                    }
                }
                else if (!inCheck(board, SQUARES[coords(newRow, newCol)], king.isWhite())) moves[count++] = coords(row, col, newRow, newCol);
            }
        }
        
        // castling 
        if (king.isWhite())
        {
            if (board.whiteCanCastleShort)
            {
                // peice in the way check
                if (!board.getPiece(7, 5).isOccupied() && !board.getPiece(7, 6).isOccupied() && !inCheck(board, SQUARES[coords(7, 4)], true))
                {
                    if (!inCheck(board, SQUARES[coords(7, 5)], true) && !inCheck(board, SQUARES[coords(7, 6)], true))
                    {
                        if (board.getPiece(7, 4).getPiece() == 'K' && board.getPiece(7, 7).getPiece() == 'R') moves[count++] = coords(row, col, 7, 6);
                    }
                }
            }
            if(board.whiteCanCastleLong)
            {
                // peice in the way check
                if (!board.getPiece(7, 1).isOccupied() && !board.getPiece(7, 2).isOccupied() && !board.getPiece(7, 3).isOccupied() && !inCheck(board, SQUARES[coords(7, 4)], true))
                {
                    if (!inCheck(board, SQUARES[coords(7, 2)], true) && !inCheck(board, SQUARES[coords(7, 3)], true))
                    {
                        if (board.getPiece(7, 4).getPiece() == 'K' && board.getPiece(7, 0).getPiece() == 'R') moves[count++] = coords(row, col, 7, 2);
                    }
                }
            }
        }
        else
        {
            if (board.blackCanCastleShort)
            {
                // peice in the way check
                if (!board.getPiece(0, 5).isOccupied() && !board.getPiece(0, 6).isOccupied() && !inCheck(board, SQUARES[coords(0, 4)], false))
                {
                    if (!inCheck(board, SQUARES[coords(0, 5)], false) && !inCheck(board, SQUARES[coords(0, 6)], false))
                    {
                        if (board.getPiece(0, 4).getPiece() == 'K' && board.getPiece(0, 7).getPiece() == 'R') moves[count++] = coords(row, col, 0, 6);
                    }
                }
            }
            if (board.blackCanCastleLong)
            {
                // peice in the way check
                if (!board.getPiece(0, 1).isOccupied() && !board.getPiece(0, 2).isOccupied() && !board.getPiece(0, 3).isOccupied() && !inCheck(board, SQUARES[coords(0, 4)], false))
                {
                    if (!inCheck(board, SQUARES[coords(0, 2)], false) && !inCheck(board, SQUARES[coords(0, 3)], false))
                    {
                        if (board.getPiece(0, 4).getPiece() == 'K' && board.getPiece(0, 0).getPiece() == 'R') moves[count++] = coords(row, col, 0, 2);
                    }
                }
            }
        }
        return count;
    }
    
    
    public static boolean inCheck(Board board, int[] square, boolean isWhite)
    {
        // diagonal       
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
            if (scanPiece.isWhite() != isWhite && (scanPiece.getPiece() == 'Q' || scanPiece.getPiece() == 'R')) return true;
        }
        
        // pawn
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
        
        // knight
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
        
        // king
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
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                if (board.getPiece(row, col).isOccupied() && board.getPiece(row, col).isWhite() == white)
                {   
                    if (!board.validMoves(SQUARES[coords(row, col)], true).isEmpty()) return false;
                }
            }
        }
        return true;
    }

    public static List<Integer> allMoves(Board board, boolean white)
    {
        int[] pseudo = new int[MAX_MOVES];
        int count = pseudoMoves(board, white, pseudo);

        List<Integer> allMoves = new ArrayList<>();
        for (int i = 0; i < count; i++)
        {
            int[] start = coordsStart(pseudo[i]);
            int[] end = coordsEnd(pseudo[i]);

            Undo u = board.makeMove(start, end, 'Q');
            boolean inCheck = inCheck(board, board.getKingLoc(white), white);
            board.unMakeMove(start, end, u);

            if (!inCheck) allMoves.add(pseudo[i]);
        }
        return allMoves;
    }
    
    public static List<Integer> pseudoMoves(Board board, boolean white)
    {
        int[] moves = new int[MAX_MOVES];
        int count = pseudoMoves(board, white, moves);

        List<Integer> allMoves = new ArrayList<>();
        for (int i = 0; i < count; i++) allMoves.add(moves[i]);
        return allMoves;
    }

    public static int pseudoMoves(Board board, boolean white, int[] moves)
    {
        int count = 0;
        
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                Piece p = board.getPiece(row, col);
                if (p.isOccupied() && p.isWhite() == white) count = pieceMoves(board, row, col, moves, count);
            }
        }

        return count;
    }

    public static int mobilityCount(Board board, int row, int col, int[] rDir, int[] cDir, boolean slider, int[] threatCounter)
    {
        if (threatCounter != null) Arrays.fill(threatCounter, 0);
        
        int mobility = 0;
        boolean isWhite = board.getPiece(row, col).isWhite();

        int counter = 0;
        for (int i = 0; i < rDir.length; i++)
        {
            counter = 0;

            if (!slider)
            {
                counter++;
                if (!board.inBounds(row + (counter * rDir[i]), col + (counter * cDir[i]))) continue;
                Piece scanPiece = board.getPiece(row + (counter * rDir[i]), col + (counter * cDir[i]));

                if (scanPiece.isOccupied())
                {
                    if (scanPiece.isWhite() != isWhite) 
                    {
                        if (threatCounter != null)
                        {
                            char pieceChar = scanPiece.getPiece();
                            if (pieceChar == 'R') threatCounter[0]++;
                            else if (pieceChar == 'Q') threatCounter[1]++;
                        }
                        mobility++;
                    }
                }
                else mobility++;
            }
            else
            {
                int maxRow = 7;
                int maxCol = 7;

                if (rDir[i] == 1) maxRow = 7 - row;
                else if (rDir[i] == -1) maxRow = row;

                if (cDir[i] == 1) maxCol = 7 - col;
                else if (cDir[i] == -1) maxCol = col;

                int steps = Math.min(maxCol, maxRow);

                for (counter = 1; counter <= steps; counter++)
                {
                    Piece scanPiece = board.getPiece(row + (counter * rDir[i]), col + (counter * cDir[i]));

                    if (scanPiece.isOccupied())
                    {
                        if (scanPiece.isWhite() != isWhite)
                        {
                            if (threatCounter != null)
                            {
                                char pieceChar = scanPiece.getPiece();
                                if (pieceChar == 'R') threatCounter[0]++;
                                else if (pieceChar == 'Q') threatCounter[1]++;
                            }
                            mobility++;
                        }
                        break;
                    }
                    else mobility++;
                }
            }
        }

        return mobility;
    }

    // WONT WORK WITH SMP
    private static final int[] seeGain = new int[32];
    private static final Piece[] seeRemovedPieces = new Piece[32];
    private static final int[] seeRemovedSquares = new int[32];

    // version for zzSee
    public static int see(Board board, int[] move) { return see(board, coords(move[0], move[1], move[2], move[3])); }

    public static int see(Board board, int move)
    {
        int startRow = startSquare(move) / 8;
        int startCol = startSquare(move) % 8;
        int endRow = endSquare(move) / 8;
        int endCol = endSquare(move) % 8;

        Piece movingPiece = board.getPiece(startRow, startCol);
        Piece pieceOnSquare = movingPiece;
        
        boolean whiteTurn = movingPiece.isWhite();
        boolean isEnPassant = startCol != endCol && movingPiece.getPiece() == 'P' && board.getPiece(endRow, endCol) == Piece.EMPTY;
        int[] gain = seeGain;
        int removedCount = 0;

        Piece epPiece = null;
        int[] epSquare = null;

        if (isEnPassant) 
        {
            gain[0] = 100;
            epPiece = board.getPiece(startRow, endCol);
            epSquare = SQUARES[coords(startRow, endCol)];
            board.addPiece(Piece.EMPTY, epSquare);
        }
        else gain[0] = board.getPiece(endRow, endCol).value();

        board.addPiece(Piece.EMPTY, SQUARES[move / 100]);
        
        while (true) 
        {
            int attacker = leastValuableAttacker(board, endRow, endCol, !whiteTurn);
            if (attacker == -1) break;

            Piece seePiece = board.getPiece(SQUARES[attacker]);
            seeRemovedPieces[removedCount] = seePiece;
            seeRemovedSquares[removedCount] = attacker;
            removedCount++;
            gain[removedCount] = pieceOnSquare.value() - gain[removedCount - 1];
            
            pieceOnSquare = seePiece;
            board.addPiece(Piece.EMPTY, SQUARES[attacker]);
            whiteTurn = !whiteTurn;
        }

        for (int i = removedCount - 1; i >= 0; i--) 
        {
            board.addPiece(seeRemovedPieces[i], SQUARES[seeRemovedSquares[i]]);
            gain[i] = -Math.max(-gain[i], gain[i + 1]);
        }
        board.addPiece(movingPiece, SQUARES[move / 100]);
        if (epSquare != null) board.addPiece(epPiece, epSquare);

        return gain[0];
    }

    public static int leastValuableAttacker (Board board, int targetRow, int targetCol, boolean isWhite)
    {
        // pawns
        int pawnRow = isWhite? targetRow + 1 : targetRow - 1;
        for (int i = -1; i <= 1; i += 2)
        {
            if (!board.inBounds(pawnRow, targetCol + i)) continue;
            if (board.getPiece(pawnRow, targetCol + i) == (isWhite? Piece.W_PAWN : Piece.B_PAWN)) return coords(pawnRow, targetCol + i);
        }
        
        // knights
        for (int i = 0; i < KNIGHT_C.length; i++)
        {
            int row = KNIGHT_R[i] + targetRow;
            int col = KNIGHT_C[i] + targetCol;
            if (!board.inBounds(row, col)) continue;
            if (board.getPiece(row, col) == (isWhite? Piece.W_KNIGHT : Piece.B_KNIGHT)) return coords(row, col);
        }

        // bishops
        int bSlider = 0;
        for (int i = 0; i < DIAG_C.length; i++)
        {
            bSlider = 0;
            while (true) 
            {
                bSlider++;
                int row = DIAG_R[i] * bSlider + targetRow;
                int col = DIAG_C[i] * bSlider + targetCol;
                if (!board.inBounds(row, col)) break;
                if (board.getPiece(row, col) == (isWhite? Piece.W_BISHOP : Piece.B_BISHOP)) return coords(row, col);
                if (board.getPiece(row, col).isOccupied()) break;
            }
        }

        // rooks
        int rSlider = 0;
        for (int i = 0; i < STRAIGHT_C.length; i++)
        {
            rSlider = 0;
            while (true) 
            {
                rSlider++;
                int row = STRAIGHT_R[i] * rSlider + targetRow;
                int col = STRAIGHT_C[i] * rSlider + targetCol;
                if (!board.inBounds(row, col)) break;
                if (board.getPiece(row, col) == (isWhite? Piece.W_ROOK : Piece.B_ROOK)) return coords(row, col);
                if (board.getPiece(row, col).isOccupied()) break;
            }
        }

        // queens
        int qSlider = 0;
        for (int i = 0; i < QUEEN_C.length; i++)
        {
            qSlider = 0;
            while (true) 
            {
                qSlider++;
                int row = QUEEN_R[i] * qSlider + targetRow;
                int col = QUEEN_C[i] * qSlider + targetCol;
                if (!board.inBounds(row, col)) break;
                if (board.getPiece(row, col) == (isWhite? Piece.W_QUEEN : Piece.B_QUEEN)) return coords(row, col);
                if (board.getPiece(row, col).isOccupied()) break;
            }
        }

        // kings
        for (int i = 0; i < KING_C.length; i++)
        {
            int row = KING_R[i] + targetRow;
            int col = KING_C[i] + targetCol;
            if (!board.inBounds(row, col)) continue;
            if (board.getPiece(row, col) == (isWhite? Piece.W_KING : Piece.B_KING)) return coords(row, col);
        }

        return -1;
    }
}