import java.io.IOException;
import java.util.List;

public class UI 
{
    // colors
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String RED_BG  = "\033[48;2;255;100;100m";
    private static final String LIGHT_MOVE_SQ = "\033[48;2;20;160;165m";
    private static final String DARK_MOVE_SQ  = "\033[48;2;20;75;160m";
    
    public static final String DEFAULT_WHITE = "\033[38;2;255;255;255m";
    public static final String DEFAULT_BLACK = "\033[38;2;0;0;0m";
    public static final String DEFAULT_LIGHT_SQ = "\033[48;2;188;188;188m";  
    public static final String DEFAULT_DARK_SQ  = "\033[48;2;88;88;88m";

    public static final String SAFE_WHITE = "\033[34m";
    public static final String SAFE_BLACK = "\033[35m";
    public static final String SAFE_LIGHT_SQ = "\033[47m";  
    public static final String SAFE_DARK_SQ  = "\033[40m";

    
    public static void welcome()
    {
        System.out.println("=========================");
        System.out.println("|        Welcome!       |");
        System.out.println("| to start type 'start' |");
        System.out.println("|                       |");
        System.out.println("|    type 'help' for    |");
        System.out.println("|   a list of commands  |");
        System.out.println("=========================");
    }
    
    public static void error()
    {
        System.out.println("=========================");
        System.out.println("|  error: invaild input |");
        System.out.println("|   please try again    |");
        System.out.println("|                       |");
        System.out.println("|    type 'help' for    |");
        System.out.println("|   a list of commands  |");
        System.out.println("=========================");
    }
    
    public static void preGameColor()
    {
        System.out.println("============================================");
        System.out.println("|                !WARNING!                 |");
        System.out.println("|   this was meant to be played with the   |");
        System.out.println("|     the terminal contrast ratio off      |");
        System.out.println("|                                          |");
        System.out.println("|       if you are using VS code set:      |");
        System.out.println("| terminal.integrated.minimumContrastRatio |");
        System.out.println("|                   to 1                   |");
        System.out.println("|                                          |");
        System.out.println("|    to toggle safe colors type 'color'    |");
        System.out.println("|       otherwise enter a blank line       |");
        System.out.println("============================================");
    }
    
    public static void preGameNotation()
    {
        System.out.println("========================================");
        System.out.println("|  Moves are noted in chess notation   |");
        System.out.println("|      ex. Nc3, Qxd5, exf3 etc.        |");
        System.out.println("|                                      |");
        System.out.println("| if you wish to use expanded notation |");
        System.out.println("|         ex. b1 > c3, e2 > e4         |");
        System.out.println("|                                      |");
        System.out.println("|     to toggle expanded notation      |");
        System.out.println("|             type 'expand'            |");
        System.out.println("|     otherwise enter a blank line     |");
        System.out.println("========================================");
    }
    
    public static void help()
    {
        System.out.println("list of all commands");
        System.out.println("---------------------------");
        System.out.println("help - list of commands");
        System.out.println("start - starts game");
        System.out.println("qs - starts & skips pre game menus");
        System.out.println("dqs - starts & skips pre game menus with debug enabled");
        System.out.println("debug - toggles the debug menu");
        System.out.println("bot - starts a game against a bot");
        System.out.println("bot game - starts a game of 2 bots");
        System.out.println();
        
        System.out.println(helpDebug());
    }
    
    public static String helpDebug()
    {
        return
        "list of all debug commands\n" +
        "---------------------------\n" +
        "history - views full game move history\n" +
        "info - views all stored information about a square\n" +
        "toggle - toggles the debug varibles from being shown\n" +
        "reset - resets the board and wipes board data\n" +
        "clear - clears all pieces but the kings off the board\n" +
        "add - adds a user difined piece to the board\n" +
        "cmds - shows all debug commands\n" +
        "save history - saves the game's history to History.txt\n" +
        "\n" +
        "list of all bot commands\n" +
        "---------------------------\n" +
        "bot - lets the bot play one move for current side\n" +
        "bot depth - changes the depth the bot searches at\n" +
        "bot toggle - makes the bot to play for a selected side\n" +
        "bot game - makes the bot play both sides\n" +
        "bot time limit - changes how long the bot can think per move (in ms)\n" + 
        "bot roots - shows the evaluation for all of the previous root moves";
    }
    

