import java.util.List;
import java.util.Scanner;

public class UCI 
{
    public static boolean whiteTurn = true;
    public static Board uciBoard = null;    
    public static void main(String[] args) 
    {
        Bot.uciMode = true;
        Scanner reader = new Scanner(System.in);
        while (reader.hasNextLine())
        {
            String input = reader.nextLine();
            String[] tokens = input.split("\\s+");
            switch (tokens[0]) 
            {
                case "position":
                {
                    uciPosition(tokens);
                    break;
                }
                case "uci":
                {
                    System.out.println("id name Out of Stock(fish)\nid author AndrewIGuess\nuciok");
                    break;
                }
                case "isready":
                {
                    System.out.println("readyok");
                    break;
                }
                case "ucinewgame":
                {
                    TTEntry.clear();
                    Game.repetition.clear();
                    break;
                }
                case "quit":
                {
                    return;
                }
                case "go":
                {
                    uciGo(tokens);
                    break;
                }
                default:
                {
                    System.err.println("invalid input");
                    break;
                }
            }
        }
    }

    public static void uciPosition(String[] tokens)
    {
        if (tokens.length < 2) return;
        if (tokens[1].equals("startpos"))
        {
            uciBoard = new Board();
            whiteTurn = true;
        }
        else if (tokens[1].equals("fen"))
        {
            uciBoard = new Board();
            String fen = String.join(" ", tokens).strip();
            int start = fen.indexOf("fen ") + 4;
            int end = fen.indexOf(" moves");

            if (end == -1) fen = fen.substring(start, fen.length());
            else fen = fen.substring(start, end);

            whiteTurn = uciBoard.importFEN(fen);
        }
        else return;

        Game.repetition.clear();

        long zobristHash = Zobrist.makeZobristHash(uciBoard, whiteTurn);
        Game.repetition.merge(zobristHash, 1, Integer::sum);

        int movesIndex = List.of(tokens).indexOf("moves");

        if (movesIndex == -1) return;
        for (int i = movesIndex + 1; i < tokens.length; i++)
        {
            char promotionPiece = 'Q';
            int[] start = Board.convertMove(tokens[i].substring(0, 2));
            int[] end = Board.convertMove(tokens[i].substring(2, 4));
            if (tokens[i].length() == 5) promotionPiece = tokens[i].charAt(4);

            uciBoard.applyMove(start, end, true, promotionPiece);
            whiteTurn = !whiteTurn;

            zobristHash = Zobrist.makeZobristHash(uciBoard, whiteTurn);
            Game.repetition.merge(zobristHash, 1, Integer::sum);
        }
    }

    public static void uciGo(String[] tokens)
    {
        int wtime = 0;
        int btime = 0;
        int winc = 0;
        int binc = 0;
        int movetime = 0;
        int depth = 0;

        for (int i = 0; i < tokens.length; i++)
        {
            switch (tokens[i]) 
            {
                case "wtime":
                {
                    wtime = Integer.parseInt(tokens[i + 1]);
                    break;
                }
                case "btime":
                {
                    btime = Integer.parseInt(tokens[i + 1]);
                    break;
                }
                case "winc":
                {
                    winc = Integer.parseInt(tokens[i + 1]);
                    break;
                }
                case "binc":
                {
                    binc = Integer.parseInt(tokens[i + 1]);
                    break;
                }
                case "movetime":
                {
                    movetime = Integer.parseInt(tokens[i + 1]);
                    break;
                }
                case "depth":
                {
                    depth = Integer.parseInt(tokens[i + 1]);
                    break;
                }
            }
        }
        long soft = 0;
        long hard = 0;

        if (movetime + wtime + btime == 0) { soft = Integer.MAX_VALUE; hard = Integer.MAX_VALUE; }
        else if (movetime == 0)
        {
            int remaining = whiteTurn? wtime : btime;

            soft = (long)((remaining / 30) + (0.75 * (whiteTurn? winc : binc)) - 50);
            soft = Math.min(soft, remaining - 50);
            soft = Math.max(10, soft);

            hard = Math.min(soft * 3, remaining / 4);
            hard = Math.max(soft, hard);
            hard = Math.max(10, hard);
        }
        else { soft = movetime; hard = movetime; }

        Settings.botSearchTimeLimit = hard;
        Bot.softTimeLimit = soft;

        if (depth == 0) depth = 30;
        int[] bestMove = Bot.bestMove(uciBoard, depth, whiteTurn);

        if (bestMove == null) { System.out.println("bestmove 0000"); return; }

        StringBuilder moveSB = new StringBuilder(); 
        moveSB.append(Board.convertMove(new int[]{bestMove[0], bestMove[1]})); 
        moveSB.append(Board.convertMove(new int[]{bestMove[2], bestMove[3]}));

        if (uciBoard.getPiece(bestMove[0], bestMove[1]).getPiece() == 'P' && bestMove[2] == (whiteTurn? 0 : 7)) moveSB.append("q");

        System.out.println("bestmove " + moveSB.toString());
    }
}
