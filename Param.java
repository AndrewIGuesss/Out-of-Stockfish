
public enum Param 
{
    // material
    MATERIAL(5, true),  // P 91, N 306, B 328, R 537, Q 981

    // piece square tables (values are in the eval tables)
    PAWN_PST_MG(64, true),
    PAWN_PST_EG(64, true),
    KNIGHT_PST(64, true),
    BISHOP_PST(64, true),
    ROOK_PST(64, true),
    QUEEN_PST(64, true),
    KING_PST_MG(64, true),
    KING_PST_EG(64, true),

    // pawn structure
    PASSED_PAWN(8, true),   // {0, 3, 8, 29, 60, 124, 126, 0} (end)
    DOUBLED_PAWN(1, true),  // 10
    ISOLATED_PAWN(1, true), // 11

    // mobility (per square)
    KNIGHT_MOBILITY(1, true),   // 2
    BISHOP_MOBILITY(1, true),   // 4
    ROOK_MOBILITY_MG(1, true),  // 3
    ROOK_MOBILITY_EG(1, true),  // 4
    QUEEN_MOBILITY_MG(1, true), // 1
    QUEEN_MOBILITY_EG(1, true), // 10

    // piece lines
    // order: own piece, enemy king, enemy queen
    BISHOP_LINES(4, true),  // 5, 51, 36, enemy pawn 7
    ROOK_FILES(4, true),    // 16, 25, 29, other enemy 9

    // misc
    EARLY_QUEEN(1, true),   // -11
    BISHOP_PAIR(1, true),   // 41

    // king safety
    KING_SHIELD(2, true),       // edge file 3, shield pawn = 8 / row
    KING_OPEN_FILE(2, true),    // half open -13, open -40
    CASTLING_RIGHTS(1, true),   // -35

    // threats
    THREAT_BY_PAWN(4, true),    // N 28, B 38, R 21, Q 35
    THREAT_BY_MINOR(2, true),   // on R 20, on Q 11
    THREAT_BY_ROOK(1, true),    // on Q 17

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