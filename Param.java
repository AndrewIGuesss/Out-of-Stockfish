
public enum Param 
{
    // material
    MATERIAL(5, true),  // P 100, N 300, B 300, R 500, Q 900

    // piece square tables
    PAWN_PST_MG(64, true),
    PAWN_PST_EG(64, true),
    KNIGHT_PST(64, true),
    BISHOP_PST(64, true),
    ROOK_PST(64, true),
    QUEEN_PST(64, true),
    KING_PST_MG(64, true),
    KING_PST_EG(64, true),

    // pawn structure
    PASSED_PAWN(8, true),   // {0, 10, 15, 25, 40, 60, 90, 0} (end)
    DOUBLED_PAWN(1, true),  // 20
    ISOLATED_PAWN(1, true), // 15

    // mobility (per square)
    KNIGHT_MOBILITY(1, true),   // 4
    BISHOP_MOBILITY(1, true),   // 5
    ROOK_MOBILITY_MG(1, true),  // 2
    ROOK_MOBILITY_EG(1, true),  // 4
    QUEEN_MOBILITY_MG(1, true), // 1
    QUEEN_MOBILITY_EG(1, true), // 2

    // piece lines
    // order: own piece, enemy king, enemy queen
    BISHOP_LINES(4, true),  // 10, 20, 10, enemy pawn 8
    ROOK_FILES(4, true),    // 20, 20, 10, other enemy 10

    // misc
    EARLY_QUEEN(1, true),   // -25
    BISHOP_PAIR(1, true),   // 40

    // king safety
    KING_SHIELD(2, true),       // shield pawn = 12 / row
    KING_OPEN_FILE(2, true),    // half open -10, open -20
    CASTLING_RIGHTS(1, true),   // -30

    // not tunable
    MOP_UP(2, false);   // 10 center distance, 4 king distance

    private final int size;
    private final boolean tunable;
    private int offset;

    public static final int TOTAL_SIZE;

    static 
    {
        int runningOffset = 0;
        for (Param p : values())
        {
            p.offset = runningOffset;
            runningOffset += p.size;
        }
        TOTAL_SIZE = runningOffset;
    }

    Param(int size, boolean tunable)
    {
        this.size = size;
        this.tunable = tunable;
    }

    public int size() { return size; }
    public boolean isTunable() { return tunable; }
    public int offset() { return offset; }
}