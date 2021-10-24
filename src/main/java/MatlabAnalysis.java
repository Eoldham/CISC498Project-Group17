import com.mathworks.engine.*;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;



public class MatlabAnalysis {

    public MatlabAnalysis(){
        System.out.println("hello");
    }

    public static void main(String args[]) throws Exception {
        MatlabEngine eng = MatlabEngine.startMatlab();
        eng.evalAsync("[X, Y] = meshgrid(-2:0.2:2);");
        eng.evalAsync("Z = X .* exp(-X.^2 - Y.^2);");
        Object[] Z = eng.getVariable("Z");
        eng.close();
        System.out.println("success");

    }
}