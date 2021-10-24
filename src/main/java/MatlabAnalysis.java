import com.mathworks.engine.*;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;



public class MatlabAnalysis {

    public MatlabAnalysis() throws Exception{
        MatlabEngine eng = MatlabEngine.startMatlab();
        eng.eval("results = readtable('dataFiles/ResultsCell.csv','Delimiter',',');");
        eng.eval("disp(results);");
        eng.eval("plot(results.Frame,results.Mean1); print('myPlot','-djpeg')");
        eng.close();
        System.out.println("successful matlab engine interaction");

    }

    public static void main(String args[]) throws Exception {
        MatlabAnalysis analysis = new MatlabAnalysis();
    }
}