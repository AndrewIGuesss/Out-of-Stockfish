public class Eval 
{
    public static int phase = 0;

    // eval breakdown
    // evalBreakDown    [eval term index (.ordinal)]    [0 - white / 1 - black]    [0 - mid game / 1 - end game]

    public static int[][][] evalBreakDown = new int[EvalTerm.SIZE][2][2];
    private static boolean recordBreakDown = false;

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

    private static final int[][] PAWN_ENDGAME_TABLE = {
        //a    b    c    d    e    f    g    h
        {800, 800, 800, 800, 800, 800, 800, 800}, // 8 pawn counted like a queen for bot calc
        { 80,  80,  80,  80,  80,  80,  80,  80}, // 7
        { 40,  40,  40,  40,  40,  40,  40,  40}, // 6
        { 20,  20,  20,  20,  20,  20,  20,  20}, // 5
        { 10,  10,  10,  10,  10,  10,  10,  10}, // 4
        {  5,   5,   5,   5,   5,   5,   5,   5}, // 3
        {  0,   0,   0,   0,   0,   0,   0,   0}, // 2
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

    private static final int[][] KING_MIDDLEGAME_TABLE = {
        //a    b    c    d    e    f    g    h
        {-50, -50, -50, -60, -60, -50, -50, -50}, // 8
        {-50, -50, -50, -60, -60, -50, -50, -50}, // 7
        {-40, -40, -40, -50, -50, -40, -40, -40}, // 6
        {-30, -30, -30, -40, -40, -30, -30, -30}, // 5
        {-20, -20, -20, -30, -30, -20, -20, -20}, // 4
        {  0,   0, -10, -20, -20, -10,   0,   0}, // 3
        { 40,  40,  30,  10,  10,  10,  40,  40}, // 2
        { 55,  55,  45,  25,  25,  25,  55,  55}  // 1
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

    private static int term(int value, boolean isWhite, int termIndex, GamePhase phase)
    {
        if (recordBreakDown)
        {
            if (phase == GamePhase.BOTH)
            {
                evalBreakDown[termIndex][isWhite? 0 : 1][0] += value;
                evalBreakDown[termIndex][isWhite? 0 : 1][1] += value;
            }
            else if (phase == GamePhase.MID) evalBreakDown[termIndex][isWhite? 0 : 1][0] += value;
            else evalBreakDown[termIndex][isWhite? 0 : 1][1] += value;
        }
        return value;
    }

    public static int evalBoard (Board board, boolean breakDown)
    {
        int midGameScore = 0;
        int endGameScore = 0;

        int whiteMaterialValue = 0; 
        int blackMaterialValue = 0;
        int whitePawnCount = 0;
        int blackPawnCount = 0;
        int whiteBishopCount = 0;
        int blackBishopCount = 0;
        int[] whiteKingLoc = board.getWhiteKingLoc();
        int[] blackKingLoc = board.getBlackKingLoc();
        int whiteMinorPiecesHome = 0;
        int blackMinorPiecesHome = 0;
        boolean whiteQueenMoved = false;
        boolean blackQueenMoved = false;

        phase = 0;

        recordBreakDown = breakDown;
        if (recordBreakDown) evalBreakDown = new int[EvalTerm.SIZE][2][2];

        for (int row = 0; row < board.getBoard().length; row++)
        {
            for (int col = 0; col < board.getBoard()[row].length; col++)
            {
                Piece currentPiece = board.getPiece(row, col);
                if (currentPiece.isOccupied())
                {
                    // skip king
                    if (currentPiece.getPiece() == 'K') continue;

                    int centipawns = term(currentPiece.value(), currentPiece.isWhite(), EvalTerm.MATERIAL.ordinal(), GamePhase.BOTH);

                    if (currentPiece.isWhite()) whiteMaterialValue += centipawns;
                    else blackMaterialValue += centipawns;
                    
                    int tableRow = currentPiece.isWhite()? row : 7 - row;



                    if (currentPiece.getPiece() == 'P')
                    {
                        midGameScore += term(PAWN_TABLE[tableRow][col], currentPiece.isWhite(), EvalTerm.PIECE_SQUARES.ordinal(), GamePhase.MID) * (currentPiece.isWhite()? 1 : -1);
                        endGameScore += term(PAWN_ENDGAME_TABLE[tableRow][col], currentPiece.isWhite(), EvalTerm.PIECE_SQUARES.ordinal(), GamePhase.END) * (currentPiece.isWhite()? 1 : -1);
                        if (currentPiece.isWhite()) whitePawnCount++;
                        else blackPawnCount++;

                        int PassedPawnBonus = 0;
                        int doubledPawnPenalty = 0;
                        int isolatedPawnPenalty = 0;

                        int adjacentPawnCounter = 0;
                        int enemyAdjacentPawnCounter = 0;

                        int forward = currentPiece.isWhite()? -1 : 1;
                        
                            for (int scanRow = row + forward; scanRow >= 0 && scanRow <= 7; scanRow += forward)
                            {
                                Piece scanPiece = board.getPiece(scanRow, col);
                                
                                if (scanPiece.getPiece() == 'P')
                                {
                                    if (scanPiece.isWhite() == currentPiece.isWhite()) doubledPawnPenalty += 20;
                                    else enemyAdjacentPawnCounter++; 
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
                                    if (isoScanRow < row && isoScanPiece == Piece.B_PAWN) enemyAdjacentPawnCounter++;
                                }
                                else
                                {
                                    if (isoScanRow > row && isoScanPiece == Piece.W_PAWN) enemyAdjacentPawnCounter++;
                                }
                            }
                        }
                        if (adjacentPawnCounter == 0) isolatedPawnPenalty = 15;
                        if (enemyAdjacentPawnCounter == 0) PassedPawnBonus = currentPiece.isWhite()? PASSED_PAWN_BONUS[7 - row] : PASSED_PAWN_BONUS[row];
                        endGameScore += term(PassedPawnBonus, currentPiece.isWhite(), EvalTerm.PASSED_PAWNS.ordinal(), GamePhase.END) * (currentPiece.isWhite()? 1 : -1);
                        centipawns += term(-doubledPawnPenalty, currentPiece.isWhite(), EvalTerm.DOUBLED_PAWNS.ordinal(), GamePhase.BOTH);
                        centipawns += term(-isolatedPawnPenalty, currentPiece.isWhite(), EvalTerm.ISOLATED_PAWNS.ordinal(), GamePhase.BOTH);
                    }
                    else if (currentPiece.getPiece() == 'N')
                    {
                        centipawns += term(KNIGHT_TABLE[tableRow][col], currentPiece.isWhite(), EvalTerm.PIECE_SQUARES.ordinal(), GamePhase.BOTH);
                        phase += 1;

                             if (((row == 7 && col == 1) || (row == 7 && col == 6)) && currentPiece.isWhite()) whiteMinorPiecesHome++;
                        else if (((row == 0 && col == 1) || (row == 0 && col == 6)) && !currentPiece.isWhite()) blackMinorPiecesHome++;
                    }
                    else if (currentPiece.getPiece() == 'B') 
                    {
                        centipawns += term(BISHOP_TABLE[tableRow][col], currentPiece.isWhite(), EvalTerm.PIECE_SQUARES.ordinal(), GamePhase.BOTH);
                        phase += 1;

                             if (((row == 7 && col == 2) || (row == 7 && col == 5)) && currentPiece.isWhite()) whiteMinorPiecesHome++;
                        else if (((row == 0 && col == 2) || (row == 0 && col == 5)) && !currentPiece.isWhite()) blackMinorPiecesHome++;

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

                        centipawns += term(-penalty, currPieceIsWhite, EvalTerm.BISHOP_LINES.ordinal(), GamePhase.BOTH);
                    }
                    else if (currentPiece.getPiece() == 'R')
                    {
                        centipawns += term(ROOK_TABLE[tableRow][col], currentPiece.isWhite(), EvalTerm.PIECE_SQUARES.ordinal(), GamePhase.BOTH);
                        phase += 2;

                        int forward = currentPiece.isWhite()? -1 : 1;

                        int penalty = 0;
                        int blockingPieces = 0;
                        for (int scanRow = row + forward; scanRow >= 0 && scanRow <= 7; scanRow += forward)
                        {
                            Piece scanPiece = board.getPiece(scanRow, col);
                            if (!scanPiece.isOccupied()) continue;
                            else blockingPieces++;
                            
                            // can x ray at most 2 pieces (pinned piece & target piece) 
                            if (blockingPieces > 2) break;

                            if (scanPiece.isWhite() == currentPiece.isWhite())
                            {
                                if (blockingPieces == 1) penalty += 20;
                            }
                            else if (scanPiece.getPiece() == 'K') penalty -= (20 / blockingPieces);
                            else if (scanPiece.getPiece() == 'Q') penalty -= (10 / blockingPieces);
                            else if (blockingPieces == 1) penalty += 10;
                        }
                    centipawns += term(-penalty, currentPiece.isWhite(), EvalTerm.ROOK_FILES.ordinal(), GamePhase.BOTH);
                    }
                    else if (currentPiece.getPiece() == 'Q')
                    {
                        centipawns += term(QUEEN_TABLE[tableRow][col], currentPiece.isWhite(), EvalTerm.PIECE_SQUARES.ordinal(), GamePhase.BOTH);
                        phase += 4;

                             if ((row != 7 || col != 3) && currentPiece.isWhite()) whiteQueenMoved = true;
                        else if ((row != 0 || col != 3) && !currentPiece.isWhite()) blackQueenMoved = true;
                    }
                    centipawns *= currentPiece.isWhite()? 1 : -1;

                    midGameScore += centipawns;
                    endGameScore += centipawns;
                }
            }
        }

        //early queen agro
        if (whiteQueenMoved && whiteMinorPiecesHome >= 2) midGameScore += term(-25, true, EvalTerm.EARLY_QUEEN.ordinal(), GamePhase.MID);
        if (blackQueenMoved && blackMinorPiecesHome >= 2) midGameScore -= term(-25, false, EvalTerm.EARLY_QUEEN.ordinal(), GamePhase.MID);

        //bishop pair
        if (whiteBishopCount >= 2) { int value = term(40, true, EvalTerm.BISHOP_PAIR.ordinal(), GamePhase.BOTH); midGameScore += value; endGameScore += value; }
        if (blackBishopCount >= 2) { int value = term(40, false, EvalTerm.BISHOP_PAIR.ordinal(), GamePhase.BOTH); midGameScore -= value; endGameScore -= value; }

        //insuficent material
        if (blackPawnCount == 0 && whitePawnCount == 0)
        {
            if (blackMaterialValue - (blackPawnCount * 100) <= 300 && whiteMaterialValue - (whitePawnCount * 100) <= 300) return 0;
        }



        //mop up check
        if (whiteMaterialValue - blackMaterialValue > 500 && blackMaterialValue < 300) endGameScore += term(evalMopup(blackKingLoc, whiteKingLoc, true), true, EvalTerm.MOP_UP.ordinal(), GamePhase.END);
        else if (blackMaterialValue - whiteMaterialValue > 500 && whiteMaterialValue < 300) endGameScore -= term(-evalMopup(whiteKingLoc, blackKingLoc, false), false, EvalTerm.MOP_UP.ordinal(), GamePhase.END);

        //king safety / endgame king
        midGameScore += kingSafety(board, whiteKingLoc, true);
        endGameScore += term(KING_ENDGAME_TABLE[whiteKingLoc[0]][whiteKingLoc[1]], true, EvalTerm.ENDGAME_KING.ordinal(), GamePhase.END);

        midGameScore += kingSafety(board, blackKingLoc, false);
        endGameScore -= term(KING_ENDGAME_TABLE[7 - blackKingLoc[0]][blackKingLoc[1]], false, EvalTerm.ENDGAME_KING.ordinal(), GamePhase.END);
        
        // blend middlegame and endgame scores by game phase (24 = all pieces, 0 = no pieces)
        phase = Math.min(phase, 24); // promotions can push phase over 24
        return (midGameScore * phase + endGameScore * (24 - phase)) / 24;
    }

    public static int kingSafety(Board board, int[] kingLoc, boolean isWhite)
    {
        int safetyBonus = 0;
        int sideMulti = isWhite? 1 : -1;
        
        // pawn sheild
        int pawnSheildBonus = 0;

        if (isWhite? !board.whiteCastled : !board.blackCastled) pawnSheildBonus = 30;
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
        int kingLocBonus = 0;
        kingLocBonus += KING_MIDDLEGAME_TABLE[isWhite? kingLoc[0] : 7 - kingLoc[0]][kingLoc[1]];
        
        //cant castle penalty
        int castlingRightsPenalty = 0;
        if (isWhite) { if (!board.whiteCastled && (!board.whiteCanCastleLong && !board.whiteCanCastleShort)) castlingRightsPenalty = -30; } 
        else { if (!board.blackCastled && (!board.blackCanCastleLong && !board.blackCanCastleShort)) castlingRightsPenalty = -30; }

        safetyBonus = term(pawnSheildBonus, isWhite, EvalTerm.KING_SHIELD.ordinal(), GamePhase.MID) + 
                      term(kingLocBonus, isWhite, EvalTerm.KING_SQUARE.ordinal(), GamePhase.MID) +
                      term(castlingRightsPenalty, isWhite, EvalTerm.CASTLING_RIGHTS.ordinal(), GamePhase.MID);
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

    public static String getEvalBreakDown (int eval)
    {
        StringBuilder result = new StringBuilder();
        int total = 0;
        int whiteMiddleGame = 0;
        int whiteEndGame = 0;
        int blackMiddleGame = 0;
        int blackEndGame = 0;

        String headerFormat = "%-19s %7s %7s %7s%n";
        String rowFormat    = "%-19s %7.2f %7.2f %+7.2f%n";

        result.append(String.format("phase: %d/24%n", phase));
        result.append(String.format(headerFormat, "term", "white", "black", "net"));
        result.append("-".repeat(40) + "\n");

        for (EvalTerm term : EvalTerm.values())
        {
            int wmg = evalBreakDown[term.ordinal()][0][0];
            int weg = evalBreakDown[term.ordinal()][0][1];
            int bmg = evalBreakDown[term.ordinal()][1][0];
            int beg = evalBreakDown[term.ordinal()][1][1];

            whiteMiddleGame += wmg;
            whiteEndGame += weg;
            blackMiddleGame += bmg;
            blackEndGame += beg;

            double white = (wmg * phase + weg * (24 - phase)) / 2400.0;
            double black = (bmg * phase + beg * (24 - phase)) / 2400.0;
            double net = white - black;
            result.append(String.format(rowFormat, (term.ordinal() + 1) + ". " + term.getName(), white, black, net));
        }

        total = ((whiteMiddleGame - blackMiddleGame) * phase + (whiteEndGame - blackEndGame) * (24 - phase)) / 24;
        result.append("-".repeat(40) + "\n");
        result.append(String.format("%-19s %15s %+7.2f   (matches evalBoard: %s)%n", "total", "", total / 100.0, total == eval? "yes" : "no"));
        return result.toString();
    }
}
