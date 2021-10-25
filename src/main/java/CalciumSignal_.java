import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import imageJ.plugins.PoorMan3DReg_;

public class CalciumSignal_ implements PlugIn {

    public void run(String arg) {
        IJ.showMessage("Calcium Signal", "Welcome to the Calcium Signal plugin!");

        PoorMan3DReg_ reg = new PoorMan3DReg_();
        reg.run(arg);

        ImagePlus imp = WindowManager.getCurrentImage();
    }

    public static void main(String[] args) {
        // sanity check
        System.out.println("Hello, world!");
    }
}
