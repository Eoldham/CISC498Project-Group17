import celldetection._3D_objects_counter;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import imageJ.plugins.PoorMan3DReg_;
import java.io.*;
import java.util.Scanner;
import java.io.IOException;

public class CalciumSignal_ implements PlugIn {

    public void run(String arg) {

        IJ.showMessage("Calcium Signal", "Welcome to the Calcium Signal plugin!");


        /*
        -- IMAGE REGISTRATION AND EDGE DETECTION --
         */
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


        /*
        -- ROI MANAGER --
         */
        runRoiManager();


        /*
        -- PEAK FINDING --
         */
        /*
        try {
            // RELATIVE TO LOCATION OF FIJI EXECUTABLE
            ProcessBuilder processBuilder = new ProcessBuilder("python", "pythonscript/peakscript.py");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
        } catch (Exception ex) {
            IJ.log(ex.getMessage());
        }
         */

    }

    void runRoiManager(){

        //Creates RoiManager
        RoiManager rm = new RoiManager();

        //Creates New scanner of edgeDetection CSV
        try {
            String pathName = "C:\\Users\\emold\\Desktop\\CISC498\\CISC498Project-Group17\\src\\main\\java\\Sample.csv";
            Scanner scan = new Scanner(new File(pathName));

            //Vars
            double x;
            double y;
            double width;
            double height;
            String line;
            String splitLine[];
            int cornerDiameter = 5;

            //Skips first line
            scan.nextLine();

            while (scan.hasNext()) {
                line = scan.nextLine();
                splitLine = line.split("[,]");

                int fixer = 0;
                //int fixer = 10

                //Set all necessary vars
                x = Double.parseDouble(splitLine[3])  - fixer;
                y = Double.parseDouble(splitLine[4]) - fixer;
                width = Double.parseDouble(splitLine[6]);
                height = Double.parseDouble(splitLine[7]);

                //Create ROI with Input: int x, int y, int width, int height, int cornerDiameter
                Roi roi = new Roi((int)x, (int)y, (int)width, (int)height, cornerDiameter);


                //Add Roi to RoiManager
                rm.addRoi(roi);


            }
        }catch(IOException e) {
            e.printStackTrace();
        }

        //Makes Roi's visible in roi Manager
        rm.runCommand("show all with labels");


        NonBlockingGenericDialog message =  new NonBlockingGenericDialog("Done editing cells");
        message.addMessage("When you are done adding and deleting cells press OK to measure");
        message.showDialog();

        if (message.wasOKed()) {

            rm.runCommand("multi-measure");

            //Gets active table and saves
            String path = "C:\\Users\\emold\\Desktop\\CISC498\\CISC498Project-Group17\\src\\main\\java\\Results.csv";
            ResultsTable results = ij.measure.ResultsTable.getResultsTable();
            try {
                results.saveAs(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        //ResultsTable resultsTable = rm.multiMeasure(imp);

    }

    public static void main(String[] args) {
        // sanity check
        // System.out.println("Hello, world!");
    }
}
