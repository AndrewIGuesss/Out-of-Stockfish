import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Bot 
{
    public static volatile boolean stopThinking = false;

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

    private static final int NULL_MOVE_REDUCTION = 2;
    private static final int LMR_REDUCTION = 1;

    private static final int TEMPO_BONUS = 15;
    private static final int DELTA_MARGIN = 200; // delta pruning
    private static final int WINDOW = 35;
    public static String rootScores = "";
    public static boolean exactRootScores = false; // true = exact root score (slower)

    public static int movesAnalysed = 0;
    public static int quiescenceMovesAnalysed = 0;
    public static int depthReached = 0;

    //   [ply]   [slot 0 newest / 1 older]   -> int[] move
    public static int[][][] killerMoves = new int[MAX_PLY][KILLER_SLOTS][];
    //   [from square 0-63]   [to square 0-63]   -> beta cutoff
    public static int[][] historyTable = new int[SQUARES][SQUARES];

    // repetition check
    public static long[] plyPathKeys = new long[MAX_PLY];

    static long deadline = System.currentTimeMillis() + Settings.botSearchTimeLimit;
    static boolean timeUp = false;
    static boolean timeCanBeUp = false;
    
    public static int orderScore (Board board, int[] move, int ply)
    {
        // MVV-LVA = most valuable victim, least valuable attacker
        int victimValue;
        int attackerValue = board.getPiece(move[0], move[1]).value();
        if (board.getPiece(move[0], move[1]).getPiece() == 'K') attackerValue = 50;

        if (board.getPiece(move[2], move[3]).isOccupied()) 
        {
            victimValue = board.getPiece(move[2], move[3]).value();
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

    public static int quiescence(Board board, boolean whiteTurn, int alpha, int beta)
    {
        // time limit check
        if (System.currentTimeMillis() > deadline && timeCanBeUp)
        { 
            timeUp = true;
            return 0;
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
            if (standPat + Piece.B_QUEEN.value() + DELTA_MARGIN <= alpha) return best;
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
        // captures.sort((a, b) -> Integer.compare(orderScore(board, b, -1), orderScore(board, a, -1)));
        
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

            boolean promoting = board.getPiece(capture[0], capture[1]).getPiece() == 'P' && (capture[2] == 7 || capture[2] == 0);
            if (!promoting && standPat + board.getPiece(capture[2], capture[3]).value() + DELTA_MARGIN <= alpha) continue;

            Board copy = new Board(board);
            copy.applyMove(new int[]{capture[0], capture[1]}, new int[]{capture[2], capture[3]}, true, 'Q');
            if (Moves.inCheck(copy, copy.getKingLoc(whiteTurn), whiteTurn)) continue;

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
        if ((System.currentTimeMillis() > deadline && timeCanBeUp) || stopThinking && timeCanBeUp)
        { 
            timeUp = true;
            return 0;
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

        // null move pruning
        if (ply != 0 && maxDepth >= 4 && !isNullMove && !inCheck)
        {
            if (board.hasNonPawnMaterial(whiteTurn))
            {
                Board nullCopy = new Board(board);
                nullCopy.setEnPassantSquare(null);
                int score = -negamax(nullCopy, maxDepth - 1 - NULL_MOVE_REDUCTION, !whiteTurn, -beta, -beta + 1, ply + 1, true);
                
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

        // moves.sort((a, b) -> Integer.compare(orderScore(board, b, ply), orderScore(board, a, ply)));
        
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
            Board copy = new Board(board);
            copy.applyMove(new int[]{move[0], move[1]}, new int[]{move[2], move[3]}, true, 'Q');
            if (Moves.inCheck(copy, copy.getKingLoc(whiteTurn), whiteTurn)) continue;
            
            validMoves++;
            
            // LMR (late move reductions)
            boolean searched = false;
            int score = 0;
            boolean promoting = board.getPiece(move[0], move[1]).getPiece() == 'P' && (move[2] == 7 || move[2] == 0);
            if (validMoves > 4 && maxDepth >= 3 && !Arrays.equals(killerMoves[ply][0], move) && !inCheck && !board.getPiece(move[2], move[3]).isOccupied() )
            {
                if (!Moves.inCheck(copy, copy.getKingLoc(!whiteTurn), !whiteTurn) && !promoting)
                {
                    searched = true;
                    // reduced depth, null window
                    score = -negamax(copy, maxDepth - 1 - LMR_REDUCTION, !whiteTurn, -alpha - 1, -alpha, ply + 1, false);
                    if (score > alpha)
                    {
                        // full depth, null window
                        score = -negamax(copy, maxDepth - 1, !whiteTurn, -alpha - 1, -alpha, ply + 1, false);
                        if (score > alpha && score < beta)
                        {
                            // full depth, full window
                            score = -negamax(copy, maxDepth - 1, !whiteTurn, -beta, -alpha, ply + 1, false);
                        }
                    }
                }
            }

            // PVS (principal variation search)
            if (!searched)
            {
                if (validMoves == 1)
                {
                    score = -negamax(copy, maxDepth - 1, !whiteTurn, -beta, -alpha, ply + 1, false);
                }
                else
                {
                    // full depth, null window
                    score = -negamax(copy, maxDepth - 1, !whiteTurn, -alpha - 1, -alpha, ply + 1, false);
                    if (score > alpha && score < beta)
                    {
                        // full depth, full window
                        score = -negamax(copy, maxDepth - 1, !whiteTurn, -beta, -alpha, ply + 1, false);
                    }
                }
            }

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
                    Board copy = new Board(board);
                    copy.applyMove(new int[]{move[0], move[1]}, new int[]{move[2], move[3]}, true, 'Q');
                    if (Moves.inCheck(copy, copy.getKingLoc(whiteTurn), whiteTurn)) continue;
                    
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
            
            // depth finished: save results, best move first for next depth
            rootScores = "root scores (depth " + depth + "):\n" + depthScores;
            previousScore = bestScoreSoFar;
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