package edu.udel.team17;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imageJ.plugins.*;

public class CalciumSignal_ implements PlugIn {

    public void run(String arg) {
        // plugin logic here
        // https://imagej.net/develop/ij1-plugins

        PoorMan3DReg_ reg = new PoorMan3DReg_();
        reg.run(arg);
    }

    public static void main(String[] args) {
        // make sure this runs in your IDE
        System.out.println("Hello, world!");
    }
}
