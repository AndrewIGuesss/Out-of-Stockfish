public enum Piece 
{
    W_PAWN(true, 'P', 100),
    W_KNIGHT(true, 'N', 300),
    W_BISHOP(true, 'B', 300),
    W_ROOK(true, 'R', 500),
    W_QUEEN(true, 'Q', 900),
    W_KING(true, 'K', 1000),

    B_PAWN(false, 'P', 100),
    B_KNIGHT(false, 'N', 300),
    B_BISHOP(false, 'B', 300),
    B_ROOK(false, 'R', 500),
    B_QUEEN(false, 'Q', 900),
    B_KING(false, 'K', 1000),

    EMPTY(false, ' ', -1);
    
    private boolean white;
    private char piece;
    private int value;
    
    Piece(boolean white, char piece, int value)
    {
        this.white = white;
        this.piece = piece;
        this.value = value;
    }
    
    // geters
    public boolean isWhite() {return white;}
    public char getPiece() {return piece;}
    public int value() { return value; }
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