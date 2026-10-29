import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Bot 
{
    public static volatile boolean stopThinking = false;
    public static boolean uciMode = false;

    public static final int MATE = 123456789;
    private static final int INFINITY = 1234567890;

    private static final int MAX_DEPTH_EXTENTION = 20;

    private static final int TT_MOVE_SCORE = 5000000;
    private static final int CAPTURE_BASE = 1000000;

    private static final int KILLER_1 = 900000;
    private static final int KILLER_2 = 800000;
    private static final int HISTORY_MAX = 799999;

    private static final int MAX_PLY = 64;
    private static final int KILLER_SLOTS = 2;
    private static final int SQUARES = 64;
    private static final int QUEEN_VALUE = 900;

    private static final int NULL_MOVE_REDUCTION = 2;

    private static int[][] LMR_TABLE = new int[64][64];
    private static double LMR_BASE = 0.75;
    private static double LMR_DIVISOR = 2.25;

    private static final int LMP_MAX_DEPTH = 2;
    private static final int LMP_MULTIPLIER = 4;
    private static final int LMP_BASE = 12; 
    
    private static final int RFP_CENTIPAWNS_PER_DEPTH = 90;
    private static final int RFP_MAX_DEPTH = 6;

    private static final int FP_MAX_DEPTH = 3;
    private static final int FP_MARGIN = 125;

    private static final int TEMPO_BONUS = 15;
    private static final int DELTA_MARGIN = 200; // delta pruning
    private static final int WINDOW = 35;
    public static String pv;
    public static String rootScores = "";
    public static boolean exactRootScores = false; // true = exact root score (slower)

    public static int movesAnalysed = 0;
    public static int quiescenceMovesAnalysed = 0;
    public static int depthReached = 0;

    public static long softTimeLimit = Long.MAX_VALUE;
    static long deadline = System.currentTimeMillis() + Settings.botSearchTimeLimit;
    static boolean timeUp = false;
    static boolean timeCanBeUp = false;

    private static final int BAD_CAPTURE_BASE = -1000000;
    
    //   [ply]   [slot 0 newest / 1 older]   -> move (Moves.NO_MOVE when empty)
    public static int[][] killerMoves = new int[MAX_PLY][KILLER_SLOTS];
    private static final int MOVE_BUFFER_PLY = 128;
    private static final int[][] moveBuffer = new int[MOVE_BUFFER_PLY][Moves.MAX_MOVES];
    private static final int[][] scoreBuffer = new int[MOVE_BUFFER_PLY][Moves.MAX_MOVES];
    //   [from square 0-63]   [to square 0-63]   -> beta cutoff
    public static int[][] historyTable = new int[SQUARES][SQUARES];

    // repetition check
    public static long[] plyPathKeys = new long[MAX_PLY];
    public static long[] posKeys;
    
    static 
    {
        for (int depth = 1; depth < LMR_TABLE.length; depth++)
        {
            for (int moveNumber = 1; moveNumber < LMR_TABLE[depth].length; moveNumber++)
            {
                LMR_TABLE[depth][moveNumber] = (int)(LMR_BASE + Math.log(depth) * Math.log(moveNumber) / LMR_DIVISOR);
            }
        }
    }


    public static String getPV(Board board, int length, boolean whiteTurn)
    {
        List<Integer> pvMoves = new ArrayList<>();
        List<Undo> undos = new ArrayList<>();
        List<Long> hashs = new ArrayList<>();
        StringBuilder pv = new StringBuilder();

        pv: while (pvMoves.size() < length)
        {
            long newHash = Zobrist.makeZobristHash(board, whiteTurn);
            
            if (board.getTimeSinceCaptureOrPawnPush() >= 100) break pv;
            if (Game.repetition.containsKey(newHash) || hashs.contains(newHash)) break pv;
            hashs.add(newHash);

            int index = (int)(newHash & (TTEntry.TT_SIZE - 1));
            TTEntry entry = TTEntry.ttTable[index];

            if (entry == null || entry.key() != newHash) break pv;

            int ttMove = entry.bestmove();

            if (ttMove == 0) break pv;
            
            List<Integer> moves = Moves.pseudoMoves(board, whiteTurn);
            boolean legal = false;

            int[] start = Moves.coordsStart(ttMove);
            int[] end = Moves.coordsEnd(ttMove);
            
            for (int i = 0; i < moves.size(); i++)
            {
                if (ttMove == moves.get(i))
                {
                    legal = true;
                    break;
                }
            }
            if (!legal) break pv;

            String move = Board.convertMove(start) + Board.convertMove(end);
            if (board.getPiece(start).getPiece() == 'P' && (end[0] == 0 || end[0] == 7)) move += ("q");

            Undo u = board.makeMove(start, end, 'q');
            if (Moves.inCheck(board, board.getKingLoc(whiteTurn), whiteTurn))
            {
                board.unMakeMove(start, end, u);
                break pv;
            }
            else undos.add(u);

            pv.append(move + " ");
            pvMoves.add(ttMove);

            whiteTurn = !whiteTurn;
        }


        for (int i = pvMoves.size() - 1; i >= 0; i--)
        {
            board.unMakeMove(Moves.coordsStart(pvMoves.get(i)), Moves.coordsEnd(pvMoves.get(i)), undos.get(i));
        }
        return pv.toString().trim();
    }

    public static boolean isCapture(Board board, int move)
    {
        int[] start = Moves.coordsStart(move);
        int[] end = Moves.coordsEnd(move);
        int[] ep = board.getEnPassantSquare();
        if (board.getPiece(end).isOccupied()) return true;
        if (board.getPiece(start).getPiece() == 'P' && ep != null && ep[0] == end[0] && ep[1] == end[1]) return true;
        return false;
    }

    public static int orderScore (Board board, int move, int ply)
    {
        // MVV-LVA = most valuable victim, least valuable attacker
        int startRow = (move / 100) / 8;
        int startCol = (move / 100) % 8;
        int endRow = (move % 100) / 8;
        int endCol = (move % 100) % 8;

        Piece attacker = board.getPiece(startRow, startCol);

        int victimValue = 0;
        int attackerValue = attacker.value();
        if (attacker.getPiece() == 'K') attackerValue = 50;
        
        boolean isPromotion = (endRow == 7 || endRow == 0) && attacker.getPiece() == 'P';

        if (isCapture(board, move) || isPromotion) 
        {
            victimValue = board.getPiece(endRow, endCol).value();
            int[] ep = board.getEnPassantSquare();
            if (ep != null && ep[0] == endRow && ep[1] == endCol) victimValue = 100;

            if (!isPromotion && attacker.value() > victimValue && Moves.see(board, move) < 0) return BAD_CAPTURE_BASE + victimValue * 10 - attackerValue;
            
            if (isPromotion) victimValue += 900;
            return CAPTURE_BASE + victimValue * 10 - attackerValue; 
        }

        if (ply == -1) return 0;

        if (killerMoves[ply][0] == move) return KILLER_1;
        else if (killerMoves[ply][1] == move) return KILLER_2;
        else return Math.min(historyTable[move / 100][move % 100], HISTORY_MAX);
    }

    public static boolean hasPromotingPawn(Board board, boolean isWhite)
    {
        if (isWhite)
        {
            for (int i = 7; i >= 0; i--)
            {
                if (board.getPiece(1, i) == Piece.W_PAWN) return true;
            }
        }
        else
        {
            for (int i = 0; i <= 7; i++)
            {
                if (board.getPiece(6, i) == Piece.B_PAWN) return true;
            }
        }
        return false;
    }

    public static void clearHistory()
    {
        historyTable = new int[SQUARES][SQUARES];
    }



    public static int quiescence(Board board, boolean whiteTurn, int alpha, int beta, int ply)
    {
        // time limit check
        if ((movesAnalysed & 1023) == 0)
        {
            if (System.currentTimeMillis() > deadline && timeCanBeUp)
            { 
                timeUp = true;
                return 0;
            }
        }

        if (Board.isInsufficientMaterial(board)) return 0;

        // node counters
        movesAnalysed++;
        quiescenceMovesAnalysed++;

        // stand pat (eval if we stop capturing here)
        int standPat = Eval.evalBoard(board, false) * (whiteTurn? 1 : -1) + TEMPO_BONUS;
        if (standPat >= beta) return standPat;
        
        alpha = Math.max(alpha, standPat);
        int best = standPat;

        if (standPat + QUEEN_VALUE + DELTA_MARGIN <= alpha && !hasPromotingPawn(board, whiteTurn)) return best;
        
        if (ply >= MOVE_BUFFER_PLY) return best;

        // captures only (MVV-LVA ordered)
        int[] moves = moveBuffer[ply];
        int[] scores = scoreBuffer[ply];
        int moveCount = Moves.pseudoMoves(board, whiteTurn, moves);
        int captureCount = 0;

        // keep only captures / promotions at the front of the buffer (same order as before)
        for (int i = 0; i < moveCount; i++)
        {
            int currOrderScore = orderScore(board, moves[i], -1);
            if (currOrderScore >= CAPTURE_BASE) 
            {
                moves[captureCount] = moves[i];
                scores[captureCount] = currOrderScore;
                captureCount++;
            }
        }
        
        // search captures
        for (int i = 0; i < captureCount; i++)
        {
            int high = Integer.MIN_VALUE;
            int highIndex = i;
            for (int j = i; j < captureCount; j++)
            {
                if (scores[j] > high)
                {
                    high = scores[j];
                    highIndex = j;
                }
            }
            scores[highIndex] = scores[i];
            scores[i] = high;
            int swap = moves[highIndex];
            moves[highIndex] = moves[i];
            moves[i] = swap;

            int capture = moves[i];
            
            int[] start = Moves.coordsStart(capture);
            int[] end = Moves.coordsEnd(capture);
            
            int[] ep = board.getEnPassantSquare();
            boolean promoting = board.getPiece(start).getPiece() == 'P' && (end[0] == 7 || end[0] == 0);

            int value = Arrays.equals(end, ep)? 100 : board.getPiece(end).value();

            if (!promoting && standPat + value + DELTA_MARGIN <= alpha) continue;

            Undo u = board.makeMove(start, end, 'Q');
            if (Moves.inCheck(board, board.getKingLoc(whiteTurn), whiteTurn))
            {
                board.unMakeMove(start, end, u);
                continue;
            } 

            int score = -quiescence(board, !whiteTurn, -beta, -alpha, ply + 1);

            board.unMakeMove(start, end, u);

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
        int bestMoveHere = Moves.NO_MOVE;

        // 50 move rule
        if (board.getTimeSinceCaptureOrPawnPush() >= 100) return 0;

        if (Board.isInsufficientMaterial(board)) return 0;
        
        // zobrist hash + repetition detection
        long currKey = Zobrist.makeZobristHash(board, whiteTurn);
        if (ply > 0)
        {
            for (int i = 0; i > posKeys.length; i++)
            {
                if (posKeys[i] == currKey) return 0;
            }
            for (int i = 0; i < ply; i++)
            {
                if (plyPathKeys[i] == currKey) return 0;
            }
            plyPathKeys[ply] = currKey;
        }

        // time limit / stop thinking check
        if ((movesAnalysed & 1023) == 0)
        {
            if ((System.currentTimeMillis() > deadline && timeCanBeUp) || stopThinking && timeCanBeUp)
            { 
                timeUp = true;
                return 0;
            }
        }

        // node counter
        movesAnalysed++;

        // check depth extension
        boolean inCheck = Moves.inCheck(board, board.getKingLoc(whiteTurn), whiteTurn);
        if (inCheck && ply < MAX_DEPTH_EXTENTION) maxDepth++;

        // transposition table
        int ttMove = Moves.NO_MOVE;
        int index = (int)(currKey & (TTEntry.TT_SIZE - 1));
        TTEntry entry = TTEntry.ttTable[index];
        if (entry != null && entry.key() == currKey)
        {
            ttMove = entry.bestmove();
            if (entry.depth() >= maxDepth)
            {
                int ttScore = entry.score();
                if (ttScore > MATE - 1000) ttScore -= ply;
                if (ttScore < -(MATE - 1000)) ttScore += ply;
                switch (entry.flag()) 
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

        // quiescence at max depth
        if (maxDepth == 0) return quiescence(board, whiteTurn, alpha, beta, ply);

        int staticEval = -1;

        if (!inCheck && maxDepth <= RFP_MAX_DEPTH && beta - alpha == 1) staticEval = Eval.evalBoard(board, false) * (whiteTurn? 1 : -1) + TEMPO_BONUS;
        // reverse futility pruning (RFP)
        if (!inCheck && maxDepth <= RFP_MAX_DEPTH && ply > 0 && beta - alpha == 1 && Math.abs(beta) < MATE - 1000)
        {
            if (staticEval - RFP_CENTIPAWNS_PER_DEPTH * maxDepth >= beta) return staticEval;
        }
        
        // null move pruning
        if (ply != 0 && maxDepth >= 4 && !isNullMove && !inCheck)
        {
            if (board.hasNonPawnMaterial(whiteTurn))
            {
                int[] undoEnPassant = board.getEnPassantSquare();
                board.setEnPassantSquare(null);

                int score = -negamax(board, maxDepth - 1 - NULL_MOVE_REDUCTION, !whiteTurn, -beta, -beta + 1, ply + 1, true);

                board.setEnPassantSquare(undoEnPassant);

                if (score >= beta) return beta;
            }
        }

        // move generation + ordering (MVV-LVA, killers, history)
        int[] moves = moveBuffer[ply];
        int[] scores = scoreBuffer[ply];
        int moveCount = Moves.pseudoMoves(board, whiteTurn, moves);
        
        int best = -MATE; // extremly low place holder value
        for (int i = 0; i < moveCount; i++)
        {
            scores[i] = orderScore(board, moves[i], ply);
        }

        
        // try TT move first
        if (ttMove != Moves.NO_MOVE)
        {
            for (int i = 0; i < moveCount; i++)
            {
                if (ttMove == moves[i])
                {
                    scores[i] = TT_MOVE_SCORE; // give big value so it gets sorted to the top
                    break;
                }
            }
        }

        // main search loop
        int validMoves = 0;
        
        for (int i = 0; i < moveCount; i++)
        {
            int high = Integer.MIN_VALUE;
            int highIndex = i;
            for (int j = i; j < moveCount; j++)
            {
                if (scores[j] > high)
                {
                    high = scores[j];
                    highIndex = j;
                }
            }
            scores[highIndex] = scores[i];
            scores[i] = high;
            int swap = moves[highIndex];
            moves[highIndex] = moves[i];
            moves[i] = swap;
            

            int move = moves[i]; 

            int[] start = Moves.coordsStart(move);
            int[] end = Moves.coordsEnd(move);

            boolean promoting = board.getPiece(start).getPiece() == 'P' && (end[0] == 7 || end[0] == 0);
            boolean capture = isCapture(board, move);

            Undo u = board.makeMove(start, end, 'Q');
            if (Moves.inCheck(board, board.getKingLoc(whiteTurn), whiteTurn))
            {
                board.unMakeMove(start, end, u);
                continue;
            }
            
            validMoves++;

            boolean quiet = !inCheck && !capture && !promoting && validMoves > 1;

            if (beta - alpha == 1 && maxDepth <= LMP_MAX_DEPTH && quiet && Math.abs(alpha) < MATE - 1000)
            {
                if (validMoves > LMP_BASE + LMP_MULTIPLIER * maxDepth * maxDepth && !Moves.inCheck(board, board.getKingLoc(!whiteTurn), !whiteTurn))
                {
                    board.unMakeMove(start, end, u);
                    continue;
                }
            }

            // futility pruning
            if (beta - alpha == 1 && maxDepth <= FP_MAX_DEPTH && !inCheck && Math.abs(alpha) < MATE - 1000 && validMoves > 1)
            {
                if (!capture && !promoting && staticEval + FP_MARGIN * maxDepth <= alpha)
                {
                    if (!Moves.inCheck(board, board.getKingLoc(!whiteTurn), !whiteTurn)) 
                    {
                        board.unMakeMove(start, end, u);
                        continue;
                    }
                }
            }

            // LMR (late move reductions)
            boolean searched = false;
            int score = 0;
            int reduction = LMR_TABLE[Math.min(maxDepth, 63)][Math.min(validMoves, 63)];
            
            if (validMoves > 4 && maxDepth >= 3 && killerMoves[ply][0] != move && !inCheck && !capture)
            {
                if (!promoting && reduction != 0 && !Moves.inCheck(board, board.getKingLoc(!whiteTurn), !whiteTurn))
                {
                    searched = true;
                    // reduced depth, null window
                    score = -negamax(board, Math.max(maxDepth - 1 - reduction, 1), !whiteTurn, -alpha - 1, -alpha, ply + 1, false);
                    if (score > alpha)
                    {
                        // full depth, null window
                        score = -negamax(board, maxDepth - 1, !whiteTurn, -alpha - 1, -alpha, ply + 1, false);
                        if (score > alpha && score < beta)
                        {
                            // full depth, full window
                            score = -negamax(board, maxDepth - 1, !whiteTurn, -beta, -alpha, ply + 1, false);
                        }
                    }
                }
            }

            // PVS (principal variation search)
            if (!searched)
            {
                if (validMoves == 1)
                {
                    score = -negamax(board, maxDepth - 1, !whiteTurn, -beta, -alpha, ply + 1, false);
                }
                else
                {
                    // full depth, null window
                    score = -negamax(board, maxDepth - 1, !whiteTurn, -alpha - 1, -alpha, ply + 1, false);
                    if (score > alpha && score < beta)
                    {
                        // full depth, full window
                        score = -negamax(board, maxDepth - 1, !whiteTurn, -beta, -alpha, ply + 1, false);
                    }
                }
            }

            board.unMakeMove(start, end, u);

            if (score > best)
            {
                bestMoveHere = move;
                best = score;
                alpha = Math.max(alpha, score);
            }
            // beta cutoff: save killer + history for quiet moves
            if (alpha >= beta)
            {
                if (!capture && !promoting)
                {
                    if (killerMoves[ply][0] != move)
                    {
                        killerMoves[ply][1] = killerMoves[ply][0];
                        killerMoves[ply][0] = move;
                    }
                    int from = Moves.startSquare(move);
                    int to = Moves.endSquare(move);
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
        killerMoves = new int[MAX_PLY][KILLER_SLOTS];

        maxDepth = Math.min(maxDepth, MAX_PLY - MAX_DEPTH_EXTENTION - 1);
        historyTable = new int[SQUARES][SQUARES];

        posKeys = new long[Game.repetition.size()];
        int counter = 0;
        for (long key : Game.repetition.keySet())
        {
            posKeys[counter] = key;
            counter++;
        }

        // stats
        depthReached = 0;

        movesAnalysed = 0;
        quiescenceMovesAnalysed = 0;

        double startTime = System.currentTimeMillis();

        // root move generation + ordering
        int[] moves = new int[Moves.MAX_MOVES];
        int moveCount = Moves.pseudoMoves(board, whiteTurn, moves);

        // alpha - the best score we've already found
        // beta - the score where the opponent would never allow this position

        int bestScore = -INFINITY;
        int bestScoreSoFar = -INFINITY;
        int alpha = -INFINITY;
        int beta = INFINITY;
        int bestMove = Moves.NO_MOVE;
        int bestMoveSoFar = Moves.NO_MOVE;
        int previousScore = -INFINITY;

        // sort order score
        int[] rootOrder = new int[moveCount];
        for (int i = 0; i < moveCount; i++) rootOrder[i] = orderScore(board, moves[i], -1);
        for (int i = 1; i < moveCount; i++)
        {
            int move = moves[i];
            int order = rootOrder[i];
            int j = i - 1;
            while (j >= 0 && rootOrder[j] < order)
            {
                moves[j + 1] = moves[j];
                rootOrder[j + 1] = rootOrder[j];
                j--;
            }
            moves[j + 1] = move;
            rootOrder[j + 1] = order;
        }
        // root position key for repetition detection
        plyPathKeys[0] = Zobrist.makeZobristHash(board, whiteTurn);

        // iterative deepening
        for (int depth = 1; depth <= maxDepth; depth++)
        {
            if (depth < 4 || Math.abs(previousScore) >= MATE - 1000 || exactRootScores)
            {
                alpha = -INFINITY;
                beta = INFINITY;
            }
            else
            {   
                alpha = previousScore - WINDOW;
                beta = previousScore + WINDOW;
            }
            StringBuilder depthScores;
            int aspAlpha = alpha;
            int aspBeta = beta;
            
            aspiration: while (true)
            {
                alpha = aspAlpha;
                beta = aspBeta;
                bestScoreSoFar = -INFINITY;
                bestMoveSoFar = Moves.NO_MOVE;
                depthScores = new StringBuilder();

                // root search loop
                roots: for (int m = 0; m < moveCount; m++)
                {
                    int move = moves[m];
                    int[] start = Moves.coordsStart(move);
                    int[] end = Moves.coordsEnd(move);
                    
                    Undo u = board.makeMove(start, end, 'Q');
                    if (Moves.inCheck(board, board.getKingLoc(whiteTurn), whiteTurn)) 
                    {
                        board.unMakeMove(start, end, u);
                        continue;
                    }
                    
                    if (bestMoveSoFar == Moves.NO_MOVE) bestMoveSoFar = move;

                    int score = -negamax(board, depth - 1, !whiteTurn, -beta, -alpha, 1, false);
                    board.unMakeMove(start, end, u);

                    if (timeUp) break;

                    // root scores display
                    depthScores.append(Board.convertMove(start) + Board.convertMove(end));
                    depthScores.append((!exactRootScores && score <= alpha && bestMoveSoFar != move)? " <= " : " = ");
                    depthScores.append(score / 100.0 + "\n");

                    if (score > bestScoreSoFar) 
                    {
                        bestScoreSoFar = score; bestMoveSoFar = move;
                        if (!exactRootScores) alpha = Math.max(alpha, score);
                    }
                    if (score >= beta) break roots;
                }
                if (timeUp) break;
                if (bestScoreSoFar >= aspBeta || bestScoreSoFar <= aspAlpha)
                {
                    if (aspAlpha == -INFINITY) break;
                    else if (aspAlpha == previousScore - WINDOW)
                    {
                        aspAlpha = previousScore - WINDOW * 2;
                        aspBeta = previousScore + WINDOW * 2;
                    }
                    else
                    {
                        aspAlpha = -INFINITY;
                        aspBeta = INFINITY;
                    }
                }
                else
                {
                    break aspiration;
                }
            }
            if (timeUp) break;
            
            if (bestMoveSoFar == Moves.NO_MOVE) return null;

            // depth finished: save results, best move first for next depth
            rootScores = "root scores (depth " + depth + "):\n" + depthScores;
            
            int[] pvStart = Moves.coordsStart(bestMoveSoFar);
            int[] pvEnd = Moves.coordsEnd(bestMoveSoFar);

            pv = Board.convertMove(pvStart) + Board.convertMove(pvEnd);
            if (board.getPiece(pvStart).getPiece() == 'P' && (pvEnd[0] == 0 || pvEnd[0] == 7)) pv += "q";
            

            Undo u = board.makeMove(pvStart, pvEnd, 'Q');
            pv += " " + getPV(board, depth - 1, !whiteTurn);
            board.unMakeMove(pvStart, pvEnd, u);
            if (uciMode) System.out.printf("info depth %d score cp %d nodes %d time %d pv %s%n", depth, bestScoreSoFar, movesAnalysed, (long)(System.currentTimeMillis() - startTime), pv);

            previousScore = bestScoreSoFar;

            int bestIndex = 0;
            while (moves[bestIndex] != bestMoveSoFar) bestIndex++;
            for (int i = bestIndex; i > 0; i--) moves[i] = moves[i - 1];
            moves[0] = bestMoveSoFar;
            
            timeCanBeUp = true;

            bestMove = bestMoveSoFar;
            bestScore = bestScoreSoFar;
            depthReached++;

            if (System.currentTimeMillis() - startTime >= softTimeLimit) break;
        }
        
        if (bestMove == Moves.NO_MOVE) return null;

        int[] result = Moves.coordsStartEnd(bestMove);

        if (Settings.highlightPreviousMove)
        {
            Game.previousMove = new boolean[8][8];
            Game.previousMove[result[0]][result[1]] = true;
            Game.previousMove[result[2]][result[3]] = true;
        }

        // results for the debug menu
        Game.botMoveTimeMs = (int)(System.currentTimeMillis() - startTime);
        Game.botMoveEval = bestScore;
        return result;
    }
}