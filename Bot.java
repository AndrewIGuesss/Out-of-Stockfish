import java.util.ArrayList;
import java.util.Arrays;

public class Bot 
{
    public static volatile boolean stopThinking = false;

    public static final int MATE = 123456789;
    private static final int INFINITY = 1234567890;
    private static final int MAX_DEPTH_EXTENTION = 20;
    public static String rootScores = "";
    public static boolean exactRootScores = true; // true = exact root score (slower)

    public static int movesAnalysed = 0;
    public static int quiescenceMovesAnalysed = 0;
    public static int depthReached = 0;

    public static int[][][] killerMoves = new int[64][2][];
    public static int[][] historyTable = new int[64][64];

    // repetition check
    public static long[] plyPathKeys = new long[64];

    static long deadline = System.currentTimeMillis() + Settings.botSearchTimeLimit;
    static boolean timeUp = false;
    static boolean timeCanBeUp = false;
    
    //king safety
    private static final int[] COLUMN_SAFETY_BONUS = {30, 30, 20, 0, 0, 0, 30, 30};
    private static final int[] ROW_SAFETY_BONUS = {25, 10, 0, 0, 0, 0, 0, 0}; // distance from home rank

    //indiv piece bonuses
    // piece square tables from whites view (row 0 is rank 8), black uses row 7 - row
    
    private static final int[] PASSED_PAWN_BONUS = {0, 10, 15, 25, 40, 60, 90, 0};
    
    private static final int[][] PAWN_TABLE = {
        //a    b    c    d    e    f    g    h
        {785, 790, 795, 800, 800, 795, 790, 785}, // 8 pawn counted like a queen for bot calc
        { 65,  70,  75,  80,  80,  75,  70,  65}, // 7
        { 25,  30,  35,  40,  40,  35,  30,  25}, // 6
        {  5,  10,  15,  30,  30,  15,  10,   5}, // 5
        {-15, -10,  10,  30,  30,  10, -10, -15}, // 4
        {-10,  -5,   0,   5,   5,   0,  -5, -10}, // 3
        {-15, -10,  -5, -10, -10,  -5, -10, -15}, // 2
        {  0,   0,   0,   0,   0,   0,   0,   0}  // 1
    };

        private static final int[][] KNIGHT_TABLE = {
        //a    b    c    d    e    f    g    h
        {-30, -10, -10, -10, -10, -10, -10, -30}, // 8
        {-20,   0,  10,  10,  10,  10,   0, -20}, // 7
        {-10,  10,  25,  25,  25,  25,  10, -10}, // 6
        {-10,  10,  25,  30,  30,  25,  10, -10}, // 5
        {-10,  10,  25,  30,  30,  25,  10, -10}, // 4
        {-10,  10,  25,  20,  20,  25,  10, -10}, // 3
        {-20,   0,   5,  10,  10,   5,   0, -20}, // 2
        {-30, -10, -10, -10, -10, -10, -10, -30}  // 1
    };

    private static final int[][] BISHOP_TABLE = {
        //a    b    c    d    e    f    g    h
        {-20, -10, -10, -10, -10, -10, -10, -20}, // 8
        {-10,   0,   0,   0,   0,   0,   0, -10}, // 7
        {-10,   0,   5,  10,  10,   5,   0, -10}, // 6
        {-10,   5,   5,  10,  10,   5,   5, -10}, // 5
        {-10,   0,  10,  10,  10,  10,   0, -10}, // 4
        {-10,  10,  10,  10,  10,  10,  10, -10}, // 3
        {-10,  10,   0, -10, -10,   0,  10, -10}, // 2
        {-20, -10, -10, -10, -10, -10, -10, -20}  // 1
    };

    private static final int[][] ROOK_TABLE = {
        //a    b    c    d    e    f    g    h
        {  0,   0,   0,   0,   0,   0,   0,   0}, // 8
        { 10,  20,  20,  20,  20,  20,  20,  10}, // 7
        { -5,   0,   0,   0,   0,   0,   0,  -5}, // 6
        { -5,   0,   0,   0,   0,   0,   0,  -5}, // 5
        { -5,   0,   0,   0,   0,   0,   0,  -5}, // 4
        { -5,   0,   0,   0,   0,   0,   0,  -5}, // 3
        { -5,   0,   0,   0,   0,   0,   0,  -5}, // 2
        {  0,   0,   5,  10,  10,   5,   0,   0}  // 1
    };

