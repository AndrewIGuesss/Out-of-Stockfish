/*
    --- TO DO ---
    TASK                          TIME        ELO          TYPE
    King safety upgrade           2-3 hr      +50-100      eval
    UCI                           3-4 hr      n/a          testing
    Make/unmake + inc. Zobrist    6-10 hr     +50-80       speed
    Show PV                       45 min      n/a          QOL/debug
    Small eval terms              1 hr        +20-40       eval
    Futility + reverse futility   1 hr        +20-40       speed
    SEE                           2 hr        +15-30       speed
    Time management               1 hr        +10-30       strength
    Small opening book            1 hr        +10-30       strength
    Undo / takeback               1-2 hr      n/a          QOL
    Lazy SMP                      4-6 hr      +40-80       speed
    Underpromotions               30 min      n/a          rules
    Analysis mode                 30 min      n/a          QOL
    Hint command                  10 min      n/a          QOL
    PGN import                    1-2 hr      n/a          QOL
    Bot stats in PGN setting      30 min      n/a          QOL
    Perft command                 15 min      n/a          QOL
    Bench command                 20 min      n/a          QOL

    small eval terms = bishop pair, rooks on open files, mobility
*/

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class Game
{
    public static String gameResult = "*";
    public static long gameStartTime;
    public static String importedFEN = "";

    public static boolean[][] previousMove = new boolean[8][8];

    // debugMenu
    public static int botMoveTimeMs = -1;
    public static int botMoveEval = 123123123;
    public static boolean botPlayedLastMove = false;
    public static String debugMsg = "";

    public static Map<Long, Integer> repetition = new HashMap<>();
    private static Board board;
    private static boolean whiteTurn = true;
    private static boolean inCheck = false;
    private static boolean setUp = false;
    
    private static boolean whiteBotToggle = false;
    private static boolean blackBotToggle = false;
    public static void main(String[] args)
    {
        Scanner reader = new Scanner(System.in);
        
        boolean error = false;

        if(!Settings.useCustomStartOptions)
        {
            Settings.safeColors = false;
            Settings.bigNotation = false;
            Settings.quickStart = false;
            Settings.enableDebug = false;
        }

        UI.welcome();
        startUpScreenLoop: while (true)
        {
            if (error) { clear(); UI.error(); error = false; }
            
            String input = reader.nextLine().toLowerCase();
            switch (input) 
            {
                case "start": break startUpScreenLoop;
                case "help": UI.help(); break;
                case "qs": Settings.quickStart = true; break startUpScreenLoop; 
                case "dqs": Settings.quickStart = true; Settings.enableDebug = true; break startUpScreenLoop;
                case "debug": Settings.enableDebug = !Settings.enableDebug; break;
                case "bot":
                {
                    System.out.println("would you like to play as white or black");
                    input = reader.nextLine().toLowerCase();
                    if (input.equals("white") || input.equals("w")) blackBotToggle = true;
                    else if (input.equals("black") || input.equals("b")) whiteBotToggle = true;
                    else {error = true; continue;}
                    break startUpScreenLoop;
                }
                case "bot game":
                {
                    blackBotToggle = true;
                    whiteBotToggle = true;
                    break startUpScreenLoop;
                }
                default:
                {
                    error = true;
                    break;
                }
            }
        }
        
        // pre game color screen
        while (true && !Settings.quickStart)
        {
            if (error) { System.out.println("Invalid input"); error = false; }
            else clear();
            System.out.println("Console safe colors[" + (Settings.safeColors? "ON" : "OFF") + "]");
            UI.preGameColor();
            
            String input = reader.nextLine().toLowerCase();
            if (input.equals("color")) Settings.safeColors =! Settings.safeColors;
            else if (input.equals("")) break;
            else error = true;
        }
        
        // pre game notation
        while (true && !Settings.quickStart)
        {
            if (error) { System.out.println("Invalid input"); error = false; }
            else clear();
            System.out.println("Expanded notation[" + (Settings.bigNotation? "ON" : "OFF") + "]");
            UI.preGameNotation();
            
            String input = reader.nextLine().toLowerCase();
            if (input.equals("expand")) Settings.bigNotation =! Settings.bigNotation;
            else if (input.equals("")) break;
            else error = true;
        }
        
        board = new Board();
        long zobristHash = Zobrist.makeZobristHash(board, whiteTurn);
        repetition.merge(zobristHash, 1, Integer::sum);








        gameStartTime = System.currentTimeMillis();

        gameLoop: while (true)
        {
            if (!Settings.highlightPreviousMove) previousMove = null;

            // clear console if no error
            if (error) error = false;
            else clear();

            if (!debugMsg.equals("")) { System.out.println(debugMsg); debugMsg = ""; }

            String endMessage = gameOverCheck();
            if (endMessage != null)
            {
                if (inCheck) UI.printBoard(board, previousMove, board.getKingLoc(whiteTurn));
                else UI.printBoard(board, previousMove, null);
                System.out.print(endMessage);
                makePGN(board, whiteBotToggle, blackBotToggle);
                break gameLoop;
            }
            else
            {
                if (inCheck)
                {
                    UI.printBoard(board, previousMove, board.getKingLoc(whiteTurn));
                    System.out.print((whiteTurn? "White" : "Black") + " is in check!");
                }
                else
                {
                    UI.printBoard(board, previousMove, null);
                    System.out.print(whiteTurn? "White's turn" : "Black's turn");
                }
                System.out.print(" chose your piece: ");
            }
            
            // bot moves
            if ((whiteBotToggle && whiteTurn || blackBotToggle && !whiteTurn) && !Bot.stopThinking)
            {
                botPlayedLastMove = true;

                if (!playEngineMove(board, whiteTurn)) {debugMsg = "bot error"; continue;}
                System.out.println();

                afterAMove("move", board);
                
                continue;
            }
            
            String rawInput = reader.nextLine();
            String input = rawInput.toLowerCase();
            
            if (Bot.stopThinking && input.equals("bot"))
            {
                Bot.stopThinking = false;
                botPlayedLastMove = true;

                if (!playEngineMove(board, whiteTurn)) {debugMsg = "bot error"; continue;}
                System.out.println();
                
                afterAMove("move", board);
                continue;
            }

            // debug cmds
            if (Settings.enableDebug)
            {
                switch (input)
                {
                    case "ff":
                    {
                        clear();
                        UI.printBoard(board, previousMove, board.getKingLoc(whiteTurn));
                        
                        System.out.println((whiteTurn? "Black" : "White") + " has won by resignation");
                        gameResult = whiteTurn? "0-1" : "1-0";
                        makePGN(board, whiteBotToggle, blackBotToggle);
                        break gameLoop;
                    }
                    case "flip":
                    {
                        Settings.blackOnBottom = !Settings.blackOnBottom;
                        debugMsg = (Settings.blackOnBottom? "Black" : "White") + "'s pieces are now on the bottom";
                        break;
                    }
                    case "icons":
                    {
                        Settings.usePieceIcons = !Settings.usePieceIcons;
                        debugMsg = "showing pieces as " + (Settings.usePieceIcons? "icons" : "characters");
                        break;
                    }
                    case "history":
                    {
                        debugMsg = "history:|";
                        int offset = board.getBlackStarted()? 1 : 0;
                        for (int i = 0; i < board.getHistory().size(); i++)
                        {
                            int moveNumber = (i + offset) / 2 + 1;
                            if ((i + offset) % 2 == 0) debugMsg += moveNumber + ". " + board.getHistory().get(i);
                            else if (i == 0) debugMsg += moveNumber + "... " + board.getHistory().get(i) + "|";
                            else debugMsg += "," + board.getHistory().get(i) + "|";
                        }
                        // close off the last pair if white moved last
                        if (board.getHistory().size() > 0 && (board.getHistory().size() + offset) % 2 == 1) debugMsg += "|";
                        break;
                    }
                    case "break down":
                    {
                        Settings.showEvalBreakDown = !Settings.showEvalBreakDown;
                        debugMsg = Settings.showEvalBreakDown? "showing eval breakdown" : "hiding eval breakdown";
                        break;
                    }
                    case "info":
                    {
                        System.out.println("input square: ");
                        int[] piece = getSquare(reader.nextLine());
                        
                        
                        if (piece == null) debugMsg = "invaild square location";
                        else if (!board.getPiece(piece).isOccupied()) debugMsg = "square is empty";
                        else
                        {
                            Piece p = board.getPiece(piece); 
                            board.validMoves(piece, false);
                            debugMsg += 
                            "Square: " + Board.convertMove(piece) + " [" + piece[0] + ", " + piece[1] + "]" +
                            "\nPiece: " + p.getPiece() +
                            "\nColor: " + (p.isWhite()? "white" : "black") +
                            "\nAll moves:" + board.getFakeMoves().size() + " " + board.getFakeMoves() +
                            "\nLegal moves:" + board.getLegalMoves().size() + " " + board.getLegalMoves();
                        }
                        break;
                    }
                    case "add":
                    {
                        boolean valid = true;
                        
                        System.out.println("type piece location: ");
                        int[] customLoc = getSquare(reader.nextLine()); // null if bad square
                        if (customLoc == null) valid = false;
                        
                        if (valid)
                        {
                            System.out.println("input one charictar piece type\n(enter a blank line to make an empty square): ");
                            input = reader.nextLine();
                            
                            if (input.equals("")) 
                            {
                                board.addPiece(customLoc);
                                afterAMove("add", board);
                            }
                            else // piece path
                            {
                                char customPiece = input.toUpperCase().charAt(0);
                                if (customPiece != 'P' && customPiece != 'N' && customPiece != 'B' && customPiece != 'R' && customPiece != 'Q' && customPiece != 'K') valid = false;
                                
                                boolean customColor = false;
                                if (valid) 
                                {
                                    System.out.println("type piece color: ");
                                    input = reader.nextLine().toLowerCase();
                                    if (input.equals("white") || input.equals("w")) customColor = true;
                                    else if (input.equals("black") || input.equals("b")) customColor = false;
                                    else valid = false;
                                }
                                
                                if (valid)
                                {
                                    board.addPiece(customPiece, customLoc, customColor);
                                    afterAMove("add", board);
                                }
                            }
                        }
                        
                        debugMsg = valid? "added piece" : "could not add piece";
                        break;
                    }
                    case "toggle":
                    {
                        Settings.debugMenu = !Settings.debugMenu;
                        debugMsg = "Debug menu[" + (Settings.debugMenu? "ON" : "OFF") + "]";
                        break;
                    }
                    case "cmds":
                    { 
                        debugMsg = UI.helpDebug(); 
                        break;
                    }
                    case "reset":
                    {
                        debugMsg = "board reset";
                        board = new Board();
                        whiteTurn = true;

                        afterAMove("reset", board);
                        
                        continue;
                    }
                    case "clear":
                    {
                        debugMsg = "board cleared";
                        for (int i = 0; i < 8; i++)
                        {
                            for (int j = 0; j < 8; j++)
                            {
                                if (board.getPiece(i, j).getPiece() != 'K')board.addPiece(new int[]{i, j});
                            }
                        }

                        afterAMove("clear", board);
                        
                        continue;
                    }
                    case "fen":
                    {
                        System.out.println("please input your fen");
                        input = reader.nextLine();
                        try
                        {
                            Board newBoard = new Board();
                            whiteTurn = newBoard.importFEN(input);

                            afterAMove("fen", newBoard);

                            importedFEN = input;
                            debugMsg = "FEN imported";
                        }
                        catch (Exception e) { debugMsg = "error importing FEN"; }
                        break;
                    }
                    case "make fen":
                    {
                        try
                        {
                            String fen = board.makeFEN(whiteTurn);
                            debugMsg = "FEN created\n" + fen;
                            if (Settings.addSavedFenInHistory)
                            {
                                Path filePath = Paths.get("History.txt");
                                Files.writeString(filePath, "saved FEN: " + fen, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                Files.writeString(filePath, "\n-------------------\n\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            }
                        }
                        catch (Exception e) { debugMsg = "error making FEN"; }
                        break;
                    }
                    case "save history":
                    {
                        makePGN(board, whiteBotToggle, blackBotToggle);
                        debugMsg = "history saved";
                        break;
                    }
                    case "bot":
                    {
                        botPlayedLastMove = true;

                        if (!playEngineMove(board, whiteTurn)) {debugMsg = "bot error"; continue;}
                        System.out.println();
                        
                        afterAMove("move", board);
                        
                        continue;
                    }
                    case "bot depth":
                    {
                        System.out.println("please type the new bot depth: ");
                        try
                        {
                            int newDepth = Integer.parseInt(reader.nextLine().trim());
                            if (newDepth > 0 && newDepth < 30)
                            {
                                Settings.botMaxSearchDepth = newDepth;
                                debugMsg = "bot depth set to " + Settings.botMaxSearchDepth;
                            }
                            else debugMsg = "invalid depth";
                        }
                        catch (NumberFormatException e)
                        {
                            debugMsg = "invalid depth";
                        }
                        break;
                    }
                    case "bot toggle":
                    {
                        System.out.println("chose side: ");
                        
                        input = reader.nextLine().toLowerCase();
                        if (input.equals("white") || input.equals("w")) {whiteBotToggle = !whiteBotToggle; debugMsg = "white plays engine moves: " + whiteBotToggle;}
                        else if (input.equals("black") || input.equals("b")) { blackBotToggle = !blackBotToggle; debugMsg = "black plays engine moves: " + blackBotToggle;}
                        else debugMsg = "error"; 
                        break;
                    }
                    case "bot game":
                    {
                        if (Settings.restartTimerOnBotGame) gameStartTime = System.currentTimeMillis();
                        whiteBotToggle = true;
                        blackBotToggle = true;
                        debugMsg = "bots now playing eachother";
                        break;
                    }
                    case "bot time limit":
                    {
                        try
                        {
                            System.out.println("type new bot search time limit (in ms)");
                            Settings.botSearchTimeLimit = Long.parseLong(reader.nextLine().trim());
                            debugMsg = "time limit set to:" + Settings.botSearchTimeLimit; 
                        }
                        catch (NumberFormatException e)
                        {
                            debugMsg = "invalid time limit";
                        }
                        break;
                    }
                    case "bot roots":
                    {
                        Settings.showBotRootScores = !Settings.showBotRootScores;
                        debugMsg = "bots roots [" + (Settings.showBotRootScores? "shown]" : "hidden]");
                        break;
                    }
                }
                if (!debugMsg.equals("")) { clear(); continue; }
            }
            
            
            int[] piece = getSquare(input);
            int promotionRow = whiteTurn? 0 : 7;
            char promotionPiece = 'z'; // z = not picked yet
            boolean[][] validMoves = new boolean[8][8];
            int[] loc = new int[2];

            // two step move (e2 then e4)
            if (piece != null && board.getPiece(piece).isOccupied() && board.getPiece(piece).isWhite() == whiteTurn)
            {
                clear();
                validMoves = board.validMoves(piece, false);
                UI.printBoard(board, validMoves, null);
                System.out.print("chose your destination: ");

                loc = getSquare(reader.nextLine().toLowerCase());
                if (loc == null || !validMoves[loc[0]][loc[1]])
                {  
                    clear();
                    System.out.println(board.getIllegalReason(piece, loc));
                    error = true; 
                    continue;
                }
            }
            // notation move (Nf3)
            else
            {
                String cleanInput = rawInput.trim().replaceAll("[+#!?x]", "").replace('0', 'O'); // 0-0 works as O-O
                boolean hasPromotion = cleanInput.indexOf("=") != -1;
                char[] promotionPieceList = {'q', 'r', 'n', 'b'};
                int matches = 0;
                boolean exactMatch = false;

                for (int[] move : Moves.allMoves(board, whiteTurn))
                {
                    boolean isPromotion = board.getPiece(move[0], move[1]).getPiece() == 'P' && move[2] == promotionRow;
                    int tries = (isPromotion && hasPromotion)? 4 : 1;

                    for (int i = 0; i < tries; i++)
                    {
                        Board copyBoard = new Board(board); // fresh copy every try
                        copyBoard.applyMove(new int[]{move[0], move[1]}, new int[]{move[2], move[3]}, false, promotionPieceList[i]);
                        String sanMove = copyBoard.getHistory().get(0).replaceAll("[+#!?x]", "");
                        if (isPromotion && !hasPromotion) sanMove = sanMove.substring(0, sanMove.indexOf("=")); // e8 matches e8=Q then asks later

                        if (!cleanInput.equalsIgnoreCase(sanMove)) continue;

                        // exact case beats ignore case (bxc4 = pawn, Bxc4 = bishop)
                        boolean exact = cleanInput.equals(sanMove);
                        if (exactMatch && !exact) continue;
                        if (exact && !exactMatch) matches = 0;
                        exactMatch = exact;
                        matches++;

                        piece = new int[]{move[0], move[1]};
                        loc = new int[]{move[2], move[3]};
                        promotionPiece = (isPromotion && hasPromotion)? promotionPieceList[i] : 'z';
                    }
                }

                if (matches != 1)
                {
                    clear();
                    if (matches > 1) System.out.println("ambiguous move, use a capital B for bishop moves");
                    else if (piece != null) System.out.println(board.getIllegalReason(piece, new int[2])); // typed a square that isnt your piece
                    else System.out.println("illegal move or unknown command");
                    error = true;
                    continue;
                }
            }

            clear();
            // promotion check
            
            if (board.getPiece(piece).getPiece() == 'P' && loc[0] == promotionRow && promotionPiece == 'z')
            {
                while (true)
                {
                    System.out.println("chose a piece to promote to: Q, R, N, B");
                    try
                    {
                        char promote = reader.nextLine().toLowerCase().charAt(0);
                        if (promote == 'q') { promotionPiece = 'q'; break; }
                        if (promote == 'r') { promotionPiece = 'r'; break; }
                        if (promote == 'n') { promotionPiece = 'n'; break; }
                        if (promote == 'b') { promotionPiece = 'b'; break; }
                    }
                    catch (StringIndexOutOfBoundsException e) { System.out.println("invalid input try again"); }
                }
            }

            board.applyMove(piece, loc, false, promotionPiece);
            if (Settings.highlightPreviousMove)
            {
                previousMove = new boolean[8][8];
                previousMove[loc[0]][loc[1]] = true;
                previousMove[piece[0]][piece[1]] = true;
            }

            afterAMove("move", board);
            // reset bot values to default / unreachable values
            botPlayedLastMove = false;
        }
    }

    public static String gameOverCheck()
    {
        if(Moves.hasNoLegalMoves(board, whiteTurn))
        {
            if (inCheck)
            {
                gameResult = whiteTurn? "0-1" : "1-0";
                return (whiteTurn? "Black" : "White") + " has won by checkmate!";
            }
            else
            {
                gameResult = "1/2-1/2";
                return "The game has ended in a stalemate!";
            }
        }
        if (setUp) return null;

        // insufficient material check(-10 for king)
        if (Board.isInsufficientMaterial(board))
        {
            gameResult = "1/2-1/2";
            return "The game has ended in a draw by insufficient material!";
        }
        // repetition check
        long currPos = Zobrist.makeZobristHash(board, whiteTurn);
        if (repetition.getOrDefault(currPos, 0) >= 3)
        {
            gameResult = "1/2-1/2";
            return "The game has ended in a draw by repetition!";
        }
        // 50 move rule
        if (board.getTimeSinceCaptureOrPawnPush() >= 100 )
        {
            gameResult = "1/2-1/2";
            return "The game has ended in a draw by the 50 move rule!";
        }

        return null;
    }
    
    public static void afterAMove(String protocol, Board board)
    {
        protocol = protocol.toLowerCase();
        switch (protocol) 
        {
            case "move":
            {
                whiteTurn = !whiteTurn;
                setUp = false;
                break;
            }
            case "add":
            {
                board.updateCastlingRights();
                board.updateKingLocs();
                board.setHistory(new ArrayList<>());
                board.setBlackStarted(!whiteTurn);
                board.setEnPassantSquare(null);
                importedFEN = board.makeFEN(whiteTurn);
                repetition.clear();
                break;
            }
            case "clear":
            {
                board.setHistory(new ArrayList<>());
                board.setBlackStarted(!whiteTurn);
                board.setEnPassantSquare(null);
                board.updateCastlingRights();
                board.updateKingLocs();
                importedFEN = board.makeFEN(whiteTurn);
                previousMove = null;
                repetition.clear();
                setUp = true;
                break;
            }
            case "reset":
            {
                importedFEN = "";
                previousMove = null;
                TTEntry.ttTable = new TTEntry[TTEntry.TT_SIZE];
                repetition.clear();
                setUp = false;
                break;
            }
            case "fen":
            {
                previousMove = null;
                TTEntry.ttTable = new TTEntry[TTEntry.TT_SIZE];
                board.updateCastlingRights();
                repetition.clear();
                Game.board = board;
                setUp = false;
                break;
            }
            default:
            {
                System.out.println("error with after a move protocol");
                System.out.println("protocol inputed: " + protocol);
            }
        }
        long zobristHash = Zobrist.makeZobristHash(board, whiteTurn);
        repetition.merge(zobristHash, 1, Integer::sum);
        inCheck = Moves.inCheck(board, board.getKingLoc(whiteTurn), whiteTurn);
    }

    public static boolean playEngineMove(Board board, boolean whiteTurn)
    {
        UI.stillThinking = true;
        Thread thinking = new Thread(UI.thinkingAnimation);

        System.out.println("\npress enter to cancel");
        thinking.start();
        try
        {
            int[] botMove = Bot.bestMove(board, Settings.botMaxSearchDepth, whiteTurn);
            if (botMove != null) board.applyMove(new int[]{botMove[0], botMove[1]}, new int[]{botMove[2], botMove[3]}, false, 'q');
            else return false;
        }
        finally
        {
            if (Bot.stopThinking && (whiteBotToggle || blackBotToggle)) debugMsg = "bot stoped type 'bot' to resume"; 
            UI.stillThinking = false;
            try 
            {
                thinking.interrupt();
                thinking.join();
                while (System.in.available() > 0)
                {
                    System.in.read();
                }
            } 
            catch (Exception e) {}
        }
        return true;
    }
    
    public static int[] getSquare(String input)
    {
        input = input.toLowerCase();
        if (input.length() != 2) return null; // crash prevention
        
        char file = input.charAt(0);
        char rank = input.charAt(1);
        
        if (file < 'a' || file > 'h') return null; // crash prevention 
        if (rank < '1' || rank > '8') return null; // crash prevention
        
        int col = file - 'a'; // a - a = 0 & a - h = 7
        int row = 8 - (rank - '0'); // makes rank an int then flips it
        
        int[] square = new int[2];
        square[0] = row;
        square[1] = col;
        return square;
    }
    
    public static void makePGN(Board board, boolean whiteBotToggle, boolean blackBotToggle)
    {
        Path filePath = Paths.get("History.txt");
        List<String> gameHistory = board.getHistory();
        StringBuilder historySB = new StringBuilder(); Date now = new Date();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd");

        historySB.append("[Event \"" + Settings.eventPGN + "\"]\n");
        historySB.append("[Date \"" + (dateFormatter.format(now)) + "\"]\n");
        historySB.append("[Round \"1\"]\n");
        historySB.append("[White \"" + (whiteBotToggle? "bot" : "player") + "\"]\n");
        historySB.append("[Black \"" + (blackBotToggle? "bot" : "player") + "\"]\n");
        historySB.append("[Result \"" + Game.gameResult + "\"]\n");
        if (!Game.importedFEN.equals(""))
        {
            historySB.append("[SetUp \"1\"]\n");
            historySB.append("[FEN \"" + Game.importedFEN + "\"]\n");
        }
        historySB.append("\n");
        if (blackBotToggle || whiteBotToggle) 
        {
            historySB.append("{bot search time limit: " + Settings.botSearchTimeLimit + "}\n");
            historySB.append("{bot max depth: " + Settings.botMaxSearchDepth + "}\n");
        }
        historySB.append("{game lasted for: " + convertMs(System.currentTimeMillis() - gameStartTime) + "}\n");

        int offset = (board.getBlackStarted()? 1 : 0);
        for (int i = 0; i < gameHistory.size(); i++)
        {
            int moveNumber = ((i + offset) / 2) + board.getMoveOffSet();
            if ((i + offset) % 2 == 0) historySB.append(moveNumber + ". " + gameHistory.get(i) + " "); // white move
            else if (i == 0) historySB.append(moveNumber + "... " + gameHistory.get(i) + " "); // black moved first
            else historySB.append(gameHistory.get(i) + " "); // black move
        }
        historySB.append(gameResult + "\n");
        try 
        {
            Files.writeString(filePath, historySB.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Files.writeString(filePath,"\n-------------------\n\n", StandardOpenOption.APPEND);
        }
        catch (IOException e)
        {
            System.out.println("error printing to file");
        }
    }
    
    public static String convertMs(long ms)
    {
        int hours, mins, seconds, milliseconds;
        if (ms >= 3600000) hours = (int)(ms / 3600000); else hours = -1;
        if (ms >= 60000) mins = (int)((ms / 60000) % 60); else mins = -1;
        if (ms >= 1000) seconds = (int)((ms / 1000) % 60); else seconds = -1;
        milliseconds = (int)(ms % 1000);

        if (hours > -1) return String.format("%d:%02d:%02d.%03d", hours, mins, seconds, milliseconds);
        if (mins > -1) return String.format("%d:%02d.%03d", mins, seconds, milliseconds);
        if (seconds > -1) return String.format("%d.%03d", seconds, milliseconds);
        return String.format("0.%03d", milliseconds);
    }

    public static void clear()
    {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}