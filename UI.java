public class UI 
{
    public static void welcome()
    {
        System.out.println("=========================");
        System.out.println("|        Welcome!       |");
        System.out.println("| to start type 'start' |");
        System.out.println("|                       |");
        System.out.println("|    type 'help' for    |");
        System.out.println("|   a list of commands  |");
        System.out.println("=========================");
    }
    
    public static void error()
    {
        System.out.println("=========================");
        System.out.println("|  error: invaild input |");
        System.out.println("|   please try again    |");
        System.out.println("|                       |");
        System.out.println("|    type 'help' for    |");
        System.out.println("|   a list of commands  |");
        System.out.println("=========================");
    }
    
    public static void preGameColor()
    {
        System.out.println("========================================");
        System.out.println("|              !WARNING!               |");
        System.out.println("|  this was meant to be played on the  |");
        System.out.println("|  CodeHS' IDE with dark mode console  |");
        System.out.println("|                                      |");
        System.out.println("| Other IDEs/themes may not accurately |");
        System.out.println("|      display piece/board colors      |");
        System.out.println("|                                      |");
        System.out.println("|  to toggle safe colors type 'color'  |");
        System.out.println("|     otherwise enter a blank line     |");
        System.out.println("========================================");
    }
    
    public static void preGameNotation()
    {
        System.out.println("========================================");
        System.out.println("|  Moves are noted in chess notation   |");
        System.out.println("|      ex. Nc3, Qxd5, exf3 etc.        |");
        System.out.println("|                                      |");
        System.out.println("| if you wish to use expanded notation |");
        System.out.println("|         ex. b1 > c3, e2 > e4         |");
        System.out.println("|                                      |");
        System.out.println("|     to toggle expanded notation      |");
        System.out.println("|             type 'expand'            |");
        System.out.println("|     otherwise enter a blank line     |");
        System.out.println("========================================");
    }
    
    public static void help()
    {
        System.out.println("list of all commands");
        System.out.println("---------------------------");
        System.out.println("help - list of commands");
        System.out.println("start - starts game");
        System.out.println("qs - starts & skips pre game menus");
        System.out.println("dqs - starts & skips pre game menus with debug enabled");
        System.out.println("debug - toggles the debug menu");
        System.out.println();
        
        System.out.println(helpDebug());
    }
    
    public static String helpDebug()
    {
        return
        "list of all debug commands\n" +
        "---------------------------\n" +
        "history - views full game move history\n" +
        "info - views all stored information about a square\n" +
        "toggle - toggles the debug varibles from being shown\n" +
        "reset - resets the board and wipes board data\n" +
        "clear - clears all pieces but the kings off the board\n" +
        "add - adds a user difined piece to the board\n" +
        "cmds - shows all debug commands\n" +
        "eval - adds a seperate eval varabile outside of the debug menu\n" +
        "et - activates eval and toggle command at the same time\n" +
        "save history - saves the game's history to History.txt\n" +
        "\n" +
        "list of all bot commands\n" +
        "---------------------------\n" +
        "bot - lets the bot play one move for current side\n" +
        "bot depth - changes the depth the bot searches at\n" +
        "bot toggle - makes the bot to play for a selected side\n" +
        "bot game - makes the bot play both sides\n" +
        "bot time limit - changes how long the bot can think per move (in ms)";
    }
    

    // bot thinking
    public static volatile boolean stillThinking = false;

    public static Runnable thinkingAnimation = () -> {
        String[] thinkingAnimationFrames = {"\r\033[2Kbot thinking[|] ", "\r\033[2Kbot thinking[/] ", "\r\033[2Kbot thinking[-] ", "\r\033[2Kbot thinking[\\] "};
        int frameNumber = 0;
        
        while (stillThinking)
        {
            System.out.print(thinkingAnimationFrames[frameNumber % 4]);
            frameNumber++;
            try {Thread.sleep(200);} catch (Exception e) {}
        }
    };
}