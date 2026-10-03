public class Settings
{
    // misc settings
    public static boolean highlightPreviousMove = true;
    public static boolean usePieceIcons = false; // uglier but little bit easier to read
    public static boolean blackOnBottom = false;
    public static boolean showBotStatsInHistory = true;

    // history/PGN vars
    public static String eventPGN = "bot long test";
    public static boolean restartTimerOnBotGame = true;
    public static boolean addSavedFenInHistory = true;

    // start options
    public static boolean useCustomStartOptions = true;

    public static boolean enableDebug = true;
    
    public static boolean debugMenu = true;
    public static boolean quickStart = true;
    public static boolean safeColors = false; // terminal.integrated.minimumContrastRatio 1
    public static boolean bigNotation = false;

    // debug menu setings
        // debug bot menu toggles
        public static boolean showBotDebugMenu = true;

        public static boolean showBotRootScores = false;
        public static boolean showBotTimeLimit = false;
        public static boolean showBotMoveTime = true;
        public static boolean showBotSelfEval = true;
        public static boolean showBotDepthReached = true;
        public static boolean showBotMovesAnalysed = true;

        // debug menu toggles
        public static boolean showStandardDebugMenu = true;

        public static boolean showEval = false;
        public static boolean showTurnIndicator = false;
        public static boolean showLegalMoves = true;
        public static boolean showGameLength = true;
        public static boolean showBlackHasValidMoves = false;
        public static boolean showWhiteHasValidMoves = false;
        public static boolean showPieceLegalMoves = false;

    // bot controls
    public static long botSearchTimeLimit = 2500;
    public static int botMaxSearchDepth = 8;
}
