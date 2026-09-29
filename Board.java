import java.util.*;
public class Board 
{
    // board is fliped
    // 0 is 8th rank
    // 7 is 1st rank

    // colors
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String RED_BG  = "\033[41m";
    private static final String LIGHT_MOVE_SQ = "\033[46m";
    private static final String DARK_MOVE_SQ  = "\033[44m";
    
    private static final String DEFAULT_WHITE = "\033[39m";
    private static final String DEFAULT_BLACK = "\033[38;5;16m";
    private static final String DEFAULT_LIGHT_SQ = "\033[48;5;250m";  
    private static final String DEFAULT_DARK_SQ  = "\033[48;5;240m";

    private static final String SAFE_WHITE = "\033[34m";
    private static final String SAFE_BLACK = "\033[35m";
    private static final String SAFE_LIGHT_SQ = "\033[47m";  
    private static final String SAFE_DARK_SQ  = "\033[40m";
    
    private String BLACK;
    private String WHITE;
    private String DARK_SQ;
    private String LIGHT_SQ;
    
    private Piece[][] playerBoard; // board that holds all pieces
    private ArrayList<String> history; // all moves on board
    private ArrayList<Piece> whiteCaptured; // pieces white has taken
    private ArrayList<Piece> blackCaptured; // pieces black has taken
    
    private int[] enPassantSquare; // for en passant captures

    private boolean safeColor;
    private boolean bigNotation;
    
    // debug vars
    private ArrayList<String> fakeMoves;
    private ArrayList<String> legalMoves;

    public Board(boolean safeColor, boolean bigNotation)
    {
        this.safeColor = safeColor;
        this.bigNotation = bigNotation;
        blackCaptured = new ArrayList<Piece>();
        whiteCaptured = new ArrayList<Piece>();
        history = new ArrayList<String>();
        enPassantSquare = null;
        
        playerBoard = new Piece[8][8];
        difine();
    }
    
