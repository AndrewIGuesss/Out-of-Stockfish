import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    
    //   [ply]   [slot 0 newest / 1 older]   -> int[] move
    public static int[][][] killerMoves = new int[MAX_PLY][KILLER_SLOTS][];
    //   [from square 0-63]   [to square 0-63]   -> beta cutoff
    public static int[][] historyTable = new int[SQUARES][SQUARES];

    // repetition check
    public static long[] plyPathKeys = new long[MAX_PLY];
    
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
        List<int[]> pvMoves = new ArrayList<>();
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

            int[] ttMove = entry.bestmove();

            if (ttMove == null) break pv;

            int[] start = new int[]{ttMove[0], ttMove[1]};
            int[] end = new int[]{ttMove[2], ttMove[3]};
            
            List<int[]> moves = Moves.pseudoMoves(board, whiteTurn);
            boolean legal = false;

            
            for (int i = 0; i < moves.size(); i++)
            {
                if (Arrays.equals(ttMove, moves.get(i)))
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
            int[] move = pvMoves.get(i);
            int[] start = new int[]{move[0], move[1]};
            int[] end = new int[]{move[2], move[3]};

            board.unMakeMove(start, end, undos.get(i));
        }
        return pv.toString().trim();
    }

    public static boolean isCapture(Board board, int[] move)
    {
        int[] ep = board.getEnPassantSquare();
        if (board.getPiece(move[2], move[3]).isOccupied()) return true;
        if (board.getPiece(move[0], move[1]).getPiece() == 'P' && ep != null && ep[0] == move[2] && ep[1] == move[3]) return true;
        return false;
    }

    public static int orderScore (Board board, int[] move, int ply)
    {
        // MVV-LVA = most valuable victim, least valuable attacker
        int victimValue = 0;
        int attackerValue = board.getPiece(move[0], move[1]).value();
        if (board.getPiece(move[0], move[1]).getPiece() == 'K') attackerValue = 50;
        
        boolean isPromotion = (move[2] == 7 || move[2] == 0) && board.getPiece(move[0], move[1]).getPiece() == 'P';

        if (isCapture(board, move) || isPromotion) 
        {
            victimValue = board.getPiece(move[2], move[3]).value();
            int[] ep = board.getEnPassantSquare();
            if (board.getEnPassantSquare() != null && ep[0] == move[2] && ep[1] == move[3]) victimValue = 100;

            if (!isPromotion && board.getPiece(move[0], move[1]).value() > victimValue && Moves.see(board, move) < 0) return BAD_CAPTURE_BASE + victimValue * 10 - attackerValue;
            
            if (isPromotion && board.getPiece(move[0], move[1]).getPiece() == 'P') victimValue += 900;
            return CAPTURE_BASE + victimValue * 10 - attackerValue; 
        }

        if (ply == -1) return 0;

        if (Arrays.equals(killerMoves[ply][0], move)) return KILLER_1;
        else if (Arrays.equals(killerMoves[ply][1], move)) return KILLER_2;
        else return Math.min(historyTable[move[0] * 8 + move[1]][move[2] * 8 + move[3]], HISTORY_MAX);
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



    public static int quiescence(Board board, boolean whiteTurn, int alpha, int beta)
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

        if (!hasPromotingPawn(board, whiteTurn))
        {
            if (standPat + QUEEN_VALUE + DELTA_MARGIN <= alpha) return best;
        }
        
        // captures only (MVV-LVA ordered)
        List<int[]> moves = Moves.pseudoMoves(board, whiteTurn);
        List<int[]> captures = new ArrayList<>();
        int[] scores = new int[moves.size()];
        int captureCount = 0;

        for (int i = 0; i < moves.size(); i++)
        {
            int currOrderScore = orderScore(board, moves.get(i), -1);
            if (currOrderScore >= CAPTURE_BASE) 
            {
                captures.add(moves.get(i));
                scores[captureCount] = currOrderScore;
                captureCount++;
            }
        }
        
        // search captures
        for (int i = 0; i < captures.size(); i++)
        {
            int high = Integer.MIN_VALUE;
            int highIndex = i;
            for (int j = i; j < captures.size(); j++)
            {
                if (scores[j] > high)
                {
                    high = scores[j];
                    highIndex = j;
                }
            }
            scores[highIndex] = scores[i];
            scores[i] = high;
            Collections.swap(captures, highIndex, i);

            int[] capture = captures.get(i);
            
            int[] start = new int[]{capture[0], capture[1]};
            int[] end = new int[]{capture[2], capture[3]};
            
            int[] ep = board.getEnPassantSquare();
            boolean promoting = board.getPiece(capture[0], capture[1]).getPiece() == 'P' && (capture[2] == 7 || capture[2] == 0);

            int value = Arrays.equals(end, ep)? 100 : board.getPiece(capture[2], capture[3]).value();

            if (!promoting && standPat + value + DELTA_MARGIN <= alpha) continue;

            Undo u = board.makeMove(start, end, 'Q');
            if (Moves.inCheck(board, board.getKingLoc(whiteTurn), whiteTurn))
            {
                board.unMakeMove(start, end, u);
                continue;
            } 

            int score = -quiescence(board, !whiteTurn, -beta, -alpha);

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
        int[] bestMoveHere = null;

        // 50 move rule
        if (board.getTimeSinceCaptureOrPawnPush() >= 100) return 0;

        if (Board.isInsufficientMaterial(board)) return 0;
        
        // zobrist hash + repetition detection
        long currKey = Zobrist.makeZobristHash(board, whiteTurn);
        if (ply > 0)
        {
            if (Game.repetition.containsKey(currKey)) return 0;
            for (int i = 0; i <= ply - 1; i++)
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

        // transposition table lookup (check if we've seen this position before)
        int[] ttMove = null;
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

        // quiescence at the leaves (last search until we reach the max depth)
        if (maxDepth == 0) return quiescence(board, whiteTurn, alpha, beta);

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
        // startRow, startCol, endRow, endCol
        List<int[]> moves = Moves.pseudoMoves(board, whiteTurn);
        int[] scores = new int[moves.size()];
        
        int best = -MATE; // extremly low place holder value
        for (int i = 0; i < moves.size(); i++)
        {
            scores[i] = orderScore(board, moves.get(i), ply);
        }

        
        // try TT move first
        if (ttMove != null)
        {
            for (int i = 0; i < moves.size(); i++)
            {
                if (Arrays.equals(ttMove, moves.get(i)))
                {
                    scores[i] = TT_MOVE_SCORE; // give big value so it gets sorted to the top
                    break;
                }
            }
        }

        // main search loop
        int validMoves = 0;
        
        for (int i = 0; i < moves.size(); i++)
        {
            int high = Integer.MIN_VALUE;
            int highIndex = i;
            for (int j = i; j < moves.size(); j++)
            {
                if (scores[j] > high)
                {
                    high = scores[j];
                    highIndex = j;
                }
            }
            scores[highIndex] = scores[i];
            scores[i] = high;
            Collections.swap(moves, highIndex, i);
            

            int[] move = moves.get(i); 

            boolean promoting = board.getPiece(move[0], move[1]).getPiece() == 'P' && (move[2] == 7 || move[2] == 0);
            boolean capture = isCapture(board, move);

            int[] start = new int[]{move[0], move[1]};
            int[] end = new int[]{move[2], move[3]};

            Undo u = board.makeMove(start, end, 'Q');
            if (Moves.inCheck(board, board.getKingLoc(whiteTurn), whiteTurn))
            {
                board.unMakeMove(start, end, u);
                continue;
            }
            
            validMoves++;

            // can only be true for quiet moves
            boolean givesCheck = !inCheck && !capture && !promoting && validMoves > 1 && Moves.inCheck(board, board.getKingLoc(!whiteTurn), !whiteTurn);

            if (beta - alpha == 1 && !inCheck && maxDepth <= LMP_MAX_DEPTH && !capture && !promoting && Math.abs(alpha) < MATE - 1000)
            {
                if (validMoves > LMP_BASE + LMP_MULTIPLIER * maxDepth * maxDepth && !givesCheck)
                {
                    board.unMakeMove(start, end, u);
                    continue;
                }
            }

            // futility pruning
            if (beta - alpha == 1 && maxDepth <= FP_MAX_DEPTH && !inCheck && Math.abs(alpha) < MATE - 1000 && validMoves > 1)
            {
                if (!givesCheck && !promoting && !capture)
                {
                    if (staticEval + FP_MARGIN * maxDepth <= alpha) 
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
            
            if (validMoves > 4 && maxDepth >= 3 && !Arrays.equals(killerMoves[ply][0], move) && !inCheck && !capture)
            {
                if (!givesCheck && !promoting && reduction != 0)
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
        killerMoves = new int[MAX_PLY][KILLER_SLOTS][];
        historyTable = new int[SQUARES][SQUARES];

        // stats
        depthReached = 0;

        movesAnalysed = 0;
        quiescenceMovesAnalysed = 0;

        double startTime = System.currentTimeMillis();

        // root move generation + ordering
        // startRow, startCol, endRow, endCol
        List<int[]> moves = Moves.pseudoMoves(board, whiteTurn);

        // alpha - the best score we've already found
        // beta - the score where the opponent would never allow this position

        int bestScore = -INFINITY;
        int bestScoreSoFar = -INFINITY;
        int alpha = -INFINITY;
        int beta = INFINITY;
        int[] bestMove = null;
        int[] bestMoveSoFar = null;
        int previousScore = -INFINITY;

        moves.sort((a, b) -> Integer.compare(orderScore(board, b, -1), orderScore(board, a, -1)));
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
                bestMoveSoFar = null;
                depthScores = new StringBuilder();

                // root search loop
                roots: for(int[] move : moves)
                {
                    int[] start = new int[]{move[0], move[1]};
                    int[] end = new int[]{move[2], move[3]};
                    
                    Undo u = board.makeMove(start, end, 'Q');
                    if (Moves.inCheck(board, board.getKingLoc(whiteTurn), whiteTurn)) 
                    {
                        board.unMakeMove(start, end, u);
                        continue;
                    }
                    
                    if (bestMoveSoFar == null) bestMoveSoFar = move;

                    int score = -negamax(board, depth - 1, !whiteTurn, -beta, -alpha, 1, false);
                    board.unMakeMove(start, end, u);

                    if (timeUp) break;

                    // root scores display
                    depthScores.append(Board.convertMove(new int[]{move[0], move[1]}) + Board.convertMove(new int[]{move[2], move[3]}));
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
            
            if (bestMoveSoFar == null) return null;

            // depth finished: save results, best move first for next depth
            rootScores = "root scores (depth " + depth + "):\n" + depthScores;
            
            int[] pvStart = new int[]{bestMoveSoFar[0], bestMoveSoFar[1]};
            int[] pvEnd = new int[]{bestMoveSoFar[2], bestMoveSoFar[3]};

            pv = Board.convertMove(pvStart) + Board.convertMove(pvEnd);
            if (board.getPiece(pvStart).getPiece() == 'P' && (pvEnd[0] == 0 || pvEnd[0] == 7)) pv += "q";
            

            Undo u = board.makeMove(pvStart, pvEnd, 'Q');
            pv += " " + getPV(board, depth - 1, !whiteTurn);
            board.unMakeMove(pvStart, pvEnd, u);
            if (uciMode) System.out.printf("info depth %d score cp %d nodes %d time %d pv %s%n", depth, bestScoreSoFar, movesAnalysed, (long)(System.currentTimeMillis() - startTime), pv);

            previousScore = bestScoreSoFar;
            moves.remove(bestMoveSoFar);
            moves.add(0, bestMoveSoFar);
            
            timeCanBeUp = true;

            bestMove = bestMoveSoFar;
            bestScore = bestScoreSoFar;
            depthReached++;

            if (System.currentTimeMillis() - startTime >= softTimeLimit) break;
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