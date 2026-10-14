import java.util.*;
public class Board 
{
    private Piece[][] playerBoard; // board that holds all pieces
    private List<String> history; // all moves on board
    private List<Piece> whiteCaptured; // pieces white has taken
    private List<Piece> blackCaptured; // pieces black has taken
    
    private int[] enPassantSquare; // for en passant captures

    private boolean bigNotation = Settings.bigNotation;

    private int moveOffSet = 1;
    private boolean blackStarted = false;
    
    // 50 move rule
    private int timeSinceCaptureOrPawnPush;

    // debug vars
    private List<String> fakeMoves;
    private List<String> legalMoves;

    private int[] whiteKingLoc = new int[2];
    private int[] blackKingLoc = new int[2];

    // castling rights
    public boolean whiteCastled = false;
    public boolean whiteCanCastleLong = true;
    public boolean whiteCanCastleShort = true;

    public boolean blackCastled = false;
    public boolean blackCanCastleLong = true;
    public boolean blackCanCastleShort = true;

    public Board()
    {
        blackCaptured = new ArrayList<Piece>();
        whiteCaptured = new ArrayList<Piece>();
        history = new ArrayList<String>();
        enPassantSquare = null;
        
        playerBoard = new Piece[8][8];
        difine();
    }
    
    // makes a duped board to not alter the real one
    public Board(Board board)
    {
        playerBoard = new Piece[8][8];
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                playerBoard[row][col] = board.getPiece(row, col);
            }
        }
        timeSinceCaptureOrPawnPush = board.getTimeSinceCaptureOrPawnPush();

        whiteCastled = board.whiteCastled;
        whiteCanCastleLong = board.whiteCanCastleLong;
        whiteCanCastleShort = board.whiteCanCastleShort;
        
        blackCastled = board.blackCastled;
        blackCanCastleLong = board.blackCanCastleLong;
        blackCanCastleShort = board.blackCanCastleShort;

        whiteKingLoc = board.getWhiteKingLoc().clone();
        blackKingLoc = board.getBlackKingLoc().clone();

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
    public boolean getBlackStarted() { return blackStarted; }
    public List<String> getHistory() { return history; }
    public int[] getEnPassantSquare() { return enPassantSquare; }
    public boolean inBounds(int row, int col) { return row >= 0 && row < 8 && col >= 0 && col < 8;}
    public int getTimeSinceCaptureOrPawnPush() { return timeSinceCaptureOrPawnPush; }
    public int getMoveOffSet() { return moveOffSet; }
    public int[] getBlackKingLoc() { return blackKingLoc; }
    public int[] getWhiteKingLoc() { return whiteKingLoc; }
    public int[] getKingLoc(boolean isWhite) { return isWhite? whiteKingLoc : blackKingLoc; }
    public List<Piece> getCaptures(boolean isWhite) {return isWhite? whiteCaptured : blackCaptured;}
    
    // seters
    public void addHistory(String s) {history.add(s);}
    public void setHistory(List<String> history) {this.history = history;}
    public void setEnPassantSquare(int[] cords) {enPassantSquare = cords;}
    public void setBlackStarted(boolean blackStarted) {this.blackStarted = blackStarted;}

    // debug geters
    public List<String> getFakeMoves() { return fakeMoves; };
    public List<String> getLegalMoves() { return legalMoves; };
    
    // debug seters
    public void addPiece(char piece, int[] cords, boolean isWhite) 
    { playerBoard[cords[0]][cords[1]] = Piece.get(isWhite, piece); }
    public void addPiece(int[] cords)
    { playerBoard[cords[0]][cords[1]] = Piece.EMPTY; }
    
    public void difine()
    {
        // fill with empty squares
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                playerBoard[row][col] = Piece.EMPTY;
            }
        }
    
        // pawns
        for (int col = 0; col < 8; col++)
        {
            playerBoard[1][col] = Piece.get(false, 'P');
            playerBoard[6][col] = Piece.get(true, 'P');
        }
    
        // back
        char[] piecesOrder = {'R','N','B','Q','K','B','N','R'};
        
        for (int col = 0; col < 8; col++)
        {
            playerBoard[0][col] = Piece.get(false, piecesOrder[col]);
            playerBoard[7][col] = Piece.get(true, piecesOrder[col]);
        }

        updateKingLocs();
    }

    public void updateKingLocs()
    {
        int kingsFound = 0;
        kingSearch: for (int row = 0; row < 8; row++) // finds king
        {
            for (int col = 0; col < 8; col++)
            {
                if (this.getPiece(row, col).getPiece() == 'K')
                {       
                    if (this.getPiece(row, col).isWhite()) whiteKingLoc = new int[]{row, col};
                    else blackKingLoc = new int[]{row, col};
                    kingsFound++;
                    if (kingsFound == 2) break kingSearch;
                }
            }
        }
    }

    public void updateCastlingRights()
    {
        if (playerBoard[0][4] != Piece.B_KING) { blackCanCastleLong = false; blackCanCastleShort = false;}
        else 
        {
            if (playerBoard[0][0] != Piece.B_ROOK) blackCanCastleLong = false;
            if (playerBoard[0][7] != Piece.B_ROOK) blackCanCastleShort = false;
        }
        if (playerBoard[7][4] != Piece.W_KING) { whiteCanCastleLong = false; whiteCanCastleShort = false;}
        else 
        {
            if (playerBoard[7][0] != Piece.W_ROOK) whiteCanCastleLong = false;
            if (playerBoard[7][7] != Piece.W_ROOK) whiteCanCastleShort = false;
        }
    }

    public boolean isWhiteTurn()
    {
        int historySize = history.size() + (blackStarted? 1 : 0);
        if (historySize % 2 == 0) return true;
        else return false;
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
    
                    int[] kingSquare = copy.getKingLoc(white); // finds king
                    if (!Moves.inCheck(copy, kingSquare, white)) legal[row][col] = true; // checks if king would be in check
                }
            }
        }
    
        // debug
        if (!Settings.debugMenu || test) return legal;
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
        if (!Settings.debugMenu || test) return preValidMoves;
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
        promotionPiece = Character.toUpperCase(promotionPiece);
        StringBuilder move = new StringBuilder();
        
        // makes temp piece and adds to its history
        Piece temp = playerBoard[start[0]][start[1]];

        if (getPiece(end).isOccupied() || getPiece(start).getPiece() == 'P') timeSinceCaptureOrPawnPush = 0;
        else timeSinceCaptureOrPawnPush++;

        if (temp.getPiece() == 'K') 
        {
            if (temp.isWhite()) whiteKingLoc = new int[]{end[0], end[1]};
            else blackKingLoc = new int[]{end[0], end[1]};
        }

        if (temp.getPiece() != 'P' && !test)
        {
            move.append(temp.getPiece());

            boolean disambiguateCol = false;
            boolean disambiguateRow = false;

            List<int[]> rivals = new ArrayList<int[]>();
            List<int[]> pMoves = Moves.pseudoMoves(this, temp.isWhite());
            for (int[] pMove : pMoves)
            {
                if ((pMove[2] == end[0] && pMove[3] == end[1]) && (pMove[0] != start[0] || pMove[1] != start[1]))
                {
                    if (temp.getPiece() == getPiece(pMove[0], pMove[1]).getPiece())
                    {
                        boolean[][] pValidMoves = validMoves(new int[]{pMove[0], pMove[1]}, true);
                        if (pValidMoves[pMove[2]][pMove[3]]) rivals.add(pMove);
                    }
                }
            }
            for (int[] rival : rivals)
            {
                if (rival[0] == start[0]) disambiguateRow = true;
                else if (rival[1] == start[1]) disambiguateCol = true;
            }
            if (rivals.size() > 0 && !disambiguateCol) disambiguateRow = true;
            if (disambiguateRow) move.append(convertMove(start).charAt(0));
            if (disambiguateCol) move.append(convertMove(start).charAt(1));
        }

        playerBoard[start[0]][start[1]] = Piece.EMPTY; //clears the space


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
                playerBoard[start[0]][0] = Piece.EMPTY;
                move = new StringBuilder("O-O-O");
            }
            else // short castle
            {
                playerBoard[start[0]][5] = playerBoard[start[0]][7];
                playerBoard[start[0]][7] = Piece.EMPTY;
                move = new StringBuilder("O-O");
            }
            if (temp.isWhite()) whiteCastled = true;
            else blackCastled = true;
        }
        if (start[0] == 7 && start[1] == 4 || end[0] == 7 && end[1] == 4) { whiteCanCastleLong = false; whiteCanCastleShort = false; }
        if (start[0] == 7 && start[1] == 0 || end[0] == 7 && end[1] == 0) whiteCanCastleLong = false;
        if (start[0] == 7 && start[1] == 7 || end[0] == 7 && end[1] == 7) whiteCanCastleShort = false;

        if (start[0] == 0 && start[1] == 4 || end[0] == 0 && end[1] == 4) { blackCanCastleLong = false; blackCanCastleShort = false; }
        if (start[0] == 0 && start[1] == 0 || end[0] == 0 && end[1] == 0) blackCanCastleLong = false;
        if (start[0] == 0 && start[1] == 7 || end[0] == 0 && end[1] == 7) blackCanCastleShort = false;
        

        // en passant
        if ((enPassantSquare != null && end[0] == enPassantSquare[0] && end[1] == enPassantSquare[1] && temp.getPiece() == 'P'))
        {
            if (temp.isWhite())
            {
                whiteCaptured.add(playerBoard[end[0] + 1][end[1]]);
                playerBoard[end[0] + 1][end[1]] = Piece.EMPTY; // removes pawn before square
            }
            else
            {
                blackCaptured.add(playerBoard[end[0] - 1][end[1]]);
                playerBoard[end[0] - 1][end[1]] = Piece.EMPTY; // removes pawn before square
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
        
        
         // converts int[] to proper notation (e4, d5, etc.)
        if (temp.getPiece() != 'K' || (start[1] - end[1] != 2 && start[1] - end[1] != -2)) move.append(convertMove(end));
        
        // promotion check
        int promotionRow = temp.isWhite()? 0 : 7;
        
        if (temp.getPiece() == 'P' && end[0] == promotionRow)
        {
            if ( promotionPiece == 'Z') promotionPiece = 'Q';
            temp = Piece.get(temp.isWhite(), promotionPiece);
            move.append("=" + promotionPiece);
        }

        // finishing move notation
        
        playerBoard[end[0]][end[1]] = temp;

        if(!test && Moves.inCheck(this, this.getKingLoc(!temp.isWhite()), !temp.isWhite())) // check notation
        {
            if (Moves.hasNoLegalMoves(this, !temp.isWhite())) move.append("#"); // checkmate
            else move.append("+"); // check
        }
        
        if (!test) 
        {
            history.add((bigNotation)? convertMove(start) + " > " + convertMove(end) : move.toString() ); // standard chess notation and square to square
        }
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

    public String getIllegalReason(int[] start, int[] end)
    {
        boolean whiteTurn = isWhiteTurn();

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
        int[] king = copy.getKingLoc(p.isWhite());
        if (Moves.inCheck(copy, king, p.isWhite())) return "Move leads to check";
    
        return null; // move is legal
    }

    public int getBoardValue()
    {
        int value = 0;
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                int pieceValue = getPiece(row, col).value() / 100;
                if (getPiece(row, col).isWhite()) value += pieceValue;
                else value -= pieceValue;
            }
        }
        return value;
    }

    public int getBoardValue(boolean isWhite)
    {
        int value = 0;
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                if (getPiece(row, col).isOccupied() && getPiece(row, col).isWhite() == isWhite)
                {
                    value += getPiece(row, col).value() / 100;
                }
            }
        }
        return value;
    }

    public boolean hasNonPawnMaterial(boolean isWhite)
    {
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                int currValue = getPiece(row, col).value();
                if (currValue > 100 && currValue < 1000 && getPiece(row, col).isWhite() == isWhite) return true;
            }
        }
        return false;
    }

    public static boolean isInsufficientMaterial(Board board)
    {
        int blackMinorPieceCount = 0;
        int whiteMinorPieceCount = 0;

        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                switch (board.getPiece(row, col).getPiece()) 
                {
                    case 'Q': return false;
                    case 'R': return false;
                    case 'P': return false;

                    case 'N': 
                    { 
                        if (board.getPiece(row, col).isWhite()) whiteMinorPieceCount++;
                        else blackMinorPieceCount++;
                        break;
                    }
                    case 'B':
                    { 
                        if (board.getPiece(row, col).isWhite()) whiteMinorPieceCount++;
                        else blackMinorPieceCount++;
                        break;
                    }
                }
                if (blackMinorPieceCount > 1 || whiteMinorPieceCount > 1) return false;
            }
        }
        return true;
    }

    public boolean importFEN (String fen)
    {
        whiteCastled = false;
        blackCastled = false;
        blackStarted = false;

        boolean whiteTurn;
        String[] preSplitFEN = fen.split(" ");
        String[] splitFEN = new String[6];

        for (int i = 0; i < 6; i++)
        {
            if (preSplitFEN.length > i) splitFEN[i] = preSplitFEN[i];
            else
            {
                switch (i) 
                {
                    case 0: splitFEN[0] = fen;
                    case 1: splitFEN[1] = "w";
                    case 2: splitFEN[2] = "-";
                    case 3: splitFEN[3] = "-";
                    case 4: splitFEN[4] = "0";
                    case 5: splitFEN[5] = "1";
                }
                break;
            }
        }
        int col = 0;
        int row = 0;

        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                addPiece(new int[]{i, j});
            }
        }

        // Piece placement
        for (char currChar : splitFEN[0].toCharArray())
        {
            if (currChar == '/') {row++; col = 0;}
            else if (Character.isDigit(currChar))
            {
                col += currChar - '0';
            }
            else 
            {
                addPiece(Character.toUpperCase(currChar), new int[]{row, col}, Character.isUpperCase(currChar));
                col++;
            }
        }

        // Side to move
        if (splitFEN[1].equals("w")) whiteTurn = true;
        else whiteTurn = false;
        if (!whiteTurn) blackStarted = true;

        // Castling rights
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                if (getPiece(i, j).getPiece() == 'K')
                {
                         if ((i == 7 && j == 2) || (i == 7 && j == 6)) whiteCastled = true;
                    else if ((i == 0 && j == 2) || (i == 0 && j == 6)) blackCastled = true;
                }
            }
        }

        whiteCanCastleShort = false;
        whiteCanCastleLong = false;
        blackCanCastleShort = false;
        blackCanCastleLong = false;

        for (char currChar : splitFEN[2].toCharArray())
        {
            switch (currChar)
            {
                case 'K': whiteCanCastleShort = true; break;
                case 'Q': whiteCanCastleLong = true; break;
                case 'k': blackCanCastleShort = true; break;
                case 'q': blackCanCastleLong = true; break;
            }
        }

        // En passant
        if (splitFEN[3].equals("-")) setEnPassantSquare(null);
        else setEnPassantSquare(convertMove(splitFEN[3]));

        // 50 move rule
        timeSinceCaptureOrPawnPush = Integer.parseInt(splitFEN[4]);

        // move offset / full moves
        moveOffSet = Integer.parseInt(splitFEN[5]);

        updateCastlingRights();
        updateKingLocs();

        return whiteTurn;
    }

    public String makeFEN(boolean whiteTurn)
    {
        StringBuilder fen = new StringBuilder();
        
        // Piece placement
        int emptySquareCounter = 0;
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                if(!getPiece(i, j).isOccupied()) emptySquareCounter++;
                else
                {
                    if(emptySquareCounter > 0) { fen.append(emptySquareCounter); emptySquareCounter = 0; }
                    if (getPiece(i, j).isWhite()) fen.append(Character.toUpperCase(getPiece(i, j).getPiece()));
                    else fen.append(Character.toLowerCase(getPiece(i, j).getPiece()));
                }
            }
            if(emptySquareCounter > 0) fen.append(emptySquareCounter);
            emptySquareCounter = 0;
            if (i + 1 < 8) fen.append("/");
        }
        fen.append(" ");

        // Side to move
        if(whiteTurn) fen.append("w");
        else fen.append("b");
        fen.append(" ");

        // Castling rights
        boolean someoneCanCastle = false;

        if (whiteCanCastleShort) { fen.append("K"); someoneCanCastle = true; }
        if (whiteCanCastleLong) { fen.append("Q"); someoneCanCastle = true; }
        if (blackCanCastleShort) { fen.append("k"); someoneCanCastle = true; }
        if (blackCanCastleLong) { fen.append("q"); someoneCanCastle = true; }

        if (!someoneCanCastle) fen.append("-");
        fen.append(" ");

        // En passant
        if(enPassantSquare == null) fen.append("-");
        else fen.append(convertMove(enPassantSquare));
        fen.append(" ");

        // 50 move rule
        fen.append(timeSinceCaptureOrPawnPush);
        fen.append(" ");

        // move offset / full moves
        fen.append(((history.size() + (blackStarted? 1 : 0)) / 2) + moveOffSet);

        return fen.toString();
    }
}