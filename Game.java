/*
    ---TO DO---
    50 move rule

    ---QOL---
    make it possible to play moves using notation (e4 plays e4 instead of doing e2 e4)
    add tests
    Undo / takeback (~30 min)
    PGN import (~1-2 hrs)
    Flip the board (~20 min)
    Show the principal variation / PV (~45 min)
    Eval breakdown command (~45 min)
    Hint command (~10 min)
    Analysis mode (~30 min)
    Perft command
    Bench command (~20 min)
    Small opening book (~1 hr)

    ---Bot---
    done    Null move pruning (speed)
    done    Late move reductions / LMR (speed) ~1-1.5 hours
    Principal variation search / PVS (speed) ~1 hour
    Tempo bonus (eval) ~10 minutes
    Tapered eval (eval) ~1.5-2 hours
    Delta pruning in quiescence (speed) ~20-30 minutes
    Aspiration windows (speed) ~45 minutes-1 hour
    UCI (testing) ~3-4 hours, plus setting up a GUI or match runner
    King safety upgrade (eval) ~1.5-2 hours
    Time management (eval) ~1 hour for a basic version
    Make/unmake + incremental Zobrist (speed) ~6-10 hours, spread over a few sessions
    Lazy SMP (speed) ~4-6 hours, mostly untangling the static variables
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

    public static ArrayList<Long> positionKeys = new ArrayList<>();

    public static void main(String[] args)
    {
        Scanner reader = new Scanner(System.in);
        
        boolean error = false;
        
        boolean whiteTurn = true;
        boolean inCheck = false;
        int[] kingSquare = new int[2];
        
        boolean whiteBotToggle = false;
        boolean blackBotToggle = false;
        

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
        
        Board board = new Board(Settings.safeColors, Settings.bigNotation);
        positionKeys.add(Zobrist.makeZobristHash(board, whiteTurn));

        








        gameStartTime = System.currentTimeMillis();

        gameLoop: while (true)
        {
            if (!Settings.highlightPreviousMove) previousMove = null;

            // clear console if no error
            if (error) error = false;
            else if (!debugMsg.equals("")) { System.out.println(debugMsg); debugMsg = ""; }
            else 
            {
                clear();
                // insufficient material check(-10 for king)
                if (board.getBoardValue(true) - 10 <= 3 && board.getBoardValue(false) - 10 <= 3)
                {
                    if (board.getBoardPawnCount(true) + board.getBoardPawnCount(false) == 0)
                    {
                        board.printBoard(previousMove, -1, -1);
                        System.out.println("The game has ended in a draw by insufficient material!");
                        gameResult = "1/2-1/2";
                        saveHistory(board, whiteBotToggle, blackBotToggle);
                        break gameLoop;
                    }
                }
                // repetition check
                if (Collections.frequency(positionKeys, positionKeys.get(positionKeys.size() - 1)) >= 3)
                {
                    board.printBoard(previousMove, -1, -1);
                    System.out.println("The game has ended in a draw by repetition!");
                    gameResult = "1/2-1/2";
                    saveHistory(board, whiteBotToggle, blackBotToggle);
                    break gameLoop;
                }
            }

            if (inCheck)
            {
                if (Moves.hasNoLegalMoves(board, whiteTurn))
                {
                    board.printBoard(previousMove, kingSquare[0], kingSquare[1]);
                    System.out.print((whiteTurn? "Black" : "White") + " has won by checkmate!");
                    gameResult = whiteTurn? "0-1" : "1-0";
                    saveHistory(board, whiteBotToggle, blackBotToggle);
                    break gameLoop;
                }
                else
                {
                    board.printBoard(previousMove, kingSquare[0], kingSquare[1]);
                    System.out.print((whiteTurn? "White" : "Black") + " is in check!");
                    System.out.print(" chose your piece: ");
                }
            }
            else
            {
                if (Moves.hasNoLegalMoves(board, whiteTurn))
                {
                    board.printBoard(previousMove, -1, -1);
                    System.out.println("The game has ended in a stalemate!");
                    gameResult = "1/2-1/2";
                    saveHistory(board, whiteBotToggle, blackBotToggle);
                    break gameLoop;
                }
                else
                {
                    board.printBoard(previousMove, -1, -1);
                    System.out.print(whiteTurn? "White's turn" : "Black's turn");
                    System.out.print(" chose your piece: ");
                }
                
            }
            
            // bot moves
            if (whiteBotToggle && whiteTurn || blackBotToggle && !whiteTurn)
            {
                botPlayedLastMove = true;

                if (!playEngineMove(board, whiteTurn)) {debugMsg = "bot error"; continue;}
                System.out.println();
                // finish move sequence
                whiteTurn = !whiteTurn;
    
                // save position
                positionKeys.add(Zobrist.makeZobristHash(board, whiteTurn));
                // is in check finder
                inCheck = false;
                kingSquare = Board.findKing(board, whiteTurn);
                if (Moves.inCheck(board, kingSquare, whiteTurn)) inCheck = true;
                
                continue;
            }
            
            String input = reader.nextLine().toLowerCase();
            
            // debug cmds
            if (Settings.enableDebug)
            {
                switch (input)
                {
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
                    case "info":
                    {
                        int counter = 1;
                        System.out.println("input square: ");
                        int[] piece = getSquare(reader.nextLine());
                        
                        
                        if (piece == null) debugMsg = "invaild square location";
                        else if (!board.getPiece(piece).isOccupied()) debugMsg = "square is empty";
                        else
                        {
                            Piece p = board.getPiece(piece); 
                            board.validMoves(new int[]{p.getX(), p.getY()}, false);
                            debugMsg += 
                            "Square: " + Board.convertMove(new int[]{p.getX(), p.getY()}) + " [" + p.getX() + ", " + p.getY() + "]" +
                            "\nPiece: " + p.getPiece() +
                            "\nColor: " + (p.isWhite()? "white" : "black") +
                            "\nAll moves:" + board.getFakeMoves().size() + " " + board.getFakeMoves() +
                            "\nLegal moves:" + board.getLegalMoves().size() + " " + board.getLegalMoves() +
                            "\nHistory:" + p.getHistory().size() + " [";
                            for(String s : p.getHistory())
                            {
                                debugMsg += counter + "." + s; 
                                debugMsg += (counter != p.getHistory().size()? "|" : "]");
                                counter++;
                            }
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
                            
                            if (input.equals("")) // empty square path, stops here
                            {
                                board.setHistory(new ArrayList<>());
                                board.setBlackStarted(!whiteTurn);
                                board.addPiece(customLoc); 
                                importedFEN = board.makeFEN(whiteTurn);
                                positionKeys.clear(); 
                                positionKeys.add(Zobrist.makeZobristHash(board, whiteTurn));
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
                                    board.setHistory(new ArrayList<>());
                                    board.setBlackStarted(!whiteTurn);
                                    board.addPiece(customPiece, customLoc, customColor); 
                                    importedFEN = board.makeFEN(whiteTurn);
                                    positionKeys.clear(); 
                                    positionKeys.add(Zobrist.makeZobristHash(board, whiteTurn));
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
                        board = new Board(Settings.safeColors, Settings.bigNotation);
                        whiteTurn = true;

                        importedFEN = "";
                        previousMove = null;
                        positionKeys.clear();
                        TTEntry.ttTable = new TTEntry[TTEntry.TT_SIZE];
                        // save position
                        positionKeys.add(Zobrist.makeZobristHash(board, whiteTurn));
                        // is in check finder
                        inCheck = false;
                        kingSquare = Board.findKing(board, whiteTurn);
                        if (Moves.inCheck(board, kingSquare, whiteTurn)) inCheck = true;
                        
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

                        board.setHistory(new ArrayList<>());
                        board.setBlackStarted(!whiteTurn);
                        board.setEnPassantSquare(null);
                        importedFEN = board.makeFEN(whiteTurn);
                        previousMove = null;
                        positionKeys.clear();
                        // save position
                        positionKeys.add(Zobrist.makeZobristHash(board, whiteTurn));
                        // is in check finder
                        inCheck = false;
                        kingSquare = Board.findKing(board, whiteTurn);
                        if (Moves.inCheck(board, kingSquare, whiteTurn)) inCheck = true;
                        
                        continue;
                    }
                    case "fen":
                    {
                        System.out.println("please input your fen");
                        input = reader.nextLine();
                        try
                        {
                            Board newBoard = new Board(Settings.safeColors, Settings.bigNotation);
                            whiteTurn = newBoard.importFEN(input);

                            previousMove = null;
                            positionKeys.clear();
                            TTEntry.ttTable = new TTEntry[TTEntry.TT_SIZE];
                            // save position
                            positionKeys.add(Zobrist.makeZobristHash(newBoard, whiteTurn));
                            // is in check finder
                            inCheck = false;
                            kingSquare = Board.findKing(newBoard, whiteTurn);
                            if (Moves.inCheck(newBoard, kingSquare, whiteTurn)) inCheck = true;

                            board = newBoard;
                            debugMsg = "FEN imported";
                            Game.importedFEN = input;
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
                        saveHistory(board, whiteBotToggle, blackBotToggle);
                        debugMsg = "history saved";
                        break;
                    }
                    case "bot":
                    {
                        botPlayedLastMove = true;

                        if (!playEngineMove(board, whiteTurn)) {debugMsg = "bot error"; continue;}
                        System.out.println();
                        // finish move sequence
                        whiteTurn = !whiteTurn;
                        // save position
                        positionKeys.add(Zobrist.makeZobristHash(board, whiteTurn));
                        // is in check finder
                        inCheck = false;
                        kingSquare = Board.findKing(board, whiteTurn);
                        if (Moves.inCheck(board, kingSquare, whiteTurn)) inCheck = true;
                        
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
            
            
            int[] piece = getSquare(input); // get users piece
            
            // bad square & empty square & correct color check
            if (piece == null || !board.getPiece(piece).isOccupied() || board.getPiece(piece).isWhite() != whiteTurn) 
            {  
                clear();
                System.out.println(board.getIllegalReason(piece, new int[2]));
                error = true; 
                continue;
            } 
            clear();
            
            boolean[][] validMoves = board.validMoves(piece, false); // finds valid moves
            board.printBoard(validMoves, -1, -1); //shows valid moves
            System.out.print("chose your destination: "); // ask for end spot
            
            int[] loc = getSquare(reader.nextLine().toLowerCase()); // get users end spot
            if (loc == null || !validMoves[loc[0]][loc[1]]) // bad square & invaild move check
            {  
                clear();
                System.out.println(board.getIllegalReason(piece, loc));
                error = true; 
                continue;
            }

            // promotion check
            int promotionRow = whiteTurn? 0 : 7;
            char promotionPiece = 'q';
            if (board.getPiece(piece).getPiece() == 'P' && loc[0] == promotionRow)
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
            whiteTurn = !whiteTurn;
            // save position
            positionKeys.add(Zobrist.makeZobristHash(board, whiteTurn));
            // is in check finder
            inCheck = false;
            kingSquare = Board.findKing(board, whiteTurn);
            if (Moves.inCheck(board, kingSquare, whiteTurn)) inCheck = true;
            

            // reset bot values to default / unreachable values
            botPlayedLastMove = false;
        }
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
    
    public static void saveHistory(Board board, boolean whiteBotToggle, boolean blackBotToggle)
    {
        Path filePath = Paths.get("History.txt");
        ArrayList<String> gameHistory = board.getHistory();
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

        int offset = board.getBlackStarted()? 1 : 0;
        for (int i = 0; i < gameHistory.size(); i++)
        {
            int moveNumber = (i + offset) / 2 + 1;
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