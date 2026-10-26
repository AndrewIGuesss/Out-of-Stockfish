import java.util.Arrays;

public class Eval 
{
    public static int[] params = new int[Param.TOTAL_SIZE];

    public static int phase = 0;

    // THIS WONT WORK WITH SMP
    // bit index read from right to left: 7 6 5 4 3 2 1 0
    private static int[] pawnPrecompBlack = new int[10];
    private static int[] pawnPrecompWhite = new int[10];

    // eval breakdown
    // evalBreakDown    [eval term index (.ordinal)]    [0 - white / 1 - black]    [0 - mid game / 1 - end game]
    public static int[][][] evalBreakDown = new int[EvalTerm.SIZE][2][2];
    private static boolean recordBreakDown = false;


    //indiv piece bonuses
    // piece square tables from whites view (row 0 is rank 8), black uses row 7 - row

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

    // fill params with the current values, same numbers as the tables and constants above
    // has to stay below the tables, static blocks run in file order
    static 
    {
        int[] material = {100, 300, 300, 500, 900};
        for (int i = 0; i < material.length; i++) params[Param.MATERIAL.offset() + i] = material[i];

        // piece square tables
        copyTable(PAWN_TABLE, Param.PAWN_PST_MG);
        copyTable(PAWN_ENDGAME_TABLE, Param.PAWN_PST_EG);
        copyTable(KNIGHT_TABLE, Param.KNIGHT_PST);
        copyTable(BISHOP_TABLE, Param.BISHOP_PST);
        copyTable(ROOK_TABLE, Param.ROOK_PST);
        copyTable(QUEEN_TABLE, Param.QUEEN_PST);
        copyTable(KING_MIDDLEGAME_TABLE, Param.KING_PST_MG);
        copyTable(KING_ENDGAME_TABLE, Param.KING_PST_EG);

        // pawn structure
        int[] passedPawn = {0, 10, 15, 25, 40, 60, 90, 0};
        for (int i = 0; i < passedPawn.length; i++) params[Param.PASSED_PAWN.offset() + i] = passedPawn[i];
        params[Param.DOUBLED_PAWN.offset()] = 20;
        params[Param.ISOLATED_PAWN.offset()] = 15;

        // mobility
        params[Param.KNIGHT_MOBILITY.offset()] = 4;
        params[Param.BISHOP_MOBILITY.offset()] = 5;
        params[Param.ROOK_MOBILITY_MG.offset()] = 2;
        params[Param.ROOK_MOBILITY_EG.offset()] = 4;
        params[Param.QUEEN_MOBILITY_MG.offset()] = 1;
        params[Param.QUEEN_MOBILITY_EG.offset()] = 2;

        // piece lines, order: own piece, enemy king, enemy queen, last slot
        int[] bishopLines = {10, 20, 10, 8};
        int[] rookFiles = {20, 20, 10, 10};
        for (int i = 0; i < 4; i++)
        {
            params[Param.BISHOP_LINES.offset() + i] = bishopLines[i];
            params[Param.ROOK_FILES.offset() + i] = rookFiles[i];
        }

        // misc
        params[Param.EARLY_QUEEN.offset()] = -25;
        params[Param.BISHOP_PAIR.offset()] = 40;

        // king safety
        params[Param.KING_SHIELD.offset()] = 12;            // edge file 
        params[Param.KING_SHIELD.offset() + 1] = 12;        // shield pawn
        params[Param.KING_OPEN_FILE.offset()] = -10;        // half open
        params[Param.KING_OPEN_FILE.offset() + 1] = -20;    // open
        params[Param.CASTLING_RIGHTS.offset()] = -30;

        params[Param.MOP_UP.offset()] = 10;     // center distance
        params[Param.MOP_UP.offset() + 1] = 4;  // king distance
    }

