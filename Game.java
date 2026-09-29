/*
    ---TO DO---

    ---QOL---
    done    Add play against bot
    done    Fix ui capture advantage to account for other sides captures
    Make debug & bot info easier to disable individual stats
    Make more quick start cmds
    Shrink ui
    
    ---Bot---
    *** Repetition detection (eval)
    Killer moves (speed)
    History heuristic (speed)
    Transposition table + Zobrist (speed)
    King attack awareness (eval)
    Rooks on open and semi-open files (eval)
    Knight outposts (eval)
*/

import java.util.*;
import java.io.IOException;
import java.nio.file.*;

public class Game
{
    // debugMenu WIP
    public static boolean debugMenu;
    public static boolean eval;
    public static int botMoveTimeMs = -1;
    public static int botMoveEval = 123123123;
    public static boolean botPlayedLastMove = false;
    public static String debugMsg = "";   

    // bot controls
    public static long botSearchTimeLimit = 2500;
    public static int botMaxSearchDepth = 5;

    public static void main(String[] args)
    {
        Scanner reader = new Scanner(System.in);
        
        boolean error = false;
        
        boolean whiteTurn = true;
        boolean inCheck = false;
        int[] kingSquare = new int[2];
        
        boolean whiteBotToggle = false;
        boolean blackBotToggle = false;
        
        /* reg settings *
        boolean safeColors = false;
        boolean bigNotation = false;
        boolean quickStart = false;
        boolean debug = false;
        //*/

        /* vscode testing */
        boolean safeColors = true;
        boolean bigNotation = false;
        boolean quickStart = true;
        boolean debug = true;
        //*/

        // start up screen
        // WIP
        UI.welcome();
        while (true)
        {
            if (error) { clear(); UI.error(); error = false; }
            
            String input = reader.nextLine().toLowerCase(); // gets user input
                 if (input.equals("start")) break;
            else if (input.equals("help")) UI.help();
            else if (input.equals("qs")) { quickStart = true; break; }
            else if (input.equals("dqs")) { quickStart = true; debug = true; break; }
            else if (input.equals("debug")) debug = !debug;
            else if (input.equals("bot"))
            {
                System.out.println("would you like to play as white or black");
                input = reader.nextLine().toLowerCase();
                if (input.equals("white") || input.equals("w")) blackBotToggle = true;
                else if (input.equals("black") || input.equals("b")) whiteBotToggle = true;
                else {error = true; continue;}
                break;
            }
            else error = true;
        }
        
        // pre game color screen
        while (true && !quickStart)
        {
            if (error) { System.out.println("Invalid input"); error = false; }
            else clear();
            System.out.println("Console safe colors[" + (safeColors? "ON" : "OFF") + "]");
            UI.preGameColor();
            
            String input = reader.nextLine().toLowerCase(); // gets user input
            if (input.equals("color")) safeColors =! safeColors;
            else if (input.equals("")) break;
            else error = true;
        }
        
        // pre game notation
        while (true && !quickStart)
        {
            if (error) { System.out.println("Invalid input"); error = false; }
            else clear();
            System.out.println("Expanded notation[" + (bigNotation? "ON" : "OFF") + "]");
            UI.preGameNotation();
            
            String input = reader.nextLine().toLowerCase(); // gets user input
            if (input.equals("expand")) bigNotation =! bigNotation;
            else if (input.equals("")) break;
            else error = true;
        }
        
        Board board = new Board(safeColors, bigNotation);
        debugMenu = debug;
        
        // game
        while (true)
        {
            // clear console if no error
            if (error) error = false;
            else if (!debugMsg.equals("")) { System.out.println(debugMsg); debugMsg = ""; }
            else clear();
            
            if (inCheck)
            {
                if (Moves.hasNoLegalMoves(board, whiteTurn))
                {
                    board.printBoard(null, kingSquare[0], kingSquare[1]);
                    System.out.print((whiteTurn? "Black" : "White") + " has won by checkmate!");
                    saveHistory(board);
                    return;
                }
            }
            else
            {
                if (Moves.hasNoLegalMoves(board, whiteTurn))
                {
                    board.printBoard(null, kingSquare[0], kingSquare[1]);
                    System.out.println("The game has ended in a stalemate!");
                    saveHistory(board);
                    return;
                }
            }
            
            // prints board when in check
            if (inCheck) 
            {
                board.printBoard(null, kingSquare[0], kingSquare[1]);
                System.out.print((whiteTurn? "White" : "Black") + " is in check!"); //turn check
                System.out.print(" chose your piece: ");
            }
            else 
            {
                board.printBoard(null, -1, -1);
                System.out.print(whiteTurn? "White's turn" : "Black's turn"); //turn check
                System.out.print(" chose your piece: ");
            }
            
            // bot moves
            if (whiteBotToggle && whiteTurn || blackBotToggle && !whiteTurn)
            {
                botPlayedLastMove = true;

                if (!playEngineMove(board, whiteTurn)) {debugMsg = "bot error"; continue;}
                System.out.println();
                // finish move sequence
                whiteTurn = !whiteTurn;
    
                // is in check finder
                inCheck = false;
                kingSquare = Board.findKing(board, whiteTurn);
                if (Moves.inCheck(board, kingSquare, whiteTurn)) inCheck = true;
                continue;
            }
            
            String input = reader.nextLine().toLowerCase();
            
            // debug cmds
            if (debug)
            {
                if (input.equals("history"))
                {
                    int counter = 1;
                    debugMsg = "history:|";
                    for (int i = 0; i < board.getHistory().size(); i += 2)
                    {
                        if (i + 1  == board.getHistory().size()) 
                        { debugMsg += (counter) + ". " + board.getHistory().get(i) + "|";}
                        else
                        { debugMsg += (counter) + ". " + board.getHistory().get(i) + "," + board.getHistory().get(i + 1) + "|"; }
                        counter++;
                    }
                }
                else if (input.equals("info"))
                {
                    int counter = 1;
                    System.out.println("input square: ");
                    Piece p;
                    try { p = board.getPiece(Board.convertMove(reader.nextLine())); }
                    catch (ArrayIndexOutOfBoundsException e) 
                    { 
                        debugMsg = "invalid string"; 
                        clear(); 
                        continue;
                    }
                    
                    if (p == null) debugMsg = "invaild square location";
                    else if (!p.isOccupied()) debugMsg = "square is empty";
                    else
                    {
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
                }
                else if (input.equals("add"))
                {
                    boolean valid = true;
                    
                    System.out.println("type piece location: ");
                    int[] customLoc = getSquare(reader.nextLine().toLowerCase()); // null if bad square
                    if (customLoc == null) valid = false;
                    
                    if (valid)
                    {
                        System.out.println("input one charictar piece type\n(enter a blank line to make an empty square): ");
                        input = reader.nextLine();
                        
                        if (input.equals("")) // empty square path, stops here
                        {
                            board.addPiece(customLoc);
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
                            
                            if (valid) board.addPiece(customPiece, customLoc, customColor);
                        }
                    }
                    
                    debugMsg = valid? "added piece" : "could not add piece";
                }
                else if (input.equals("toggle"))
                {
                    debugMenu = !debugMenu;
                    debugMsg = "Debug menu[" + (debugMenu? "ON" : "OFF") + "]";
                }
                else if (input.equals("cmds"))
                { 
                    debugMsg = UI.helpDebug(); 
                }
                else if (input.equals("reset"))
                {
                    debugMsg = "board reset";
                    board = new Board(safeColors, bigNotation);
                    whiteTurn = true;

                    // is in check finder
                    inCheck = false;
                    kingSquare = Board.findKing(board, whiteTurn);
                    if (Moves.inCheck(board, kingSquare, whiteTurn)) inCheck = true;
                    
                    continue;
                }
                else if (input.equals("clear"))
                {
                    debugMsg = "board cleared";
                    for (int i = 0; i < 8; i++)
                    {
                        for (int j = 0; j < 8; j++)
                        {
                            if (board.getPiece(i, j).getPiece() != 'K')board.addPiece(new int[]{i, j});
                        }
                    }

                    // is in check finder
                    inCheck = false;
                    kingSquare = Board.findKing(board, whiteTurn);
                    if (Moves.inCheck(board, kingSquare, whiteTurn)) inCheck = true;
                    
                    continue;
                }
                else if (input.equals("save history"))
                {
                    saveHistory(board);
                }
                else if (input.equals("eval"))
                {
                    eval = true;
                }
                else if (input.equals("et"))
                {
                    eval = true;
                    debugMenu = !debugMenu;
                    debugMsg = "Debug menu[" + (debugMenu? "ON" : "OFF") + "]";
                }
                else if (input.equals("bot"))
                {
                    botPlayedLastMove = true;

                    if (!playEngineMove(board, whiteTurn)) {debugMsg = "bot error"; continue;}
                    System.out.println();
                    // finish move sequence
                    whiteTurn = !whiteTurn;
        
                    // is in check finder
                    inCheck = false;
                    kingSquare = Board.findKing(board, whiteTurn);
                    if (Moves.inCheck(board, kingSquare, whiteTurn)) inCheck = true;
                    
                    continue;
                }
                else if (input.equals("bot depth"))
                {
                    System.out.println("please type the new bot depth: ");
                    try
                    {
                        int newDepth = Integer.parseInt(reader.nextLine().trim());
                        if (newDepth >= 1 && newDepth <= 7)
                        {
                            botMaxSearchDepth = newDepth;
                            debugMsg = "bot depth set to " + botMaxSearchDepth;
                        }
                        else debugMsg = "depth must be 1 - 7";
                    }
                    catch (NumberFormatException e)
                    {
                        debugMsg = "invalid depth";
                    }
                }
                else if (input.equals("bot toggle"))
                {
                    System.out.println("chose side: ");
                    
                    input = reader.nextLine().toLowerCase();
                    if (input.equals("white") || input.equals("w")) {whiteBotToggle = !whiteBotToggle; debugMsg = "white plays engine moves: " + whiteBotToggle;}
                    else if (input.equals("black") || input.equals("b")) { blackBotToggle = !blackBotToggle; debugMsg = "black plays engine moves: " + blackBotToggle;}
                    else debugMsg = "error"; 
                }
                else if (input.equals("bot game"))
                {
                    whiteBotToggle = true;
                    blackBotToggle = true;
                    debugMsg = "bots now playing eachother";
                }
                else if (input.equals("bot time limit"))
                {
                    try
                    {
                        System.out.println("type new bot search time limit (in ms)");
                        botSearchTimeLimit = Long.parseLong(reader.nextLine().trim());
                        debugMsg = "time limit set to:" + botSearchTimeLimit; 
                    }
                    catch (NumberFormatException e)
                    {
                        debugMsg = "invalid time limit";
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
            
            int[] loc = getSquare(reader.nextLine()); // get users end spot
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
            
            whiteTurn = !whiteTurn;
            
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
        thinking.start();
        try
        {
            int[] botMove = Bot.bestMove(board, botMaxSearchDepth, whiteTurn);
            if (botMove != null) board.applyMove(new int[]{botMove[0], botMove[1]}, new int[]{botMove[2], botMove[3]}, false, 'q');
            else return false;
        }
        finally
        {
            UI.stillThinking = false;
            try {thinking.join();} catch (Exception e) {}
        }
        return true;
    }
    
    public static int[] getSquare(String input)
    {
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
    
    public static void saveHistory(Board board)
    {
        Path filePath = Paths.get("History.txt");
        ArrayList<String> gameHistory = board.getHistory();
        StringBuilder historySB = new StringBuilder();
        
        for (int i = 1; i <= gameHistory.size(); i++)
        {
            historySB.append(i + ". " + gameHistory.get(i - 1) + System.lineSeparator());
        }
        try 
        {
            Files.writeString(filePath, historySB.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Files.writeString(filePath, "-------------------" + System.lineSeparator(), StandardOpenOption.APPEND);
        }
        catch (IOException e)
        {
            System.out.println("error printing to file");
        }
    }
    
    public static void clear()
    {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}