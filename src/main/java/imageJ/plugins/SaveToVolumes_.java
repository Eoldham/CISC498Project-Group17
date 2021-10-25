package imageJ.plugins;

import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.*;
import ij.io.*;


/*
* SaveTo Volumes saves an XYZT stack to a series of volume files.
*/
public class SaveToVolumes_ implements PlugIn {


   /**
    * The default constructor.  This is the constructor used when the
    * plugin is loaded.
    */
    public SaveToVolumes_() {;} //Nothing to do.


   /**
    * Builds a dialog to query users for the group size and
    * projection method.  The projection method defaultMethod will
    * be selected by default.
    *
    * @param depth      The default group size
   	* @param parent  The dialog's parent Frame
    * @return  The resulting GenericDialog
    */
    private GenericDialog buildDialog(int depth, java.awt.Frame parent) {

       GenericDialog gd =
          new GenericDialog("Save one volume file per time point...", parent);

       // Text field for the group size.
       gd.addNumericField("Depth:", depth, 0/*digits*/);

        return gd;
    }


   /**
    * Saves the volumes to separate files
    *
    * 
    *
    */
    public void SaveFiles(ImagePlus imp, int depth, String directory, String basename) {

        int imp_size = imp.getStackSize();

        // Validate the arguments.
        if ( imp_size == 0 )
            throw new IllegalArgumentException("Empty stack.");
        if ( !validGroupSize(imp_size, depth) )
            throw new IllegalArgumentException("Invalid group size.");

        // Create an empty stack to hold the projections.
        // It will have the same width, height, and color model as imp.
		ImagePlus temp_imp;
        ImageStack out_stack;

        // Important note: Slices are numbered 1,2,...,n where n is the
        // number of slices in the stack.
		int sli=0;
		int tmax=imp_size/depth;
        for (int t=0; t<tmax; t++) {
			out_stack = imp.createEmptyStack();
			for (int k=0; k<depth; k++){
				sli++;
				imp.setSlice(sli);
				out_stack.addSlice("Temp slice", imp.getProcessor());
			}
			temp_imp = new ImagePlus("TempStack",out_stack);
			new FileSaver(temp_imp).saveAsTiffStack(directory+basename+num2str(t,tmax)+".tif");
        }

        return;
    }



   /**
    * This method will be called when the plugin is loaded.
    *
    * @param arg  Currently ignored
    */
    public void run(String arg) {

        // Set the input image.
        ImagePlus in_image = WindowManager.getCurrentImage();

        // Check that there is a current image.
        if( in_image==null ) {
            IJ.noImage(); //This posts a message.
            return;
        }


       //  Make sure the input image is a stack.
        int in_size = in_image.getStackSize();
        if( in_size == 1 ) {
            IJ.error("SaveAsVolumes:" +
              " this plugin must be called on an image stack.");
            return;
        }

       // Build and display the dialog.
       GenericDialog gd =
          buildDialog(in_image.getStackSize(), IJ.getInstance());
       gd.showDialog();

       if( gd.wasCanceled() )
            return; //The user pushed the cancel button.

        // Set the group size and the method from the dialog.
        int depth = (int)gd.getNextNumber();

        // If the entered group size is invalid, post a message and
        // then redisplay the dialog.
        while ( !validGroupSize(in_image.getStackSize(), depth ) ) {
            IJ.showMessage("The group size must evenly divide the " +
              "stack size (" + in_image.getStackSize() + ").");
            gd = buildDialog(in_image.getStackSize(), IJ.getInstance());
            gd.showDialog();

            if(gd.wasCanceled())
                return; //The user pushed the cancel button.
            else {
                // Set the group size and the method from the dialog.
                depth = (int)gd.getNextNumber();
            }
        }

        // Get a lock on the image.
        if( !in_image.lock() )
            return; //The image is in use.

		// Get the directory and filebasename
		String directory = "", basename = ""; 
        SaveDialog sd = new SaveDialog("Save TIFF Volumes As...", in_image.getTitle(), ".tif");
        directory = sd.getDirectory();
        basename = sd.getFileName();

        // Save the files.
		SaveFiles(in_image, depth, directory, basename);

        // Free the image lock.
        in_image.unlock();

        // This ensures that the class variables (defaultMethod here)
        // is not reloaded the next time the plugin is launched.
        // This can be that of as an ImageJ bug workaround.
        IJ.register(SaveToVolumes_.class);

        return;
    }



   /**
    * Validates the group size.
    *
    * @param  stack_size  The size of the input image stack
    * @param  gs          The group size to be validated
    * @return  true if gs is valid and false otherwise
    */
    public boolean validGroupSize(int stack_size, int gs) {
        boolean retval; // The return value.

        if ( gs > 0  &&  (gs <= stack_size)  &&  (stack_size % gs) == 0 )
            retval = true;
        else
            retval = false;

        return retval;
    }
	/**
    * Returns a number with leading zeros.
    *
    */
    public String num2str(int number, int maxnumber) {
        StringBuffer retstring = new StringBuffer(""+number); // The return value.
		String maxstring = new String(""+maxnumber);
		int len = maxstring.length();

		while ( retstring.length() <  len) { 
        	retstring.insert( 0, "0");
       	} // End while.
     
       return retstring.toString(); 
    }
}  // End Grouped_ZProjector.