    private static final int[][] QUEEN_TABLE = {
        //a    b    c    d    e    f    g    h
        {-20, -10, -10,  -5,  -5, -10, -10, -20}, // 8
        {-10,   0,   0,   0,   0,   0,   0, -10}, // 7
        {-10,   0,   5,   5,   5,   5,   0, -10}, // 6
        { -5,   0,   5,   5,   5,   5,   0,  -5}, // 5
        { -5,   0,   5,   5,   5,   5,   0,  -5}, // 4
        {-10,   0,   5,   5,   5,   5,   0, -10}, // 3
        {-10,   0,   0,   0,   0,   0,   0, -10}, // 2
        {-20, -10, -10,   0,   0, -10, -10, -20}  // 1
    };

    private static final int[][] KING_ENDGAME_TABLE = {
        //a    b    c    d    e    f    g    h
        {-30, -20, -10, -10, -10, -10, -20, -30}, // 8
        {-20, -10,   0,   5,   5,   0, -10, -20}, // 7
        {-10,   0,  15,  20,  20,  15,   0, -10}, // 6
        {-10,   5,  20,  30,  30,  20,   5, -10}, // 5
        {-10,   5,  20,  30,  30,  20,   5, -10}, // 4
        {-10,   0,  15,  20,  20,  15,   0, -10}, // 3
        {-20, -10,   0,   5,   5,   0, -10, -20}, // 2
        {-30, -20, -10, -10, -10, -10, -20, -30}  // 1
    };

    public static int getPieceValue(char piece)
    {
        piece = Character.toLowerCase(piece);
        if (piece == 'p') return 100;
        if (piece == 'n') return 300;
        if (piece == 'b') return 300;
        if (piece == 'r') return 500;
        if (piece == 'q') return 900;
        if (piece == 'k') return 1000;
        return -1;
    }

