import java.util.ArrayList;
import java.util.Arrays;

public class zzTests 
{
    private static boolean runBench = false;
    private static boolean runPerft = true;
    private static boolean runFen = true;

    public static final String PASS   = "\u001B[1;32mPASS\u001B[0m";
    public static final String PASSED = "\u001B[1;32mPASSED\u001B[0m";
    public static final String FAIL   = "\u001B[1;31mFAIL\u001B[0m";
    public static final String FAILED = "\u001B[1;31mFAILED\u001B[0m";

    private static boolean passedAllTest = true;
    public static ArrayList<String[]> tests = new ArrayList<>();
    public static void main(String[] args) 
    {
        System.out.println("\n\nTesting");
        if (runBench) zzBench.runBench();
        if (runPerft) 
        {
            String perftResult = Arrays.toString(zzPerft.runPerft().toArray()).replace("[", "").replace("]", "");
            tests.add(new String[]{"perft", perftResult});
        } 
        if (runFen)
        {
            String[] fenTest = zzFen.runFen();
            tests.add(new String[]{"Fen round trip", fenTest[0]});
            tests.add(new String[]{"Fen move test", fenTest[1]});
        }

        System.out.println("\n\n-----Summary-----\n");
        System.out.println("passed   test\n----------------------");
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
