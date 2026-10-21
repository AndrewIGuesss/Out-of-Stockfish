import java.util.ArrayList;
import java.util.List;

public class zzSee 
{
    // fen, move (uci), expected see in centipawns
    private static String[][] seeTests =
    {
        {"1k1r4/1pp4p/p7/4p3/8/P5P1/1PP4P/2K1R3 w - - 0 1", "e1e5", "100"},              // rook takes undefended pawn
        {"1k1r3q/1ppn3p/p4b2/4p3/8/P2N2P1/1PP1R1BP/2K1Q3 w - - 0 1", "d3e5", "-200"},    // knight takes pawn defended by knight
        {"4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1", "e4d5", "100"},                            // pawn takes undefended pawn
        {"4k3/8/2p5/3p4/4P3/8/8/4K3 w - - 0 1", "e4d5", "0"},                            // pawn takes pawn defended by pawn
        {"4k3/8/2n5/3p4/4Q3/8/8/4K3 w - - 0 1", "e4d5", "100"},                          // knight on c6 does not defend d5
        {"3rk3/3r4/8/3p4/8/8/3R4/3RK3 w - - 0 1", "d2d5", "-400"},                       // rook battery vs rook battery (x-ray)
        {"3rk3/3r4/3q4/3p4/8/8/3R4/3RK3 w - - 0 1", "d2d5", "0"},                        // queen in front of the rooks
        {"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6", "100"},                            // en passant, undefended
        {"4k3/2p5/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6", "0"}                             // en passant, defended by pawn
    };

    public static List<Integer> runSee()
    {
        System.out.println("\n- static exchange evaluation test -\n");
        System.out.println("-----------------------------");

        List<Integer> failed = new ArrayList<>();
        for (int testNum = 0; testNum < seeTests.length; testNum++)
        {
            String fen = seeTests[testNum][0];
            String uciMove = seeTests[testNum][1];
            int expected = Integer.parseInt(seeTests[testNum][2]);

            Board board = new Board();
            boolean whiteTurn = board.importFEN(fen);
            String beforeFen = board.makeFEN(whiteTurn);

            int[] start = Board.convertMove(uciMove.substring(0, 2));
            int[] end = Board.convertMove(uciMove.substring(2, 4));
            int result = Moves.see(board, new int[]{start[0], start[1], end[0], end[1]});

            boolean boardSame = beforeFen.equals(board.makeFEN(whiteTurn));

            if (result != expected || !boardSame)
            {
                failed.add(testNum);
                System.out.printf("%s | fen:%s | move:%s | see %d vs expected %d%s%n", zTests.FAIL, fen, uciMove, result, expected, boardSame? "" : " | BOARD CHANGED");
            }
            else
            {
                System.out.printf("%s | fen:%s | move:%s | see %d%n", zTests.PASS, fen, uciMove, result);
            }
        }
        return failed;
    }
}