    public static int evalBoard (Board board)
    {
        int totalCentipawns = 0;
        int whiteMaterialValue = 0; 
        int blackMaterialValue = 0;
        int whitePawnCount = 0;
        int blackPawnCount = 0;
        int whiteBishopCount = 0;
        int blackBishopCount = 0;
        int[] whiteKingLoc = new int[2];
        int[] blackKingLoc = new int[2];
        int whiteMinorPiecesHome = 0;
        int blackMinorPiecesHome = 0;
        boolean whiteQueenMoved = false;
        boolean blackQueenMoved = false;

        for (int row = 0; row < board.getBoard().length; row++)
        {
            for (int col = 0; col < board.getBoard()[row].length; col++)
            {
                Piece currentPiece = board.getPiece(row, col);
                if (currentPiece.isOccupied())
                {
                    // save king location then skip
                    if (currentPiece.getPiece() == 'K')
                    {
                        if (currentPiece.isWhite()) whiteKingLoc = new int[]{row, col};
                        else blackKingLoc = new int[]{row, col};
                        continue;
                    }

                    int centipawns = getPieceValue(currentPiece.getPiece());

                    if (currentPiece.isWhite()) whiteMaterialValue += centipawns;
                    else blackMaterialValue += centipawns;
                    
                    int tableRow = currentPiece.isWhite()? row : 7 - row;

                    
                    if (currentPiece.getPiece() == 'P')
                    {
                        centipawns += PAWN_TABLE[tableRow][col];
                        if (currentPiece.isWhite()) whitePawnCount++;
                        else blackPawnCount++;

                        int PassedPawnBonus = 0;
                        int doubledPawnPenalty = 0;
                        int isolatedPawnPenalty = 0;

                        int adjacentPawnCounter = 0;
                        int enemyAdjacentPawnCounter = 0;

                        if (currentPiece.isWhite()) 
                        {
                            for (int scanRow = row - 1; scanRow >= 0; scanRow--)
                            {
                                Piece scanPiece = board.getPiece(scanRow, col);
                                if (scanPiece.getPiece() == 'P')
                                {
                                    if (scanPiece.isWhite() == currentPiece.isWhite()) doubledPawnPenalty += 20;
                                    else enemyAdjacentPawnCounter++; 
                                }
                            }
                        }
                        else
                        {
                            for (int scanRow = row + 1; scanRow <= 7; scanRow++)
                            {
                                Piece scanPiece = board.getPiece(scanRow, col);
                                if (scanPiece.getPiece() == 'P')
                                {
                                    if (scanPiece.isWhite() == currentPiece.isWhite()) doubledPawnPenalty += 20;
                                    else enemyAdjacentPawnCounter++; 
                                }
                            }
                        }

                        
                        for (int isoScanCol = -1; isoScanCol <= 1; isoScanCol += 2)
                        {
                            if (!board.inBounds(row, col + isoScanCol)) continue;
                            for (int isoScanRow = 7; isoScanRow >= 0; isoScanRow--)
                            {
                                Piece isoScanPiece = board.getPiece(isoScanRow, col + isoScanCol);
                                if (isoScanPiece.getPiece() == 'P' && isoScanPiece.isWhite() == currentPiece.isWhite()) adjacentPawnCounter++;

                                if (currentPiece.isWhite())
                                {
                                    if (isoScanRow < row && isoScanPiece.getPiece() == 'P' && isoScanPiece.isWhite() == false) enemyAdjacentPawnCounter++;
                                }
                                else
                                {
                                    if (isoScanRow > row && isoScanPiece.getPiece() == 'P' && isoScanPiece.isWhite() == true) enemyAdjacentPawnCounter++;
                                }
                            }
                        }
                        if (adjacentPawnCounter == 0) isolatedPawnPenalty = 15;
                        if (enemyAdjacentPawnCounter == 0) PassedPawnBonus = currentPiece.isWhite()? PASSED_PAWN_BONUS[7 - row] : PASSED_PAWN_BONUS[row];
                        centipawns += PassedPawnBonus - doubledPawnPenalty - isolatedPawnPenalty;
                    }
                    else if (currentPiece.getPiece() == 'N')
                    {
                        centipawns += KNIGHT_TABLE[tableRow][col];
                        if (!currentPiece.hasMoved())
                        {
                            if (currentPiece.isWhite()) whiteMinorPiecesHome++;
                            else blackMinorPiecesHome++;
                        }
                    }
                    else if (currentPiece.getPiece() == 'B') 
                    {
                        centipawns += BISHOP_TABLE[tableRow][col];
                        if (!currentPiece.hasMoved())
                        {
                            if (currentPiece.isWhite()) whiteMinorPiecesHome++;
                            else blackMinorPiecesHome++;
                        }

                        boolean currPieceIsWhite = currentPiece.isWhite();

                        if (currPieceIsWhite) whiteBishopCount++;
                        else blackBishopCount++;

                        int forward = currPieceIsWhite? -1 : 1;
                        int penalty = 0;

                        for (int diagCol = -1; diagCol <= 1; diagCol += 2)
                        {
                            int blockingPieces = 0;
                            for (int scanDiagonal = 1; scanDiagonal < 8; scanDiagonal++)
                            {
                                if (!board.inBounds(row + (scanDiagonal * forward), col + (scanDiagonal * diagCol))) break;
                                Piece scanPiece = board.getPiece(row + (scanDiagonal * forward), col + (scanDiagonal * diagCol));

                                if (!scanPiece.isOccupied()) continue;
                                else blockingPieces++;

                                if (blockingPieces > 2) break;
                                if (scanPiece.isWhite() == currPieceIsWhite)
                                {
                                    if (blockingPieces == 1) penalty += 10;
                                }
                                else if (scanPiece.getPiece() == 'K') penalty -= (20 / blockingPieces);
                                else if (scanPiece.getPiece() == 'Q') penalty -= (10 / blockingPieces);
                                else if (scanPiece.getPiece() == 'P') penalty += (8 / blockingPieces);
                            }
                        }

                        centipawns -= penalty;
                    }
                    else if (currentPiece.getPiece() == 'R')
                    {
                        centipawns += ROOK_TABLE[tableRow][col];

                        int penalty = 0;
                        if (currentPiece.isWhite())
                        {
                            int blockingPieces = 0;
                            for (int scanRow = row; scanRow > 0; scanRow--)
                            {
                                Piece scanPiece = board.getPiece(scanRow - 1, col);
                                if (!scanPiece.isOccupied()) continue;
                                else blockingPieces++;
                                
                                // can x ray at most 2 pieces (pinned piece & target piece) 
                                if (blockingPieces > 2) break;

                                if (scanPiece.isWhite())
                                {
                                    if (blockingPieces == 1) penalty += 20;
                                }
                                else if (scanPiece.getPiece() == 'K') penalty -= (20 / blockingPieces);
                                else if (scanPiece.getPiece() == 'Q') penalty -= (10 / blockingPieces);
                                else if (blockingPieces == 1) penalty += 10;
                            }
                        }
                        else
                        {
                            int blockingPieces = 0;
                            for (int scanRow = row; scanRow < 7; scanRow++)
                            {
                                Piece scanPiece = board.getPiece(scanRow + 1, col);
                                if (!scanPiece.isOccupied()) continue;
                                else blockingPieces++;
                                
                                // can x ray at most 2 pieces (pinned piece & target piece) 
                                if (blockingPieces > 2) break;

                                if (!scanPiece.isWhite())
                                {
                                    if (blockingPieces == 1) penalty += 20;
                                }
                                else if (scanPiece.getPiece() == 'K') penalty -= (20 / blockingPieces);
                                else if (scanPiece.getPiece() == 'Q') penalty -= (10 / blockingPieces);
                                else if (blockingPieces == 1) penalty += 10;
                            }
                        }
                        centipawns -= penalty;
                    }
                    else if (currentPiece.getPiece() == 'Q')
                    {
                        centipawns += QUEEN_TABLE[tableRow][col];
                        if (currentPiece.hasMoved())
                        {
                            if (currentPiece.isWhite()) whiteQueenMoved = true;
                            else blackQueenMoved = true;
                        }
                    }
                    centipawns *= currentPiece.isWhite()? 1 : -1;
                    totalCentipawns += centipawns;
                }
            }
        }

        //early queen agro
        if (whiteQueenMoved && whiteMinorPiecesHome >= 2) totalCentipawns -= 25;
        if (blackQueenMoved && blackMinorPiecesHome >= 2) totalCentipawns += 25;

        //bishop pair
        if (whiteBishopCount >= 2) totalCentipawns += 40;
        if (blackBishopCount >= 2) totalCentipawns -= 40;

        //insuficent material
        if (blackPawnCount == 0 && whitePawnCount == 0)
        {
            if (blackMaterialValue - (blackPawnCount * 100) <= 300 && whiteMaterialValue - (whitePawnCount * 100) <= 300) return 0;
        }

        //mop up check
        if (whiteMaterialValue - blackMaterialValue > 500 && blackMaterialValue < 300) totalCentipawns += evalMopup(blackKingLoc, whiteKingLoc, true);
        else if (blackMaterialValue - whiteMaterialValue > 500 && whiteMaterialValue < 300) totalCentipawns += evalMopup(whiteKingLoc, blackKingLoc, false);

        //king safety / endgame king
        if (blackMaterialValue - (blackPawnCount * 100) > 1300) totalCentipawns += kingSafety(board, whiteKingLoc, true);
        else totalCentipawns += KING_ENDGAME_TABLE[whiteKingLoc[0]][whiteKingLoc[1]];

        if (whiteMaterialValue - (whitePawnCount * 100) > 1300) totalCentipawns += kingSafety(board, blackKingLoc, false);
        else totalCentipawns -= KING_ENDGAME_TABLE[7 - blackKingLoc[0]][blackKingLoc[1]];
        
        return totalCentipawns;
    }

