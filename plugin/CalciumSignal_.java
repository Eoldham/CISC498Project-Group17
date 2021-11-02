import celldetection._3D_objects_counter;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import imageJ.plugins.PoorMan3DReg_;


public class CalciumSignal_ implements PlugIn {

    public void run(String arg) {
        IJ.showMessage("Calcium Signal", "Welcome to the Calcium Signal plugin!");

        int[] idList = WindowManager.getIDList();
        PoorMan3DReg_ reg = new PoorMan3DReg_();
        _3D_objects_counter counter = new _3D_objects_counter();

        for (int id : idList) {
            ImagePlus img = WindowManager.getImage(id);
            WindowManager.setTempCurrentImage(img);
            reg.run(arg);
            counter.run(arg);
        }

    }

    public static void main(String[] args) {
        // sanity check
        System.out.println("Hello, world!");
    }
}
