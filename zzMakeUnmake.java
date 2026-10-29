import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class zzMakeUnmake 
{
    private static String[] testFens =
    {
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",                // startpos
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",    // kiwipete
        "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",                                    // white castling + corner rook captures
        "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1",                                    // black castling + corner rook captures
        "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3",           // white en passant
        "rnbqkbnr/ppp1pppp/8/8/2PpP3/8/PP1P1PPP/RNBQKBNR b KQkq c3 0 3",           // black en passant
        "r3k3/1P6/8/8/8/8/8/4K3 w - - 0 1",                                        // white promotion + capture promotion
        "4k3/8/8/8/8/8/1p6/R3K3 b - - 0 1",                                        // black promotion + capture promotion
        "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",               // CPW perft pos 5
        "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"                                // CPW perft pos 3
    };

    public static List<Integer> runMakeUnmake()
    {
        System.out.println("\n- make/unmake round trip test -\n");
        System.out.println("-----------------------------");

        List<Integer> failed = new ArrayList<>();
        for (int fenNum = 0; fenNum < testFens.length; fenNum++)
        {
            String fen = testFens[fenNum];
            Board board = new Board();
            boolean whiteTurn = board.importFEN(fen);

            String beforeFen = board.makeFEN(whiteTurn);
            int[] beforeWhiteKing = board.getWhiteKingLoc().clone();
            int[] beforeBlackKing = board.getBlackKingLoc().clone();
            boolean beforeWhiteCastled = board.whiteCastled;
            boolean beforeBlackCastled = board.blackCastled;

            List<Integer> moves = Moves.pseudoMoves(board, whiteTurn);
            int badMoves = 0;
            for (int moveInt : moves)
            {
                int[] move = Moves.coordsStartEnd(moveInt); // {startRow, startCol, endRow, endCol}
                int[] start = new int[]{move[0], move[1]};
                int[] end = new int[]{move[2], move[3]};

                Undo undo = board.makeMove(start, end, 'Q');
                board.unMakeMove(start, end, undo);

                StringBuilder problem = new StringBuilder();
                String afterFen = board.makeFEN(whiteTurn);
                if (!afterFen.equals(beforeFen)) problem.append(" fen:" + afterFen);
                if (!Arrays.equals(board.getWhiteKingLoc(), beforeWhiteKing)) problem.append(" whiteKingLoc");
                if (!Arrays.equals(board.getBlackKingLoc(), beforeBlackKing)) problem.append(" blackingLoc");
                if (board.whiteCastled != beforeWhiteCastled) problem.append(" whiteCastled");
                if (board.blackCastled != beforeBlackCastled) problem.append(" blackCastled");

                if (!problem.isEmpty())
                {
                    badMoves++;
                    // problems go after the fen bc they can contain a fen themselves
                    System.out.printf("%s | move:%-4s | fen:%s |%s%n", zTests.FAIL, Board.convertMove(start) + Board.convertMove(end), fen, problem.toString());
                    board.importFEN(fen); // reset so one broken move doesnt cascade into the rest
                }
            }

            if (badMoves > 0) failed.add(fenNum);
            else System.out.printf("%s | %3d moves | fen:%s%n", zTests.PASS, moves.size(), fen);
        }
        return failed;
    }
}