    public static int kingSafety(Board board, int[] kingLoc, boolean isWhite)
    {
        int safetyBonus = 0;
        int sideMulti = isWhite? 1 : -1;
        
        // pawn sheild
        int pawnSheildBonus = 0;
        if (!board.getPiece(kingLoc).hasCastled()) pawnSheildBonus = 30;
        else
        {
            for (int i = -1; i <= 1; i++)
            {
                if (board.inBounds(kingLoc[0] - sideMulti, kingLoc[1] + i))
                {
                    if (board.getPiece(kingLoc[0] - sideMulti, kingLoc[1] + i).getPiece() == 'P' && board.getPiece(kingLoc[0] - sideMulti, kingLoc[1] + i).isWhite() == isWhite)
                    {
                        pawnSheildBonus += 10;
                    }
                }
                else pawnSheildBonus += 10;
            }
        }
        
        // king location
        int columnBonus = 0;
        columnBonus += COLUMN_SAFETY_BONUS[kingLoc[1]];
        
        int rowBonus = 0;
        
        rowBonus += isWhite? ROW_SAFETY_BONUS[7 - kingLoc[0]] : ROW_SAFETY_BONUS[kingLoc[0]];
        
        //cant castle penalty
        int castlingRightsPenalty = 0;
        if (!board.getPiece(kingLoc).hasCastled() && isWhite && board.getPiece(kingLoc).hasMoved()) castlingRightsPenalty = -30;
        else if (!board.getPiece(kingLoc).hasCastled() && !isWhite && board.getPiece(kingLoc).hasMoved()) castlingRightsPenalty = -30;

        safetyBonus = pawnSheildBonus + columnBonus + rowBonus + castlingRightsPenalty;
        return safetyBonus * sideMulti;
    }