    public static String captureList(List<Piece> captures)
    {
        String list = "";
        char[] hiarchy = new char[]{'Q', 'R', 'N', 'B', 'P'};

        for (int i = 0; i < 5; i++)
        {
            for (Piece p : captures)
            {
                if (p.getPiece() == hiarchy[i]) list += hiarchy[i];
            }
        }
        
        return list;
    }


    public static void printBoard(Board board, boolean[][] validMoves, int[]checkSquare)
    {
        String BLACK = Settings.safeColors? SAFE_BLACK : DEFAULT_BLACK;
        String WHITE = Settings.safeColors? SAFE_WHITE : DEFAULT_WHITE;
        String DARK_SQ = Settings.safeColors? SAFE_DARK_SQ : DEFAULT_DARK_SQ;
        String LIGHT_SQ = Settings.safeColors? SAFE_LIGHT_SQ : DEFAULT_LIGHT_SQ;

        List<String> history = board.getHistory();
        // previous move
        if (!history.isEmpty()) 
        {
            if (!board.isWhiteTurn()) System.out.println("White Played: " + history.get(history.size() - 1));
            else System.out.println("Black Played: " + history.get(history.size() - 1));
            System.out.println();
        }
        else System.out.println("Good luck!\n");
        // black captures
        System.out.println("Black: " + (board.getBoardValue() < 0? "+" + Math.abs(board.getBoardValue()) : "") + " " + captureList(board.getCaptures(false)));
        
        
        int firstRow = 0;
        int lastRow = 8;
        int firstCol = 0;
        int lastCol = 8;
        char[] file = new char[]{'a','b','c','d','e','f','g','h'};
        if (Settings.blackOnBottom)
        {
            firstRow = 7;
            lastRow = -1;
            firstCol = 7;
            lastCol = -1;
            file = new char[]{'h','g','f','e','d','c','b','a'};
        }
        // files
        if (Settings.usePieceIcons) System.out.printf(RESET + "   %c    %c   %c   %c   %c   %c   %c   %c%n", file[0], file[1], file[2], file[3], file[4], file[5], file[6], file[7]);
        else System.out.printf(RESET + "   %c  %c  %c  %c  %c  %c  %c  %c%n", file[0], file[1], file[2], file[3], file[4], file[5], file[6], file[7]);
        for (int row = firstRow; row != lastRow; row += (Settings.blackOnBottom? -1 : 1))
        {
            // ranks
            System.out.print(RESET + (8 - row) + " ");
            for (int col = firstCol; col != lastCol; col += (Settings.blackOnBottom? -1 : 1))
            {
                String bg = (col + row) % 2 == 0? LIGHT_SQ : DARK_SQ;
                if (validMoves != null && validMoves[row][col]) bg = (col + row) % 2 == 0? LIGHT_MOVE_SQ : DARK_MOVE_SQ;
                if (checkSquare != null && checkSquare[0] == row && checkSquare[1] == col) bg = RED_BG;
    
                if (board.getBoard()[row][col].isOccupied())
                {
                    String color = board.getBoard()[row][col].isWhite()? WHITE : BLACK;
                    if (Settings.usePieceIcons) System.out.print(RESET + BOLD + color + bg + " " + board.getBoard()[row][col].toIcon() + "  ");
                    else System.out.print(RESET + BOLD + color + bg + " " + board.getBoard()[row][col].getPiece() + " ");
                }
                else
                {
                    if (Settings.usePieceIcons) System.out.print(RESET + bg + "    ");
                    else System.out.print(RESET + bg + "   ");
                }
            }
            System.out.println(RESET);
        }
        
        // white captures
        System.out.println("White: " + (board.getBoardValue() > 0? "+" + board.getBoardValue() : "") + " " + captureList(board.getCaptures(true)));
        
        System.out.println();

        // debug menu
        if (!Settings.debugMenu) return;
        if (Settings.showBotDebugMenu)
        {
            System.out.println("---- bot ----");
            if (Settings.showBotRootScores) System.out.println((Game.botPlayedLastMove)? "roots: " + Bot.rootScores : "roots: N/A");
            if (Settings.showBotTimeLimit) System.out.println("bot time limit: " + Settings.botSearchTimeLimit  + "ms (" + (Settings.botSearchTimeLimit / 1000.0) + "s)");
            if (Settings.showBotMoveTime) System.out.println((Game.botPlayedLastMove)? "bot took: " + Game.botMoveTimeMs + "ms (" + (Game.botMoveTimeMs / 1000.0) + "s)" : "bot took: N/A");
            if (Settings.showBotSelfEval) 
            {
                if (!Game.botPlayedLastMove) System.out.println("bot self eval: N/A");
                else
                {
                    int botSelfEval = Game.botMoveEval * (board.isWhiteTurn()? -1 : 1);   
                    if (Math.abs(botSelfEval) > Bot.MATE - 1000)
                    {
                        int movesTillMate = (Bot.MATE - Math.abs(botSelfEval) + 1) / 2;
                        System.out.println("bot self eval: " + (botSelfEval > 0? "M" : "-M") + movesTillMate);
                    }
                    else System.out.println("bot self eval: " + botSelfEval / 100.0);
                }
            }
            if (Settings.showBotDepthReached) System.out.println((Game.botPlayedLastMove)? "bot depth reached: " + Bot.depthReached : "bot depth reached: N/A");
            if (Settings.showBotMovesAnalysed) System.out.println((Game.botPlayedLastMove)? "Moves analysed: " + Bot.movesAnalysed + "|" + Bot.quiescenceMovesAnalysed + "|" + Bot.movesAnalysed / Math.max(1,Game.botMoveTimeMs) + " moves per ms" : "Moves analysed: N/A");
        }
        if (Settings.showStandardDebugMenu)
        {
            System.out.println("\n----debug----");
            
            if (Settings.showEval) System.out.println("eval: " + Eval.evalBoard(board, false) / 100.0); // divide by 100 to convert centipawns into eval
            if (Settings.showEvalBreakDown) { int eval = Eval.evalBoard(board, true); System.out.println(Eval.getEvalBreakDown(eval)); }
            if (Settings.showTurnIndicator) System.out.println("turn:" + history.size() + "|" + ((board.isWhiteTurn())? "WHITE" : "BLACK")); 
            if (Settings.showLegalMoves) System.out.println("total legal moves:" + Moves.allMoves(board, board.isWhiteTurn()).size());
            if (Settings.showGameLength) System.out.println("game length: move " + (history.size() + (board.isWhiteTurn()? 1 : 2)) / 2 + " | ply " + history.size());
            if (Settings.showWhiteHasValidMoves) System.out.println("white has moves:" + !Moves.hasNoLegalMoves(board, true));
            if (Settings.showBlackHasValidMoves) System.out.println("black has moves:" + !Moves.hasNoLegalMoves(board, false));
            if (Settings.showPieceLegalMoves) System.out.println((board.getLegalMoves() == null)? "legal moves:null": "legal moves:" + board.getLegalMoves().size() + ":" + board.getLegalMoves());
        }

        System.out.println();
    }




    // bot thinking
    public static volatile boolean stillThinking = false;

    public static Runnable thinkingAnimation = () -> {
        String[] thinkingAnimationFrames = {"\r\033[2Kbot thinking[|] ", "\r\033[2Kbot thinking[/] ", "\r\033[2Kbot thinking[-] ", "\r\033[2Kbot thinking[\\] "};
        int frameNumber = 0;
        
        while (stillThinking)
        {
            // tell bot to stop thinking when user inputs a new line
            try { if (System.in.available() > 0) Bot.stopThinking = true; } catch (IOException e) {}

            System.out.print(thinkingAnimationFrames[frameNumber % 4]);
            frameNumber++;
            try {Thread.sleep(200);} catch (Exception e) {}
        }
    };
}