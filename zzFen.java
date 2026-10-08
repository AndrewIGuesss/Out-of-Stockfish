// imports a FEN and the makes a FEN to check if they are the same
// imports a FEN and the makes some moves then makes a new FEN to check if it matches the correct one

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class zzFen 
{
    public static String[] runFen() 
    {
        String[] roundTripFens = new String[]{
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
            "r3k2r/8/8/8/8/8/8/R3K2R w Kq - 0 1",
            "8/5k2/8/3K4/8/8/8/8 b - - 37 64",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"
        };

        String[] startFens = new String[]{
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        };

        String[][] testMoves = new String[][]{
            {"e2e4"},
            {"e2e4", "e7e5", "g1f3"},
            {"g1f3", "g8f6", "f3g1", "f6g8"},
            {"e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6", "e1g1"},
            {"h2h4", "a7a5", "h1h3"},
            {"a1a8"},
            {"e7e5"},
            {"e7e5", "g1f3"}
        };

        String[] expectedFens = new String[]{
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 4 3",
            "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQ1RK1 b kq - 5 4",
            "rnbqkbnr/1ppppppp/8/p7/7P/7R/PPPPPPP1/RNBQKBN1 b Qkq - 1 2",
            "R3k2r/8/8/8/8/8/8/4K2R b Kk - 0 1",
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2"
        };

        System.out.println("\n\n---Fen test---");
        System.out.println("\n-round trip test-\n");
        List<Integer> failedRT = new ArrayList<>();
        List<Integer> failedPM = new ArrayList<>();
        System.out.println("-----------------------------");
        for (int i = 0; i < roundTripFens.length; i++)
        {
            String fen = roundTripFens[i];
            String roundTrip = roundTrip(fen);
            if (roundTrip.equals(fen)) System.out.println(zTests.PASS);
            else { System.out.printf(zTests.FAIL + " | %s%n       %s%n", roundTrip, fen); failedRT.add(i); }
        }

        System.out.println("\n-play moves test-\n");
        System.out.println("-----------------------------");
        for (int i = 0; i < startFens.length; i++)
        {
            String fen = startFens[i];
            String playedMove = playMoves(fen, testMoves[i]);
            if (playedMove.equals(expectedFens[i])) System.out.println(zTests.PASS);
            else { System.out.printf(zTests.FAIL + " | %s%n       %s%n", playedMove, expectedFens[i]); failedPM.add(i); }
        }
        System.out.println();
        if (failedRT.size() + failedPM.size() > 0) System.out.println(zTests.FAILED + " A TEST");
        else System.out.println(zTests.PASSED + " ALL TESTS");
        if (failedRT.size() > 0)
        {
            System.out.print("failed round trip tests:");
            for (int test : failedRT)
            {
                System.out.print(" " + test);
            }
            System.out.println();
        }
        if (failedPM.size() > 0)
        {
            System.out.print("failed play moves tests:");
            for (int test : failedPM)
            {
                System.out.print(" " + test);
            }
        }
        return new String[]
        {Arrays.toString(failedRT.toArray()).replace("[", "").replace("]", ""), 
         Arrays.toString(failedPM.toArray()).replace("[", "").replace("]", "")};
    }

    public static String roundTrip(String fen)
    {
        Board testBoard = new Board(Settings.safeColors, Settings.bigNotation);
        boolean whiteTurn = testBoard.importFEN(fen);
        return testBoard.makeFEN(whiteTurn);
    }
    public static String playMoves(String fen, String[] moves)
    {
        Board testBoard = new Board(Settings.safeColors, Settings.bigNotation);
        boolean whiteTurn = testBoard.importFEN(fen);
        for (String move : moves)
        {
            testBoard.applyMove(Game.getSquare(move.substring(0, 2)), Game.getSquare(move.substring(2, 4)), false, 'Q');
            whiteTurn = !whiteTurn;
        }
        return testBoard.makeFEN(whiteTurn);
    }
}