    public static int evalMopup(int[] lossingkingLoc, int[] winningkingLoc, boolean whiteWinning)
    {
        int middleDistanceRow = Math.min(Math.abs(3 - lossingkingLoc[0]), Math.abs(4 - lossingkingLoc[0]));
        int middleDistanceCol = Math.min(Math.abs(3 - lossingkingLoc[1]), Math.abs(4 - lossingkingLoc[1]));
        int centerDistance = Math.max(middleDistanceCol, middleDistanceRow);

        int kingDistanceRow = Math.abs(winningkingLoc[0] - lossingkingLoc[0]);
        int kingDistanceCol = Math.abs(winningkingLoc[1] - lossingkingLoc[1]);
        int kingDistance = Math.max(kingDistanceRow, kingDistanceCol);

        int bonus = centerDistance * 10 + (7 - kingDistance) * 4;
        return bonus * ((whiteWinning)? 1 : -1);
    }

    public static int orderScore (Board board, int[] move, int ply)
    {
        // MVV-LVA = most valuable victim, least valuable attacker
        int victimValue;
        int attackerValue = getPieceValue(board.getPiece(move[0], move[1]).getPiece());
        if (board.getPiece(move[0], move[1]).getPiece() == 'K') attackerValue = 50;

        if (board.getPiece(move[2], move[3]).isOccupied()) 
        {
            victimValue = getPieceValue(board.getPiece(move[2], move[3]).getPiece());
            return 1000000 + victimValue * 10 - attackerValue; 
        }

        if (ply == -1) return 0;

        if (Arrays.equals(killerMoves[ply][0], move)) return 900000;
        else if (Arrays.equals(killerMoves[ply][1], move)) return 800000;
        else return Math.min(historyTable[move[0] * 8 + move[1]][move[2] * 8 + move[3]], 799999);
    }

    public static int quiescence(Board board, boolean whiteTurn, int alpha, int beta)
    {
        // time limit check
        if (System.currentTimeMillis() > deadline && timeCanBeUp)
        { 
            timeUp = true;
            return 0;
        }

        // node counters
        movesAnalysed++;
        quiescenceMovesAnalysed++;

        // stand pat (eval if we stop capturing here)
        int standPat = evalBoard(board) * (whiteTurn? 1 : -1);
        if (standPat >= beta) return standPat;
        
        alpha = Math.max(alpha, standPat);
        int best = standPat;

        // captures only (MVV-LVA ordered)
        ArrayList<int[]> moves = Moves.pseudoMoves(board, whiteTurn);
        ArrayList<int[]> captures = new ArrayList<>();
        
        for (int[] move : moves)
        {
            if (orderScore(board, move, -1) != 0) captures.add(move);
        }
        captures.sort((a, b) -> Integer.compare(orderScore(board, b, -1), orderScore(board, a, -1)));
        
        // search captures
        for (int[] capture : captures)
        {
            Board copy = new Board(board);
            copy.applyMove(new int[]{capture[0], capture[1]}, new int[]{capture[2], capture[3]}, true, 'Q');
            if (Moves.inCheck(copy, Board.findKing(copy, whiteTurn), whiteTurn)) continue;

            int score = -quiescence(copy, !whiteTurn, -beta, -alpha);
            if (score > best)
            {
                best = score;
                alpha = Math.max(alpha, score);
            }
            if (alpha >= beta) break;
        }
        return best;
    }

