import celldetection._3D_objects_counter;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import imageJ.plugins.PoorMan3DReg_;

import java.io.*;


public class CalciumSignal_ implements PlugIn {

    public void run(String arg) {
        /*
        System.getProperties().setProperty("plugins.dir", System.getProperty("user.dir")+File.separator+"dist"+ File.separator);
        ImageJ ij=new ImageJ();

        ij.exitWhenQuitting(true);

         */

        IJ.showMessage("Calcium Signal", "Welcome to the Calcium Signal plugin!");

        try {
            // RELATIVE TO LOCATION OF FIJI EXECUTABLE
            ProcessBuilder processBuilder = new ProcessBuilder("python", "python/test_.py");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
        } catch (Exception ex) {
            IJ.log(ex.getMessage());
        }

        /*
        int imageCount = WindowManager.getImageCount();
        int[] idList = WindowManager.getIDList();
        PoorMan3DReg_ reg = new PoorMan3DReg_();
        _3D_objects_counter counter = new _3D_objects_counter();

        if (imageCount < 1) {
            IJ.showMessage("Calcium Signal", "No image found.");
            return;
        }

        for (int id : idList) {
            ImagePlus img = WindowManager.getImage(id);
            WindowManager.setTempCurrentImage(img);
            reg.run(arg);
            counter.run(arg);
        }

         */

    }

    public static void main(String[] args) {
        // sanity check
        // System.out.println("Hello, world!");
        /*
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python", "test_.py");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
        } catch (IOException ex) {
            System.out.println("Error.");
        }

         */
    }
}
