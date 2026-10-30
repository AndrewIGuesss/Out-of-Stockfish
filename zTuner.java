import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class zTuner 
{
    private static List<Board> boards = new ArrayList<>();
    private static List<Double> results = new ArrayList<>();
    private static String savePath; // tuned.txt next to the dataset
    public static void main(String[] args) throws IOException
    {
        load(args[0]);
        savePath = new File(args[0]).getAbsoluteFile().getParent() + File.separator + "tuned.txt";
        double k = findK();
        tune(k);
    }

    private static void tune(double k) throws IOException
    {
        boolean improved = true;
        double bestError = error(k);
        int round = 0;

        while (improved)
        {
            round++;
            improved = false;
            for (Param p : Param.values())
            {

                if (p.isTunable())
                {
                    for (int i = 0; i < p.size(); i++)
                    {
                        Eval.params[p.offset() + i] += 1;
                        double newError = error(k);
                        if (newError < bestError) 
                        {
                            improved = true;
                            bestError = newError;
                        }
                        else
                        {
                            Eval.params[p.offset() + i] -= 2;
                            newError = error(k);
                            if (newError < bestError) 
                            {
                                improved = true;
                                bestError = newError;
                            }
                            else Eval.params[p.offset() + i] += 1;
                        }
                    }
                }
            }
            System.out.println(round + ": " + bestError);
            save(savePath, round, bestError);
        }
    }

    // writes every param grouped by Param, 64 size tables as 8 rows so they paste straight into a table
    private static void save(String path, int round, double error) throws IOException
    {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path)))
        {
            writer.println("// round " + round + ", error " + error);
            for (Param p : Param.values())
            {
                if (p.size() == 64)
                {
                    writer.println(p + ":");
                    for (int row = 0; row < 8; row++)
                    {
                        StringBuilder line = new StringBuilder("    {");
                        for (int col = 0; col < 8; col++)
                        {
                            line.append(String.format("%4d", Eval.params[p.offset() + row * 8 + col]));
                            if (col < 7) line.append(",");
                        }
                        line.append(row < 7? "}," : "}");
                        writer.println(line);
                    }
                }
                else
                {
                    StringBuilder line = new StringBuilder(p + ": ");
                    for (int i = 0; i < p.size(); i++)
                    {
                        line.append(Eval.params[p.offset() + i]);
                        if (i < p.size() - 1) line.append(", ");
                    }
                    writer.println(line);
                }
            }
        }
        System.out.println("saved to " + path);
    }

    private static void load(String path) throws IOException
    {
        int lines = 0;
        int win = 0;
        int loss = 0;
        int draw = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(path)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                String fen = line.substring(0, line.indexOf(" c9 "));
                Board b = new Board();
                b.importFEN(fen);
                boards.add(b);

                lines++;
                if (line.contains("1-0")) 
                {
                    win++;
                    results.add(1.0);
                }
                if (line.contains("0-1")) 
                {
                    loss++;
                    results.add(0.0);
                }
                if (line.contains("1/2-1/2")) 
                {
                    draw++;
                    results.add(0.5);
                }

            }
            System.out.printf("%d lines | wins: %d | losses: %d | draws: %d%n", lines, win, loss, draw);
        }
    }

    private static double error(double k)
    {
        double errorTotal = 0;
        for (int i = 0; i < boards.size(); i++)
        {
            int eval = Eval.evalBoard(boards.get(i), false);
            double predicted = 1.0 / (1.0 + Math.pow(10, -k * eval / 400.0));
            double diff = results.get(i) - predicted;
            errorTotal += diff * diff;
        }
        return errorTotal / boards.size();
    }

    private static double findK()
    {
        double lowest = Double.MAX_VALUE;
        double k = 0.0;
        for (double i = 5; i <= 20; i += 1)
        {
            double error = error(i / 10);
            if (error < lowest) 
            {
                k = i / 10;
                lowest = error;
            }
        }

        double newLowest = Double.MAX_VALUE;
        double newK = 0.0;
        for (double i = k - 0.1; i < k + 0.1; i += 0.01)
        {
            double error = error(i);
            if (error < newLowest) 
            {
                newK = i;
                newLowest = error;
            }
        }
        
        return newK;
    }
}