    public static int negamax(Board board, int maxDepth, boolean whiteTurn, int alpha, int beta, int ply, boolean isNullMove)
    {
        // transposition table save (remember this position's result)
        int originalAlpha = alpha;
        int[] bestMoveHere = null;

        // zobrist hash + repetition detection
        long currKey = Zobrist.makeZobristHash(board, whiteTurn);
        if (ply > 0)
        {
            if (Game.positionKeys.contains(currKey)) return 0;
            for (int i = 0; i <= ply - 1; i++)
            {
                if (plyPathKeys[i] == currKey) return 0;
            }
            plyPathKeys[ply] = currKey;
        }

        // time limit / stop thinking check
        if ((System.currentTimeMillis() > deadline && timeCanBeUp) || stopThinking && timeCanBeUp)
        { 
            timeUp = true;
            return 0;
        }

        // node counter
        movesAnalysed++;

        // check depth extension
        boolean inCheck = Moves.inCheck(board, Board.findKing(board, whiteTurn), whiteTurn);
        if (inCheck && ply < MAX_DEPTH_EXTENTION) maxDepth++;

        // transposition table lookup (check if we've seen this position before)
        int[] ttMove = null;
        int index = (int)(currKey & (TTEntry.TT_SIZE - 1));
        TTEntry entry = TTEntry.ttTable[index];
        if (entry != null && entry.key == currKey)
        {
            ttMove = entry.bestmove;
            if (entry.depth >= maxDepth)
            {
                int ttScore = entry.score;
                if (ttScore > MATE - 1000) ttScore -= ply;
                if (ttScore < -(MATE - 1000)) ttScore += ply;
                switch (entry.flag) 
                {
                    case TTEntry.EXACT: return ttScore;
                    case TTEntry.LOWER: 
                        if (ttScore >= beta) return ttScore;
                        else break;
                    case TTEntry.UPPER:
                        if (ttScore <= alpha) return ttScore;
                        else break;
                }
            }
        }

        // quiescence at the leaves (last search until we reach the max depth)
        if (maxDepth == 0) return quiescence(board, whiteTurn, alpha, beta);

        // null move pruning
        if (ply != 0 && maxDepth >= 4 && !isNullMove && !inCheck)
        {
            // - 10 for king
            if (board.getBoardValue(whiteTurn) - 10 != board.getBoardPawnCount(whiteTurn))
            {
                int reduction = 2;
                Board nullCopy = new Board(board);
                nullCopy.setEnPassantSquare(null);
                int score = -negamax(nullCopy, maxDepth - 1 - reduction, !whiteTurn, -beta, -beta + 1, ply + 1, true);
                
                if (score >= beta) return beta;
            }
        }

        // move generation + ordering (MVV-LVA, killers, history)
        // startRow, startCol, endRow, endCol
        ArrayList<int[]> moves = Moves.pseudoMoves(board, whiteTurn);
        
        int best = -MATE; // extremly low place holder value

        moves.sort((a, b) -> Integer.compare(orderScore(board, b, ply), orderScore(board, a, ply)));
        
        // try TT move first
        if (ttMove != null)
        {
            for (int [] move : moves)
            {
                if (Arrays.equals(ttMove, move))
                {
                    moves.remove(move);
                    moves.add(0, move);
                    break;
                }
            }
        }

        // main search loop
        int validMoves = 0;
        
        for (int[] move : moves)
        {

            Board copy = new Board(board);
            copy.applyMove(new int[]{move[0], move[1]}, new int[]{move[2], move[3]}, true, 'Q');
            if (Moves.inCheck(copy, Board.findKing(copy, whiteTurn), whiteTurn)) continue;
            
            validMoves++;
            
            int score = -negamax(copy, maxDepth - 1, !whiteTurn, -beta, -alpha, ply + 1, false);
            if (score > best)
            {
                bestMoveHere = move;
                best = score;
                alpha = Math.max(alpha, score);
            }
            // beta cutoff: save killer + history for quiet moves
            if (alpha >= beta)
            {
                if (!board.getPiece(move[2], move[3]).isOccupied())
                {
                    if (!Arrays.equals(killerMoves[ply][0], move))
                    {
                        killerMoves[ply][1] = killerMoves[ply][0];
                        killerMoves[ply][0] = move;
                    }
                    int from = move[0] * 8 + move[1];
                    int to = move[2] * 8 + move[3];
                    historyTable[from][to] += maxDepth * maxDepth;
                }
                break;
            }
        }
        
        // TT save (mate scores made relative to this node)
        if (!timeUp && validMoves > 0)
        {
            int flag;
            if (best <= originalAlpha) flag = TTEntry.UPPER;
            else if (best >= beta) flag = TTEntry.LOWER;
            else flag = TTEntry.EXACT;

            int storedScore;
            if (best > MATE - 1000) storedScore = best + ply;
            else if (best < -(MATE - 1000)) storedScore = best - ply;
            else storedScore = best;

            TTEntry.ttTable[index] = new TTEntry(currKey, maxDepth, storedScore, flag, bestMoveHere);
        }

        // checkmate / stalemate
        if (validMoves > 0) return best;
        
        if (inCheck) return -MATE + ply;
        else return 0;
    }

