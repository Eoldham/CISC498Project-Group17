import celldetection._3D_objects_counter;
import ij.*;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.text.TextPanel;
import ij.text.TextWindow;
import imageJ.plugins.PoorMan3DReg_;

import java.awt.*;
import java.io.*;
import java.util.Scanner;
import java.io.IOException;

public class CalciumSignal_ implements PlugIn {
    private final String EDGE_DATA_PATH = "plugins/CalciumSignal/edge_data";
    private final String PYTHONSCRIPT_PATH = "plugins/CalciumSignal/pythonscript";

    public void run(String arg) {

        IJ.showMessage("Calcium Signal", "Welcome to the Calcium Signal plugin!");
        Frame roiWindow = WindowManager.getCurrentWindow();

        /*
        -- IMAGE REGISTRATION AND EDGE DETECTION --
         */
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


        /*
        -- ROI MANAGER --
         */

        //Gets active table and saves
        String path = EDGE_DATA_PATH + "/edgeDetectResults.csv";
        ResultsTable results = ij.measure.ResultsTable.getResultsTable();

        try {
            results.saveAs(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        WindowManager.removeWindow(WindowManager.getFrontWindow());
        WindowManager.removeWindow(WindowManager.getFrontWindow());
        WindowManager.toFront(roiWindow);

        runRoiManager();


    }

    void runRoiManager(){

        //Creates RoiManager
        custom_roiManager crm = new custom_roiManager();

        //Creates New scanner of edgeDetection CSV
        try {
            String pathName = EDGE_DATA_PATH + "/edgeDetectResults.csv";
            Scanner scan = new Scanner(new File(pathName));

            //Vars
            double x;
            double y;
            double width;
            double height;
            String line;
            String splitLine[];
            int cornerDiameter = 20;

            //Skips first line
            scan.nextLine();

            while (scan.hasNext()) {
                line = scan.nextLine();
                splitLine = line.split("[,]");

                // This is used to make sure we have x and y at the center of the detected region
                width = Double.parseDouble(splitLine[24]);
                height = Double.parseDouble(splitLine[25]);
                x = Double.parseDouble(splitLine[12]) - width/2 ;
                y = Double.parseDouble(splitLine[13]) - height/2;

                //Create ROI with Input: int x, int y, int width, int height, int cornerDiameter
                Roi roi = new Roi((int)x, (int)y, (int)width, (int)height, cornerDiameter);


                //Add Roi to RoiManager
                crm.addRoi(roi);

            }
        }catch(IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        // sanity check
        // System.out.println("Hello, world!");
    }
}
