import java.util.ArrayList;
import java.util.List;

public class zzMirror 
{
    private static String[] mirrorFens = {
        // start position (symmetric, sanity check: should be 0 both ways)
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        // kiwipete (busy middlegame, everything on the board)
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
        // white castled, black not (king shield, castling rights, king square)
        "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQ1RK1 b kq - 0 5",
        // queens gambit declined (pawn structure, developed vs home pieces)
        "rnbqkb1r/ppp2ppp/4pn2/3p2B1/2PP4/2N5/PP2PPPP/R2QKBNR b KQkq - 3 4",
        // opposite side castling (black castled long, white still has rights)
        "2kr3r/pppq1ppp/2np1n2/2b1p1B1/2B1P1b1/2NP1N2/PPPQ1PPP/R3K2R w KQ - 0 9",
        // early queen out (early queen penalty)
        "rnb1kbnr/pppp1ppp/8/4p3/4P2q/8/PPPP1PPP/RNBQKBNR w KQkq - 1 3",
        // bishop pair vs knights (bishop pair, bishop lines)
        "r3k2r/pp3ppp/2n1bn2/8/8/2B2B2/PP3PPP/R3K2R w KQkq - 0 15",
        // en passant square set (tests the ep rank flip in your mirror function)
        "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3",
        // doubled + isolated pawns
        "4k3/pp4pp/8/8/2P5/2P5/P5PP/4K3 w - - 0 30",
        // passed pawns for both sides (passed pawn bonus, endgame tables)
        "8/5k2/8/1P6/6p1/8/5K2/8 w - - 0 50",
        // rook endgame (rook files, endgame king)
        "8/8/3k4/2pP4/2P2R2/6r1/5K2/8 w - - 0 45",
        // pawns about to promote for both sides
        "8/P5k1/8/8/8/8/6Kp/8 w - - 0 1",
        // KR vs K, white winning (mop up)
        "8/8/8/4k3/8/8/8/R3K3 w - - 0 1",
        // KQ vs K, black winning (mop up from black's side)
        "8/8/2k5/8/8/8/q7/6K1 b - - 0 60",
        // a-file king, castled-long style, c-file shield pawn missing
        "r3k2r/ppp2ppp/8/8/8/8/PP3PPP/K6R w kq - 0 20",
        // h-file king with open g-file vs a-file king with full shield
        "k7/ppp5/8/8/8/8/5P1P/7K w - - 0 30",
        // a-file king with no a-pawn (would have hung the old version)
        "4k3/8/8/8/8/8/1PP5/K7 w - - 0 30"
    };
    public static List<Integer> runMirror()
    {
        System.out.println("\n- mirror eval test -\n");
        System.out.println("-----------------------------");

        List<Integer> failed = new ArrayList<>();
        for (int fenNum = 0; fenNum < mirrorFens.length; fenNum++)
        {
            String fen = mirrorFens[fenNum];
            String[] fenParts = fen.split(" ");
            String[] ranks = fenParts[0].split("/");
            StringBuilder mirroredBoard = new StringBuilder();

            for (int i = 7; i >= 0; i--)
            {
                for (char c : ranks[i].toCharArray())
                {
                    if (Character.isUpperCase(c)) mirroredBoard.append(Character.toLowerCase(c));
                    else if (Character.isLowerCase(c)) mirroredBoard.append(Character.toUpperCase(c));
                    else mirroredBoard.append(c);
                }
                if (i > 0) mirroredBoard.append("/");
            }
            mirroredBoard.append(" ");

            if (fenParts[1].equals("b")) mirroredBoard.append("w");
            else mirroredBoard.append("b");
            mirroredBoard.append(" ");

            if (fenParts[2].indexOf("-") == -1)
            {
                if (fenParts[2].indexOf("k") != -1) mirroredBoard.append("K");
                if (fenParts[2].indexOf("q") != -1) mirroredBoard.append("Q");
                if (fenParts[2].indexOf("K") != -1) mirroredBoard.append("k");
                if (fenParts[2].indexOf("Q") != -1) mirroredBoard.append("q");
            }
            else mirroredBoard.append("-");
            mirroredBoard.append(" ");

            if (fenParts[3].indexOf("-") == -1)
            {
                int preRow = Integer.parseInt(Character.toString(fenParts[3].charAt(1)));
                int postRow = Math.abs(9 - preRow);
                mirroredBoard.append(Character.toString(fenParts[3].charAt(0)) + postRow);
            }
            else mirroredBoard.append("-");
            mirroredBoard.append(" ");

            mirroredBoard.append(fenParts[4]);
            mirroredBoard.append(" ");

            mirroredBoard.append(fenParts[5]);
            mirroredBoard.append(" ");


            Board board = new Board();
            board.importFEN(fen);
            int preEval = Eval.evalBoard(board, false);

            Board mirrorBoard = new Board();
            mirrorBoard.importFEN(mirroredBoard.toString());
            int postEval = Eval.evalBoard(mirrorBoard, false);

            if (preEval != -postEval) 
            {
                failed.add(fenNum);
                System.out.printf("%s | eval:%6d vs %6d | fen:%s%n", zTests.FAIL, preEval, postEval, fen);
            }
            else
            {
                System.out.printf("%s | eval:%6d vs %6d | fen:%s%n", zTests.PASS, preEval, postEval, fen);
            }
        }
        return failed;
    }
}
