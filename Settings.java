public class Settings
{
    // start options
    public static boolean useCustomStartOptions = false;

    public static boolean quickStart = true;
    public static boolean debugMenu = true;
    public static boolean safeColors = true;
    public static boolean bigNotation = false;

    // debug menu setings
        // debug bot menu toggles
        public static boolean showBotDebugMenu = true;

        public static boolean showBotRootScores = false;
        public static boolean showBotTimeLimit = true;
        public static boolean showBotMoveTime = true;
        public static boolean showBotSelfEval = true;
        public static boolean showBotDepthReached = true;
        public static boolean showBotMovesAnalysed = true;

        // debug menu toggles
        public static boolean showStandardDebugMenu = false;

        public static boolean showEval = false;
        public static boolean showTurnIndicator = false;
        public static boolean showLegalMoves = true;
        public static boolean showBlackHasValidMoves = false;
        public static boolean showWhiteHasValidMoves = false;
        public static boolean showPieceLegalMoves = false;

    // bot controls
    public static long botSearchTimeLimit = 2500;
    public static int botMaxSearchDepth = 5;
}
