
public enum Param 
{
    // material
    MATERIAL(5, false),  // P 91, N 306, B 328, R 537, Q 981

    // piece square tables (values are in the eval tables)
    PAWN_PST_MG(64, false),
    PAWN_PST_EG(64, false),
    KNIGHT_PST(64, false),
    BISHOP_PST(64, false),
    ROOK_PST(64, true),
    QUEEN_PST(64, false),
    KING_PST_MG(64, false),
    KING_PST_EG(64, false),

    // pawn structure
    PASSED_PAWN(8, false),   // {0, 3, 8, 29, 60, 124, 126, 0}
    DOUBLED_PAWN(1, false),  // 10
    ISOLATED_PAWN(1, false), // 11

    // mobility (per square)
    KNIGHT_MOBILITY(1, false),   // 2
    BISHOP_MOBILITY(1, false),   // 4
    ROOK_MOBILITY_MG(1, true),  // 2
    ROOK_MOBILITY_EG(1, true),  // 3
    QUEEN_MOBILITY_MG(1, false), // 1
    QUEEN_MOBILITY_EG(1, false), // 10

    // piece lines
    // order: own piece, enemy king, enemy queen
    BISHOP_LINES(4, false),      // 5, 51, 36, enemy pawn 7
    ROOK_FILES(4, false),        // 16, 25, 29, other enemy 9
    ROOK_OPEN_FILE(2, true),    // open 13, semi-open 11
    ROOK_7TH_PAWN(2, true),     // mg -13, eg 11 (per pawn)
    ROOK_7TH_KING(2, true),     // mg 16, eg 16

    // misc
    EARLY_QUEEN(1, false),   // -11
    BISHOP_PAIR(1, false),   // 41

    // king safety
    KING_SHIELD(2, false),       // edge file 3, shield pawn = 8 / row
    KING_OPEN_FILE(2, false),    // half open -13, open -40
    CASTLING_RIGHTS(1, false),   // -35

    // threats
    THREAT_BY_PAWN(4, false),    // N 28, B 38, R 21, Q 35
    THREAT_BY_MINOR(2, false),   // on R 20, on Q 11
    THREAT_BY_ROOK(1, false),    // on Q 17

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