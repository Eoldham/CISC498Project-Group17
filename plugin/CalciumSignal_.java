import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import imageJ.plugins.PoorMan3DReg_;

public class CalciumSignal_ implements PlugIn {

    public void run(String arg) {
        IJ.showMessage("Calcium Signal", "Welcome to the Calcium Signal plugin!");

        // int imageCount = WindowManager.getImageCount();
        int[] idList = WindowManager.getIDList();
        PoorMan3DReg_ reg = new PoorMan3DReg_();

        for (int id : idList) {
            ImagePlus img = WindowManager.getImage(id);
            WindowManager.setTempCurrentImage(img);
            reg.run(arg);
        }

        ImagePlus imp = WindowManager.getCurrentImage();
    }

    public static void main(String[] args) {
        // sanity check
        System.out.println("Hello, world!");
    }
}
