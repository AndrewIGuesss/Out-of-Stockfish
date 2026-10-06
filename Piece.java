public enum Piece 
{
    EMPTY(false, ' ', -1),

    W_PAWN(true, 'P', 0),
    W_KNIGHT(true, 'N', 1),
    W_BISHOP(true, 'B', 2),
    W_ROOK(true, 'R', 3),
    W_QUEEN(true, 'Q', 4),
    W_KING(true, 'K', 5),

    B_PAWN(false, 'P', 6),
    B_KNIGHT(false, 'N', 7),
    B_BISHOP(false, 'B', 8),
    B_ROOK(false, 'R', 9),
    B_QUEEN(false, 'Q', 10),
    B_KING(false, 'K', 11);
    
    private boolean white;
    private char piece;
    private int index;
    
    Piece(boolean white, char piece, int index)
    {
        this.white = white;
        this.piece = piece;
        this.index = index;
    }
    
    // geters
    public boolean isWhite() {return white;}
    public char getPiece() {return piece;}
    public int pieceToIndex() { return index; }
    public boolean isOccupied() { return this != EMPTY; }


    public static Piece get(boolean white, char piece)
    {
        piece = Character.toUpperCase(piece);
        if (white)
        {
            switch (piece) 
            {
                case 'P': { return W_PAWN; } 
                case 'N': { return W_KNIGHT; } 
                case 'B': { return W_BISHOP; } 
                case 'R': { return W_ROOK; } 
                case 'Q': { return W_QUEEN; } 
                case 'K': { return W_KING; } 
            }
        }
        else
        {
            switch (piece) 
            {
                case 'P': { return B_PAWN; } 
                case 'N': { return B_KNIGHT; } 
                case 'B': { return B_BISHOP; } 
                case 'R': { return B_ROOK; } 
                case 'Q': { return B_QUEEN; } 
                case 'K': { return B_KING; } 
            }
        }
        return EMPTY;
    }

    public String toIcon()
    {
        switch (getPiece())
        {
            case 'K': return "\u2654";
            case 'Q': return "\u2655";
            case 'R': return "\u2656";
            case 'B': return "\u2657";
            case 'N': return "\u2658";
            case 'P': return "\u2659";
        }
        return "";
    }
}