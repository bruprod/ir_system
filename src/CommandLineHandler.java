import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Andreas.
 */
public class CommandLineHandler {
    boolean evaluate = false;
    boolean index = false;
    boolean suppress_histograms = false;
    boolean stop_after_evaluation = false;
    String path_string;
    String handler_string;
    String histogram_prefix;
    int number_evals = 1;

    public boolean isEvaluate() {
        return evaluate;
    }

    public boolean isIndex() {
        return index;
    }

    /**
     * Handler for the commandline flags. The Flags and there usage can be shown with the --help flag.
     *The following flags can be set:<br>
     *--evaluate | this will start the evaluation directly with the given index<br>
     *--index "Path to XML-File" "Handler" | this flag will index a new XML-File,
     * the Handle can be Stackexchange or Wikipedia"<br>
     * --histogram-prefix "Prefix" | will add an prefix to the histogram files<br>
     * --suppress-histograms | suppresses the display of the evaluation histograms<br>
     * --stop-after-evaluation | kills the program after evaluation is done<br>
     * --evaluation-runs "Number of Evaluation invocations" | defines how often the evaluation will be invoked<br>
     * @param args The Array of Strings containing the commandline arguments
     */
    public CommandLineHandler(String[] args){
        ArrayList<String> arguments = new ArrayList<>(Arrays.asList(args));
        if(arguments.contains("--evaluate")){
            evaluate = true;
            System.out.println("Evaluate flag set!");
        }
        if(arguments.contains("--help")){
            System.out.println("The following flags can be set: ");
            System.out.println("--evaluate | this will start the evaluation directly with the given index");
            System.out.println("--index <Path to XML-File> <Handler> | this flag will index a new XML-File," +
                    " the Handle can be Stackexchange or Wikipedia");
            System.out.println("--histogram-prefix <Prefix> | will add an prefix to the histogram files");
            System.out.println("--suppress-histograms | suppresses the display of the evaluation histograms");
            System.out.println("--stop-after-evaluation | kills the program after evaluation is done");
            System.out.println("--evaluation-runs <Number of Evaluation invocations> | " +
                    "defines how often the evaluation will be invoked");
            System.exit(0);
        }
        path_string = new String();
        handler_string = new String();
        if(arguments.contains("--index")){
            int index_of_flag = arguments.indexOf("--index");
            path_string = arguments.get(index_of_flag+1);
            handler_string = arguments.get(index_of_flag+2);

            if(path_string.contains(".xml") && (handler_string.equals("Stackexchange") || handler_string.equals("Wikipedia")))
                index = true;
            else {
                System.err.println("Not a XML-File or False XML-Handler name!");
                System.exit(1);
            }
        }
        histogram_prefix = new String();
        if(arguments.contains("--histogram-prefix")){
            int index_of_flag = arguments.indexOf("--histogram-prefix");
            histogram_prefix = arguments.get(index_of_flag+1);
        }

        if(arguments.contains("--suppress-histograms")){
            System.out.println("Suppress Histograms flag set!");
            suppress_histograms = true;
        }

        if(arguments.contains("--stop-after-evaluation")){
            System.out.println("Stop after evaluation flag set");
            stop_after_evaluation = true;
        }
        if(arguments.contains("--evaluation-runs")){

            int index_of_flag = arguments.indexOf("--evaluation-runs");
            try {
                number_evals = Integer.parseInt(arguments.get(index_of_flag + 1));
                System.out.println("Evaluation Runs: " + number_evals);
            }catch (NumberFormatException e){
                System.err.println("Evaluation runs argument is not a number!");
            }

        }
    }

    public String getHandlerString() {
        return handler_string;
    }

    public String getPathString() {
        return path_string;
    }

    public String getHistogramPrefix() {
        return histogram_prefix;
    }

    public boolean isSuppressHistograms() {
        return suppress_histograms;
    }

    public boolean isStopAfterEvaluation() {
        return stop_after_evaluation;
    }

    public int getNumberEvals() {
        return number_evals;
    }
}