    // makes a duped board for checks without altering real board
    public Board(Board board)
    {
        playerBoard = new Piece[8][8];
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                Piece p = board.playerBoard[row][col];
                // make copy of piece
                if (p.isOccupied()) playerBoard[row][col] = new Piece(p.isWhite(), p.getPiece(), row, col, p.hasMoved(), p.hasCastled(), false);
                else playerBoard[row][col] = Piece.blankSquare;
            }
        }
        
        //needed for no crash
        whiteCaptured = new ArrayList<>();
        blackCaptured = new ArrayList<>();
        history = new ArrayList<>();
        if (board.enPassantSquare != null) enPassantSquare = board.getEnPassantSquare();
        else enPassantSquare = null;
    }
    
    // geters
    public Piece[][] getBoard(){ return playerBoard; }
    public Piece getPiece(int[] cords) { return playerBoard[cords[0]][cords[1]]; }
    public Piece getPiece(int row, int col) { return playerBoard[row][col]; }
    public ArrayList<String> getHistory() { return history; }
    public int[] getEnPassantSquare() { return enPassantSquare; }
    public boolean inBounds(int row, int col) { return row >= 0 && row < 8 && col >= 0 && col < 8;}
    
    // debug geters
    public ArrayList<String> getFakeMoves() { return fakeMoves; };
    public ArrayList<String> getLegalMoves() { return legalMoves; };
    
    // debug seters
    public void addPiece(char piece, int[] cords, boolean isWhite) 
    { playerBoard[cords[0]][cords[1]] = new Piece(isWhite, piece, cords[0], cords[1], false, false,  true); }
    public void addPiece(int[] cords)
    { playerBoard[cords[0]][cords[1]] = Piece.blankSquare; }
    
    public void difine()
    {
        // fill with empty squares
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                playerBoard[row][col] = Piece.blankSquare;
            }
        }
    
        // pawns
        for (int col = 0; col < 8; col++)
        {
            playerBoard[1][col] = new Piece(false, 'P', 1, col, false, false,  true);
            playerBoard[6][col] = new Piece(true,  'P', 6, col, false, false,  true);
        }
    
        // back
        char[] piecesOrder = {'R','N','B','Q','K','B','N','R'};
        
        for (int col = 0; col < 8; col++)
        {
            playerBoard[0][col] = new Piece(false, piecesOrder[col], 0, col, false, false, true);
            playerBoard[7][col] = new Piece(true,  piecesOrder[col], 7, col, false, false, true);
        }
        
        if (safeColor)
        {
            BLACK = SAFE_BLACK;
            WHITE = SAFE_WHITE;
            DARK_SQ = SAFE_DARK_SQ;
            LIGHT_SQ = SAFE_LIGHT_SQ;
        }
        else
        {
            BLACK = DEFAULT_BLACK;
            WHITE = DEFAULT_WHITE;
            DARK_SQ = DEFAULT_DARK_SQ;
            LIGHT_SQ = DEFAULT_LIGHT_SQ;
        }
    }
    
    public static String captureValue(ArrayList<Piece> captures)
    {
        int val = 0;
        for (Piece p : captures)
        {
            if (p.getPiece() == 'P') val += 1;
            if (p.getPiece() == 'N') val += 3;
            if (p.getPiece() == 'B') val += 3;
            if (p.getPiece() == 'R') val += 5;
            if (p.getPiece() == 'Q') val += 9;
        }
        return (val > 0)? "+" + val : "";
    }
    
    //main validater
    public boolean[][] validMoves(int[] square, boolean test)
    {
        boolean[][] moves = preValidMoves(square, test); // gets moves a piece could do
        boolean white = getPiece(square).isWhite(); // gets pieces color
    
        boolean[][] legal = new boolean[8][8]; // makes new 2d array for legal moves
    
        for (int row = 0; row < 8; row++) // pin check
        {
            for (int col = 0; col < 8; col++)
            {
                if (moves[row][col]) // if piece could move to this square
                {
                    Board copy = new Board(this); // make a copy of the board
                    copy.applyMove(square, new int[]{row, col}, true, 'Q'); // applies move to copied board
    
                    int[] kingSquare = Board.findKing(copy, white); // finds king
                    if (!Moves.inCheck(copy, kingSquare, white)) legal[row][col] = true; // checks if king would be in check
                }
            }
        }
    
        // debug
        if (!Game.debugMenu || test) return legal;
        legalMoves = new ArrayList<>();
        for (int row = 0; row < 8; row++)
        {   for (int col = 0; col < 8; col++)
            {  
                if (legal[row][col]) legalMoves.add(convertMove(new int[]{row,col}));
            }
        }
        return legal;
    }
    
    // gives all moves before checking if piece is pinned
    public boolean[][] preValidMoves(int[] square, boolean test)
    {
        char pieceType = getPiece(square).getPiece();
        boolean[][] preValidMoves = new boolean[8][8];
        
        if (pieceType == 'P') preValidMoves = Moves.pawnMoves(this, square);
        if (pieceType == 'R') preValidMoves = Moves.rookMoves(this, square);
        if (pieceType == 'N') preValidMoves = Moves.knightMoves(this, square);
        if (pieceType == 'B') preValidMoves = Moves.bishopMoves(this, square);
        if (pieceType == 'Q') preValidMoves = Moves.queenMoves(this, square);
        if (pieceType == 'K') preValidMoves = Moves.kingMoves(this, square);
        
        // debug
        if (!Game.debugMenu || test) return preValidMoves;
        fakeMoves = new ArrayList<>();
        for (int row = 0; row < 8; row++)
        {   for (int col = 0; col < 8; col++)
            {  
                if (preValidMoves[row][col]) fakeMoves.add(convertMove(new int[]{row,col}));
            }
        }
        return preValidMoves;
    }
    



    public void applyMove(int[] start, int[] end, boolean test, char promotionPiece)
    {
        promotionPiece = Character.toLowerCase(promotionPiece);
        StringBuilder move = new StringBuilder();
        
        // makes temp piece and adds to its history
        Piece temp = playerBoard[start[0]][start[1]];
        
        playerBoard[start[0]][start[1]] = Piece.blankSquare; //clears the space
        
        if (temp.getPiece() != 'P') move.append(temp.getPiece());

        if (playerBoard[end[0]][end[1]].isOccupied()) //capture check
        {
            if (playerBoard[end[0]][end[1]].isWhite()) blackCaptured.add(playerBoard[end[0]][end[1]]);
            else whiteCaptured.add(playerBoard[end[0]][end[1]]);
            
            // pawn capture check for move notaion
            if (temp.getPiece() == 'P') move.append(convertMove(start).charAt(0) + "x");
            else move.append("x"); // capture notation
        }
        
        

        // castling
        if (temp.getPiece() == 'K' && (start[1] - end[1] == 2 || start[1] - end[1] == -2))
        {
            if (end[1] == 2) // long castle
            {
                playerBoard[start[0]][3] = playerBoard[start[0]][0];
                playerBoard[start[0]][0] = Piece.blankSquare;
                move = new StringBuilder("O-O-O");
                
                playerBoard[start[0]][3].setHasMoved(true);
                temp.setHasCastled(true);
                playerBoard[start[0]][3].setY(3);
            }
            else // short castle
            {
                playerBoard[start[0]][5] = playerBoard[start[0]][7];
                playerBoard[start[0]][7] = Piece.blankSquare;
                move = new StringBuilder("O-O");

                playerBoard[start[0]][5].setHasMoved(true);
                temp.setHasCastled(true);
                playerBoard[start[0]][5].setY(5);
            }
        }
        

        // en passant
        if ((enPassantSquare != null && end[0] == enPassantSquare[0] && end[1] == enPassantSquare[1] && temp.getPiece() == 'P'))
        {
            if (temp.isWhite())
            {
                whiteCaptured.add(playerBoard[end[0] + 1][end[1]]);
                playerBoard[end[0] + 1][end[1]] = Piece.blankSquare; // removes pawn before square
            }
            else
            {
                blackCaptured.add(playerBoard[end[0] - 1][end[1]]);
                playerBoard[end[0] - 1][end[1]] = Piece.blankSquare; // removes pawn before square
            }
            move.append(convertMove(start).charAt(0) + "x");
        }

        
        // en passant maker
        int doubleMove = end[0] - start[0]; // double moved pawn
        if (temp.getPiece() == 'P' && (doubleMove == -2 || doubleMove == 2)) // double moved pawn check
        {
            enPassantSquare = null; // clears old en passant square
            enPassantSquare = new int[]{(start[0] + end[0]) / 2, start[1]}; // sets square behind pawn to en passant square
        }
        else enPassantSquare = null; // clear square every non double pawn move
        

        // changes piece x and y vars
        temp.setX(end[0]);
        temp.setY(end[1]);
        
         // converts int[] to proper notation (e4, d5, etc.)
        if (temp.getPiece() != 'K' || (start[1] - end[1] != 2 && start[1] - end[1] != -2)) move.append(convertMove(end));
        
        // promotion check
        int promotionRow = temp.isWhite()? 0 : 7;
        
        if (temp.getPiece() == 'P' && end[0] == promotionRow)
        {

                 if (promotionPiece == 'q') { temp.setPiece('Q'); move.append("=Q"); }
            else if (promotionPiece == 'r') { temp.setPiece('R'); move.append("=R"); }
            else if (promotionPiece == 'n') { temp.setPiece('N'); move.append("=N"); }
            else if (promotionPiece == 'b') { temp.setPiece('B'); move.append("=B"); }
            else                            { temp.setPiece('Q'); move.append("=Q"); }
        }
        
        // finishing move notation
        
        playerBoard[end[0]][end[1]] = temp;

        if(!test && Moves.inCheck(this, findKing(this, !temp.isWhite()), !temp.isWhite())) // check notation
        {
            if (Moves.hasNoLegalMoves(this, !temp.isWhite())) move.append("#"); // checkmate
            else move.append("+"); // check
        }
        
        if (!test) 
        {
            history.add((bigNotation)? convertMove(start) + " > " + convertMove(end) : move.toString() ); // standard chess notation and square to square
            temp.addHistory(move.toString());
        }
        temp.setHasMoved(true);
    }
    
    public static int[] findKing(Board board, boolean white)
    {
        int[] kingLoc = new int [2];
        boolean found = false;
        for (int row = 0; row < 8 && !found; row++) // finds king
        {
            for (int col = 0; col < 8 && !found; col++)
            {
                if (board.getPiece(row, col).getPiece() == 'K' && board.getPiece(row, col).isWhite() == white)
                {       
                    kingLoc[0] = row;
                    kingLoc[1] = col;
                    found = true;
                }
            }
        }
        return kingLoc;
    }

    public static String convertMove(int[] move)
    {
        String stringMove = "";
        stringMove += (char)(move[1] + 'a'); // converts col to char
        stringMove += 8 - move[0]; // flips row
        return stringMove;
    }

    public static int[] convertMove(String move)
    {
        int[] square = new int[2];
        square[1] = move.charAt(0) - 'a';        // file letter -> col
        square[0] = 8 - (move.charAt(1) - '0');  // rank digit -> row
        return square;
    }

    public static String captureList(ArrayList<Piece> captures)
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


    // if no selected piece validmoves should equal null
    // check row and col are for if king in check otherwise type -1 -1

    public void printBoard(boolean[][] validMoves, int checkRow, int checkCol)
    {
        // previous move
        if (history.size() > 0) 
        {
            if (history.size() % 2 == 1) System.out.println("White Played: " + history.get(history.size() - 1));
            else System.out.println("Black Played: " + history.get(history.size() - 1));
            System.out.println();
        }
        else System.out.println("Good luck!\n");
        // black captures
        if (blackCaptured.size() > 0) System.out.println("Black " + captureValue(blackCaptured) + " : " + captureList(blackCaptured)); 
        else System.out.println("Black");
        
        // files
        System.out.println(RESET + "   a  b  c  d  e  f  g  h");
        for (int row = 0; row < 8; row++)
        {
            // ranks
            System.out.print(RESET + (8 - row) + " ");
            for (int col = 0; col < 8; col++)
            {
                // bg reset
                String bg = (col + row) % 2 == 0? LIGHT_SQ : DARK_SQ;
                // piece selection check & valid moves for it
                if (validMoves != null && validMoves[row][col]) bg = (col + row) % 2 == 0? LIGHT_MOVE_SQ : DARK_MOVE_SQ;
                // if king in check
                if (checkRow == row && checkCol == col) bg = RED_BG;
    
                // if piece is there
                if (playerBoard[row][col].isOccupied())
                {
                    // if piece is white change color to white else change color to black
                    String color = playerBoard[row][col].isWhite()? WHITE : BLACK;
                    System.out.print(BOLD + color + bg + " " + playerBoard[row][col].getPiece()+ " " + RESET);
                }
                // empty squares
                else
                {
                    System.out.print(bg + "   " + RESET);
                }
            }
            System.out.println();
        }
        
        // white captures
        if (whiteCaptured.size() > 0) System.out.println("White " + captureValue(whiteCaptured) + " : " + captureList(whiteCaptured) + "\n");
        else System.out.println("White\n");
        
        if(Game.eval) System.out.println("eval:" + (Bot.evalBoard(this) / 100.0) + "\n"); // divide by 100 to convert centipawns into eval
        
        // debug menu
        if (!Game.debugMenu) return;
        System.out.println("---- bot ----");
        // if (Game.botPlayedLastMove) System.out.println(Bot.rootScores); debugging only prints every root and its eval
        System.out.println("bot time limit: " + Game.botSearchTimeLimit  + "ms (" + (Game.botSearchTimeLimit / 1000.0) + "s)");
        System.out.println((Game.botPlayedLastMove)? "bot took: " + Game.botMoveTimeMs + "ms (" + (Game.botMoveTimeMs / 1000.0) + "s)" : "bot took: N/A");
        if (!Game.botPlayedLastMove) System.out.println("bot self eval: N/A");
        else
        {
            int botSelfEval = Game.botMoveEval * (history.size() % 2 == 0? -1 : 1);   
            if (Math.abs(botSelfEval) > Bot.MATE - 1000)
            {
                int movesTillMate = (Bot.MATE - Math.abs(botSelfEval) + 1) / 2;
                System.out.println("bot self eval: " + (botSelfEval > 0? "M" : "-M") + movesTillMate);
            }
            else System.out.println("bot self eval: " + botSelfEval / 100.0);
        }
        System.out.println((Game.botPlayedLastMove)? "bot depth reached: " + Bot.depthReached : "bot depth reached: N/A");
        System.out.println((Game.botPlayedLastMove)? "Moves analysed: " + Bot.movesAnalysed + "|" + Bot.quiescenceMovesAnalysed + "|" + Bot.movesAnalysed / Game.botMoveTimeMs + " moves per ms" : "Moves analysed: N/A");
        System.out.println("\n----debug----");
        System.out.println("eval: " + Bot.evalBoard(this) / 100.0); // divide by 100 to convert centipawns into eval
        // System.out.println("turn:" + history.size() + "|" + ((history.size() % 2 == 0)? "WHITE" : "BLACK"));
        System.out.println("total legal moves:" + Moves.allMoves(this, true).size());
        // System.out.println("white has moves:" + !Moves.hasNoLegalMoves(this, true));
        // System.out.println("black has moves:" + !Moves.hasNoLegalMoves(this, false));
        // System.out.println((fakeMoves == null)? "all moves:null" : "all moves:" + fakeMoves.size() + ":" + fakeMoves);
        // System.out.println((legalMoves == null)? "legal moves:null": "legal moves:" + legalMoves.size() + ":" + legalMoves);

        System.out.println();
    }
    
    public String getIllegalReason(int[] start, int[] end)
    {
        boolean whiteTurn = history.size() % 2 == 0;

        Piece p;
        try { p = getPiece(end); }
        catch (NullPointerException | ArrayIndexOutOfBoundsException e) { return "Space is out of bounds"; }
        try 
        {
            p = getPiece(start);
            if (!p.isOccupied()) return "No piece selected";
            if (p.isWhite() != whiteTurn) return "Wrong color piece";
        }
        catch (NullPointerException | ArrayIndexOutOfBoundsException e) { return "Space is out of bounds"; }
        
        Piece target = getPiece(end);
        if (target.isOccupied() && target.isWhite() == p.isWhite()) return "Space is occupied";
        
        boolean[][] pieceMoves = preValidMoves(start, true);
        if (!pieceMoves[end[0]][end[1]]) return "Piece cant travel there";
        
        Board copy = new Board(this);
        copy.applyMove(start, end, true, 'q');
        int[] king = findKing(copy, p.isWhite()); 
        if (Moves.inCheck(copy, king, p.isWhite())) return "Move leads to check";
    
        return null; // move is legal
    }
}