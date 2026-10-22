public class zzBench 
{
    // set by runBench so the test summary can print them
    public static long lastTotalTimeMs = 0;
    public static long lastNps = 0;

    public static long runBench()
    {
        int maxDepth = 7; // 7 = quick 8 = more accurate

        
        String[] testPositions = new String[]{
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "r1bq1rk1/ppp2ppp/2np1n2/2b1p3/2B1P3/2PP1N2/PP3PPP/RNBQ1RK1 w - - 2 7",
            "r1bq1rk1/p1p2ppp/2p5/3p2B1/4n3/2Q2P2/PPP3PP/R3KB1R w KQ - 0 11",
            "2r3k1/5pp1/7p/p7/4P3/7P/5PP1/3R2K1 w - - 0 1"
        };
        String[] testPositionNames = new String[]{
            "Opening - starting position",
            "Early middlegame - Italian",
            "Sharp middlegame - briliant move/tactic",
            "Endgame - rook and 4 pawns each"
        };

        Settings.botSearchTimeLimit = 99999999;
        Bot.exactRootScores = false;
        long totalNodes = 0;
        long totalSearchNodes = 0; // every depth of every position, for NPS
        long totalTimeNanos = 0;

        System.out.println("\n\n---Bench test---");
        for (int i = 0; i < testPositions.length; i++)
        {
            String position = testPositions[i];
            System.out.printf("\ntesting: %s%n(%s)%n-----------------------------------------%n", position, testPositionNames[i]);

            Board testBoard = new Board();
            boolean whiteTurn = testBoard.importFEN(position);
            long positionNodes = 0;
            long positionTimeNanos = 0;
            for (int depth = 1; depth <= maxDepth; depth++)
            {
                long zobristHash = Zobrist.makeZobristHash(testBoard, whiteTurn);
                
                Game.repetition.clear();
                
                Game.repetition.merge(zobristHash, 1, Integer::sum);

                TTEntry.ttTable = new TTEntry[TTEntry.TT_SIZE];
                Bot.clearHistory();

                long startTime = System.nanoTime();
                int[] bestMove = Bot.bestMove(testBoard, depth, whiteTurn);
                long timeNanos = System.nanoTime() - startTime;
                long time = timeNanos / 1000000;
                
                String moveString = Board.convertMove(bestMove) + Board.convertMove(new int[]{bestMove[2], bestMove[3]});
                double eval = Game.botMoveEval / 100.0;
                int movesAnalysed = Bot.movesAnalysed;

                if (depth == maxDepth) totalNodes += movesAnalysed;
                positionNodes += movesAnalysed;
                positionTimeNanos += timeNanos;
                System.out.printf("D:%d M:%-6s E:%6.2f N:%10d T:%-5d NPS:%d%n", depth, moveString, eval, movesAnalysed, time, nps(movesAnalysed, timeNanos));
            }
            totalSearchNodes += positionNodes;
            totalTimeNanos += positionTimeNanos;
            System.out.printf("position time: %d ms   nodes: %d   NPS: %d%n", positionTimeNanos / 1000000, positionNodes, nps(positionNodes, positionTimeNanos));
        }
        System.out.println("\n-----------------------------------------");
        System.out.println("total nodes:" + totalNodes);
        lastTotalTimeMs = totalTimeNanos / 1000000;
        lastNps = nps(totalSearchNodes, totalTimeNanos);
        System.out.printf("total time: %d ms   NPS: %d%n", lastTotalTimeMs, lastNps);
        return totalNodes;
    }

    // nodes per second, nanos avoids divide by 0 on the fast depths
    private static long nps(long nodes, long timeNanos)
    {
        if (timeNanos <= 0) return 0;
        return nodes * 1000000000L / timeNanos;
    }
}