    private static void copyTable(int[][] table, Param param)
    {
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                params[param.offset() + row * 8 + col] = table[row][col];
            }
        }
    }

    private static int fileIndex(int col) {return  col + 1;}

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
        Arrays.fill(pawnPrecompBlack, 0);
        Arrays.fill(pawnPrecompWhite, 0);

        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++) 
            {
                Piece scanPiece = board.getPiece(row, col);

                if (scanPiece == Piece.W_PAWN) pawnPrecompWhite[fileIndex(col)] |= (1 << row);
                else if (scanPiece == Piece.B_PAWN) pawnPrecompBlack[fileIndex(col)] |= (1 << row);
            }
        }

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

                    int centipawns = term(params[Param.MATERIAL.offset() + currentPiece.ordinal() % 6], currentPiece.isWhite(), EvalTerm.MATERIAL.ordinal(), GamePhase.BOTH);

                    if (currentPiece.isWhite()) whiteMaterialValue += currentPiece.value();
                    else blackMaterialValue += currentPiece.value();
                    
                    int tableRow = currentPiece.isWhite()? row : 7 - row;



                    if (currentPiece.getPiece() == 'P')
                    {
                        midGameScore += term(params[Param.PAWN_PST_MG.offset() + (tableRow * 8 + col)], currentPiece.isWhite(), EvalTerm.PIECE_SQUARES.ordinal(), GamePhase.MID) * (currentPiece.isWhite()? 1 : -1);
                        endGameScore += term(params[Param.PAWN_PST_EG.offset() + (tableRow * 8 + col)], currentPiece.isWhite(), EvalTerm.PIECE_SQUARES.ordinal(), GamePhase.END) * (currentPiece.isWhite()? 1 : -1);
                        if (currentPiece.isWhite()) whitePawnCount++;
                        else blackPawnCount++;

                        int passedPawnBonus = 0;
                        int doubledPawnPenalty = 0;
                        int isolatedPawnPenalty = 0;

                        
                        if (currentPiece.isWhite())
                        {
                            int whiteAheadMask = (1 << row) - 1;

                            // doubled
                            int whiteBitDoubledPawns = whiteAheadMask & pawnPrecompWhite[fileIndex(col)];
                            doubledPawnPenalty = Integer.bitCount(whiteBitDoubledPawns) * params[Param.DOUBLED_PAWN.offset()];

                            // isolated
                            if ((pawnPrecompWhite[fileIndex(col) + 1] | pawnPrecompWhite[fileIndex(col) - 1]) == 0) isolatedPawnPenalty = params[Param.ISOLATED_PAWN.offset()];
                        
                            // passed
                            if (((pawnPrecompBlack[fileIndex(col) - 1] | pawnPrecompBlack[fileIndex(col)] | pawnPrecompBlack[fileIndex(col) + 1]) & whiteAheadMask) == 0)
                            { passedPawnBonus = params[Param.PASSED_PAWN.offset() + 7 - row]; }
                        }
                        else
                        {
                            int blackAheadMask = ~((1 << row + 1) - 1) & 0xFF;
                            
                            // doubled
                            int blackBitDoubledPawns = blackAheadMask & pawnPrecompBlack[fileIndex(col)];
                            doubledPawnPenalty = Integer.bitCount(blackBitDoubledPawns) * params[Param.DOUBLED_PAWN.offset()];

                            // isolated
                            if ((pawnPrecompBlack[fileIndex(col) + 1] | pawnPrecompBlack[fileIndex(col) - 1]) == 0) isolatedPawnPenalty = params[Param.ISOLATED_PAWN.offset()];
                        
                            // passed
                            if (((pawnPrecompWhite[fileIndex(col) - 1] | pawnPrecompWhite[fileIndex(col)] | pawnPrecompWhite[fileIndex(col) + 1]) & blackAheadMask) == 0)
                            { passedPawnBonus = params[Param.PASSED_PAWN.offset() + row]; }
                        }

                        endGameScore += term(passedPawnBonus, currentPiece.isWhite(), EvalTerm.PASSED_PAWNS.ordinal(), GamePhase.END) * (currentPiece.isWhite()? 1 : -1);
                        centipawns += term(-doubledPawnPenalty, currentPiece.isWhite(), EvalTerm.DOUBLED_PAWNS.ordinal(), GamePhase.BOTH);
                        centipawns += term(-isolatedPawnPenalty, currentPiece.isWhite(), EvalTerm.ISOLATED_PAWNS.ordinal(), GamePhase.BOTH);
                    }
                    else if (currentPiece.getPiece() == 'N')
                    {
                        centipawns += term(params[Param.KNIGHT_PST.offset() + (tableRow * 8 + col)], currentPiece.isWhite(), EvalTerm.PIECE_SQUARES.ordinal(), GamePhase.BOTH);

                        int mobility = Moves.mobilityCount(board, row, col, Moves.KNIGHT_R, Moves.KNIGHT_C, false) * params[Param.KNIGHT_MOBILITY.offset()];
                        centipawns += term(mobility, currentPiece.isWhite(), EvalTerm.PIECE_MOBILITY.ordinal(), GamePhase.BOTH);
                        
                        phase += 1;

                             if (((row == 7 && col == 1) || (row == 7 && col == 6)) && currentPiece.isWhite()) whiteMinorPiecesHome++;
                        else if (((row == 0 && col == 1) || (row == 0 && col == 6)) && !currentPiece.isWhite()) blackMinorPiecesHome++;
                    }
                    else if (currentPiece.getPiece() == 'B') 
                    {
                        centipawns += term(params[Param.BISHOP_PST.offset() + (tableRow * 8 + col)], currentPiece.isWhite(), EvalTerm.PIECE_SQUARES.ordinal(), GamePhase.BOTH);
                        
                        int mobility = Moves.mobilityCount(board, row, col, Moves.DIAG_R, Moves.DIAG_C, true) * params[Param.BISHOP_MOBILITY.offset()];
                        centipawns += term(mobility, currentPiece.isWhite(), EvalTerm.PIECE_MOBILITY.ordinal(), GamePhase.BOTH);
                        
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
                                    if (blockingPieces == 1) penalty += params[Param.BISHOP_LINES.offset()];
                                }
                                else if (scanPiece.getPiece() == 'K') penalty -= (params[Param.BISHOP_LINES.offset() + 1] / blockingPieces);
                                else if (scanPiece.getPiece() == 'Q') penalty -= (params[Param.BISHOP_LINES.offset() + 2] / blockingPieces);
                                else if (scanPiece.getPiece() == 'P') penalty += (params[Param.BISHOP_LINES.offset() + 3] / blockingPieces);
                            }
                        }

                        centipawns += term(-penalty, currPieceIsWhite, EvalTerm.BISHOP_LINES.ordinal(), GamePhase.BOTH);
                    }
                    else if (currentPiece.getPiece() == 'R')
                    {
                        centipawns += term(params[Param.ROOK_PST.offset() + (tableRow * 8 + col)], currentPiece.isWhite(), EvalTerm.PIECE_SQUARES.ordinal(), GamePhase.BOTH);
                        
                        int mobility = Moves.mobilityCount(board, row, col, Moves.STRAIGHT_R, Moves.STRAIGHT_C, true);
                        midGameScore += term(mobility * params[Param.ROOK_MOBILITY_MG.offset()], currentPiece.isWhite(), EvalTerm.PIECE_MOBILITY.ordinal(), GamePhase.MID) * (currentPiece.isWhite()? 1 : -1);
                        endGameScore += term(mobility * params[Param.ROOK_MOBILITY_EG.offset()], currentPiece.isWhite(), EvalTerm.PIECE_MOBILITY.ordinal(), GamePhase.END) * (currentPiece.isWhite()? 1 : -1);

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
                                if (blockingPieces == 1) penalty += params[Param.ROOK_FILES.offset()];
                            }
                            else if (scanPiece.getPiece() == 'K') penalty -= (params[Param.ROOK_FILES.offset() + 1] / blockingPieces);
                            else if (scanPiece.getPiece() == 'Q') penalty -= (params[Param.ROOK_FILES.offset() + 2] / blockingPieces);
                            else if (blockingPieces == 1) penalty += params[Param.ROOK_FILES.offset() + 3];
                        }
                    centipawns += term(-penalty, currentPiece.isWhite(), EvalTerm.ROOK_FILES.ordinal(), GamePhase.BOTH);
                    }
                    else if (currentPiece.getPiece() == 'Q')
                    {
                        centipawns += term(params[Param.QUEEN_PST.offset() + (tableRow * 8 + col)], currentPiece.isWhite(), EvalTerm.PIECE_SQUARES.ordinal(), GamePhase.BOTH);
                        
                        int mobility = Moves.mobilityCount(board, row, col, Moves.QUEEN_R, Moves.QUEEN_C, true);
                        midGameScore += term(mobility * params[Param.QUEEN_MOBILITY_MG.offset()], currentPiece.isWhite(), EvalTerm.PIECE_MOBILITY.ordinal(), GamePhase.MID) * (currentPiece.isWhite()? 1 : -1);
                        endGameScore += term(mobility * params[Param.QUEEN_MOBILITY_EG.offset()], currentPiece.isWhite(), EvalTerm.PIECE_MOBILITY.ordinal(), GamePhase.END) * (currentPiece.isWhite()? 1 : -1);
                        
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
        if (whiteQueenMoved && whiteMinorPiecesHome >= 2) midGameScore += term(params[Param.EARLY_QUEEN.offset()], true, EvalTerm.EARLY_QUEEN.ordinal(), GamePhase.MID);
        if (blackQueenMoved && blackMinorPiecesHome >= 2) midGameScore -= term(params[Param.EARLY_QUEEN.offset()], false, EvalTerm.EARLY_QUEEN.ordinal(), GamePhase.MID);

        //bishop pair
        if (whiteBishopCount >= 2) { int value = term(params[Param.BISHOP_PAIR.offset()], true, EvalTerm.BISHOP_PAIR.ordinal(), GamePhase.BOTH); midGameScore += value; endGameScore += value; }
        if (blackBishopCount >= 2) { int value = term(params[Param.BISHOP_PAIR.offset()], false, EvalTerm.BISHOP_PAIR.ordinal(), GamePhase.BOTH); midGameScore -= value; endGameScore -= value; }

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
        endGameScore += term(params[Param.KING_PST_EG.offset() + whiteKingLoc[0] * 8 + whiteKingLoc[1]], true, EvalTerm.ENDGAME_KING.ordinal(), GamePhase.END);

        midGameScore += kingSafety(board, blackKingLoc, false);
        endGameScore -= term(params[Param.KING_PST_EG.offset() + (7 - blackKingLoc[0]) * 8 + blackKingLoc[1]], false, EvalTerm.ENDGAME_KING.ordinal(), GamePhase.END);
        
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
        boolean[] pawnsFound = new boolean[3];
        for (int col = -1; col <= 1; col++)
        {
            if (!board.inBounds(kingLoc[0], kingLoc[1] + col))
            {
                pawnSheildBonus += params[Param.KING_SHIELD.offset()];
                continue;
            }
            rowScan: for (int row = 1; row <= 2; row++)
            {
                if (board.inBounds(kingLoc[0] - sideMulti * row, kingLoc[1] + col))
                {
                    Piece scanPiece = board.getPiece(kingLoc[0] - sideMulti * row, kingLoc[1] + col);
                    if (scanPiece.getPiece() == 'P' && scanPiece.isWhite() == isWhite)
                    {
                        pawnsFound[col + 1] = true;
                        pawnSheildBonus += params[Param.KING_SHIELD.offset() + 1] / row;
                        break rowScan;
                    }
                }
                else break rowScan;
            }
        }

        // empty file
        int openFilePenalty = 0;
        for (int i = 0; i < 3; i++)
        {
            if (pawnsFound[i]) continue;
            int col = i + (kingLoc[1] - 1);
            if (!board.inBounds(0, col)) continue;

            if (isWhite)
            {
                if (pawnPrecompWhite[fileIndex(col)] != 0) continue;
                else if (pawnPrecompBlack[fileIndex(col)] != 0) openFilePenalty += params[Param.KING_OPEN_FILE.offset()];
                else openFilePenalty += params[Param.KING_OPEN_FILE.offset() + 1];
            }
            else
            {
                if (pawnPrecompBlack[fileIndex(col)] != 0) continue;
                else if (pawnPrecompWhite[fileIndex(col)] != 0) openFilePenalty += params[Param.KING_OPEN_FILE.offset()];
                else openFilePenalty += params[Param.KING_OPEN_FILE.offset() + 1];
            }
        }
        
        // king location
        int kingLocBonus = 0;
        kingLocBonus += params[Param.KING_PST_MG.offset() + (isWhite? kingLoc[0] : 7 - kingLoc[0]) * 8 + kingLoc[1]];
        
        // cant castle penalty
        int castlingRightsPenalty = 0;
        if (isWhite) { if (!board.whiteCastled && (!board.whiteCanCastleLong && !board.whiteCanCastleShort)) castlingRightsPenalty = params[Param.CASTLING_RIGHTS.offset()]; } 
        else { if (!board.blackCastled && (!board.blackCanCastleLong && !board.blackCanCastleShort)) castlingRightsPenalty = params[Param.CASTLING_RIGHTS.offset()]; }

        safetyBonus = 
                    term(pawnSheildBonus, isWhite, EvalTerm.KING_SHIELD.ordinal(), GamePhase.MID) + 
                    term(openFilePenalty, isWhite, EvalTerm.KING_OPEN_FILES.ordinal(), GamePhase.MID) + 
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

        int bonus = centerDistance * params[Param.MOP_UP.offset()] + (7 - kingDistance) * params[Param.MOP_UP.offset() + 1];
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
