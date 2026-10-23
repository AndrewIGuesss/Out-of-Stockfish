public enum EvalTerm 
{
    MATERIAL("material"), 
    PIECE_SQUARES("piece squares"), 
    PIECE_MOBILITY("piece mobility"),
    PASSED_PAWNS("passed pawns"), 
    DOUBLED_PAWNS("doubled pawns"), 
    ISOLATED_PAWNS("isolated pawns"), 
    BISHOP_LINES("bishop lines"), 
    ROOK_FILES("rook files"), 
    EARLY_QUEEN("early queen"), 
    BISHOP_PAIR("bishop pair"), 
    KING_SHIELD("king shield"), 
    KING_OPEN_FILES("open king"),
    KING_SQUARE ("king square"), 
    CASTLING_RIGHTS("castling rights"), 
    ENDGAME_KING("endgame king"), 
    MOP_UP("mop up");

    private String displayName;
    public static final int SIZE = values().length;

    EvalTerm(String displayName)
    {
        this.displayName = displayName;
    }
    
    public String getName() { return displayName; }
}
