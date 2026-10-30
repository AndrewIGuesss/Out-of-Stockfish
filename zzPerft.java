// gets total legal moves for each depth of a position

import java.util.ArrayList;
import java.util.List;
public class zzPerft 
{
    public static List<Integer> runPerft () 
    {
        String[] testPositionFens = new String[]{
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"
        };

        int[] testPositionDepths = new int[]{4,3,5,4,3};
        int[] testPositionKnown = new int[]{197281, 97862, 674624, 320802, 54007}; // dosent include promotions besides queen
        List<Integer> failed = new ArrayList<>();
        System.out.println("\n\n---Perft Test---\n");
        System.out.println("passed  calculated  expected");
        System.out.println("-----------------------------");
        for (int i = 0; i < testPositionFens.length; i++)
        {
            Board testBoard = new Board();
            testBoard.importFEN(testPositionFens[i]);
            long totalMoves = perft(testBoard, testPositionDepths[i], testBoard.isWhiteTurn());
            if (totalMoves == testPositionKnown[i]) System.out.printf(zTests.PASS + " | %9d %9d%n", totalMoves, testPositionKnown[i]);
            else { System.out.printf(zTests.FAIL + " | %9d %9d%n", totalMoves, testPositionKnown[i]); failed.add(i); }
        }
        if (failed.size() > 0)
        {
            System.out.print("failed tests:");
            for (int test : failed)
            {
                System.out.print(" " + (test + 1));
            }
        }
        return failed;
    }

    public static long perft(Board board, int depth, boolean isWhite)
    {
        if (depth == 0) return 1;

        long totalMoves = 0;
        List<Integer> moves = Moves.allMoves(board, isWhite);
        for (int moveInt : moves)
        {
            int[] move = Moves.coordsStartEnd(moveInt); // {startRow, startCol, endRow, endCol}
            Board copy = new Board(board);
            copy.makeMove(new int[]{move[0], move[1]}, new int[]{move[2], move[3]}, 'Q');
            
            totalMoves += perft(copy, depth - 1, !isWhite);
        }
        return totalMoves;
    }
}
