import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class zTests 
{
    private static boolean runAll = false;

    private static boolean runBench = true;
    private static boolean runPerft = false;
    private static boolean runFen = true;
    private static boolean runMirror = true;
    private static boolean runMakeUnmake = true;

    public static final String PASS   = "\u001B[1;32mPASS\u001B[0m";
    public static final String PASSED = "\u001B[1;32mPASSED\u001B[0m";
    public static final String FAIL   = "\u001B[1;31mFAIL\u001B[0m";
    public static final String FAILED = "\u001B[1;31mFAILED\u001B[0m";

    private static boolean passedAllTest = true;
    public static List<String[]> tests = new ArrayList<>();
    public static void main(String[] args) 
    {
        long benchNodes = 0;
        System.out.println("\n\nTesting");
        if (runBench || runAll) benchNodes = zzBench.runBench();
        if (runPerft || runAll) 
        {
            String perftResult = Arrays.toString(zzPerft.runPerft().toArray()).replace("[", "").replace("]", "");
            tests.add(new String[]{"perft", perftResult});
        } 
        if (runFen || runAll)
        {
            String[] fenTest = zzFen.runFen();
            tests.add(new String[]{"Fen round trip", fenTest[0]});
            tests.add(new String[]{"Fen move test", fenTest[1]});
        }
        if (runMirror || runAll)
        {
            String mirrorResult = Arrays.toString(zzMirror.runMirror().toArray()).replace("[", "").replace("]", "");
            tests.add(new String[]{"Mirror", mirrorResult});
        }
        if (runMakeUnmake || runAll)
        {
            String makeUnmakeResult = Arrays.toString(zzMakeUnmake.runMakeUnmake().toArray()).replace("[", "").replace("]", "");
            tests.add(new String[]{"Make/unmake", makeUnmakeResult});
        }
        System.out.println("\n\n-----Summary-----");
        if (runBench || runAll) System.out.printf("bench nodes: %s%n", benchNodes);
        for (String[] test : tests)
        {
            String testMsg = PASSED;
            if (!test[1].isEmpty()) { testMsg = test[1]; passedAllTest = false;}
            if (testMsg.equals(PASSED)) System.out.printf("%s | %s%n", testMsg, test[0]);
            else System.out.printf("%s | %s%n", testMsg, test[0]);
        }
        System.out.println("\n" + (passedAllTest? PASSED : FAILED) + " tests");
    }
}