    public static int[] bestMove(Board board, int maxDepth, boolean whiteTurn)
    {
        // reset search state (stop flag, time limit, killers, history)
        stopThinking = false;

        deadline = System.currentTimeMillis() + Settings.botSearchTimeLimit;
        timeCanBeUp = false;
        timeUp = false;
        killerMoves = new int[64][2][];
        historyTable = new int[64][64];

        // stats
        depthReached = 0;

        movesAnalysed = 0;
        quiescenceMovesAnalysed = 0;

        double startTime = System.currentTimeMillis();

        // root move generation + ordering
        // startRow, startCol, endRow, endCol
        ArrayList<int[]> moves = Moves.pseudoMoves(board, whiteTurn);

        int bestScore = -INFINITY;
        int bestScoreSoFar = -INFINITY;
        int alpha = -INFINITY;
        int beta = INFINITY;
        int[] bestMove = null;
        int[] bestMoveSoFar = null;

        moves.sort((a, b) -> Integer.compare(orderScore(board, b, -1), orderScore(board, a, -1)));
        // root position key for repetition detection
        plyPathKeys[0] = Zobrist.makeZobristHash(board, whiteTurn);

        // iterative deepening
        for (int depth = 1; depth <= maxDepth; depth++)
        {
            bestScoreSoFar = -INFINITY;
            bestMoveSoFar = null;
            alpha = -INFINITY;
            beta = INFINITY;
            StringBuilder depthScores = new StringBuilder();
            
            // root search loop
            for(int[] move : moves)
            {
                Board copy = new Board(board);
                copy.applyMove(new int[]{move[0], move[1]}, new int[]{move[2], move[3]}, true, 'Q');
                if (Moves.inCheck(copy, Board.findKing(copy, whiteTurn), whiteTurn)) continue;
                
                if (bestMoveSoFar == null) bestMoveSoFar = move;
                
                int score = -negamax(copy, depth - 1, !whiteTurn, -beta, -alpha, 1, false);
                
                if (timeUp) break;
                
                // root scores display
                depthScores.append(Board.convertMove(new int[]{move[0], move[1]}) + Board.convertMove(new int[]{move[2], move[3]}));
                depthScores.append((!exactRootScores && score <= alpha && bestMoveSoFar != move)? " <= " : " = ");
                depthScores.append(score / 100.0 + "\n");

                if (score > bestScoreSoFar) 
                {
                    bestScoreSoFar = score; bestMoveSoFar = move;
                    alpha = Math.max(alpha, score);
                }
            }
            if (timeUp) break;
            
            // depth finished: save results, best move first for next depth
            rootScores = "root scores (depth " + depth + "):\n" + depthScores;

            moves.remove(bestMoveSoFar);
            moves.add(0, bestMoveSoFar);
            
            timeCanBeUp = true;

            bestMove = bestMoveSoFar;
            bestScore = bestScoreSoFar;
            depthReached++;
        }
        
        if (Settings.highlightPreviousMove)
        {
            Game.previousMove = new boolean[8][8];
            Game.previousMove[bestMove[0]][bestMove[1]] = true;
            Game.previousMove[bestMove[2]][bestMove[3]] = true;
        }

        // results for the debug menu
        Game.botMoveTimeMs = (int)(System.currentTimeMillis() - startTime);
        Game.botMoveEval = bestScore;
        return bestMove;
    }
}