import ij.*;
import ij.ImagePlus.*;
import ij.process.*;
import ij.gui.*;
import ij.util.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Image;
import java.awt.Graphics2D;
import ij.plugin.PlugIn;
import ij.gui.GenericDialog;
import java.text.DecimalFormat;
import ij.gui.Overlay;
import java.util.Arrays;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.Toolkit;
import Objects3D.Counter3D;

public class _3D_objects_counter implements PlugIn, AdjustmentListener, FocusListener {
    ImagePlus imp;
    ImageProcessor ip;
    int width, height, nbSlices, length;
    double min, max;
    String title, redirectTo;
    int thr, minSize, maxSize, dotSize, fontSize;
    boolean excludeOnEdges, showObj, showSurf, showCentro, showCOM, showNb, whiteNb, newRT, showStat, showMaskedImg, closeImg, showSummary, redirect;
    Vector sliders, values;
    private final static float GAUSSIAN_CUT_OFF = 0.005f;
    private final static float MAGNITUDE_SCALE = 100F;
    private final static float MAGNITUDE_LIMIT = 1000F;
    private final static int MAGNITUDE_MAX = (int) (MAGNITUDE_SCALE * MAGNITUDE_LIMIT);
    private int picsize;
    private int[] data;
    private int[] magnitude;
    private ImagePlus sourceImage;
    private static float sGaussianKernelRadius = 2f;
    private static float sLowThreshold = 2.5f;
    private static float sHighThreshold = 7.5f;
    private float gaussianKernelRadius = 2f;
    private float lowThreshold = 2.5f;
    private float highThreshold = 7.5f;
    private int gaussianKernelWidth = 16;
    private boolean contrastNormalized;
    private float[] xConv;
    private float[] yConv;
    private float[] xGradient;
    private float[] yGradient;

    public void run(String arg) {
        if (IJ.versionLessThan("1.39i")) return;
        sourceImage = IJ.getImage();
        /*if (sourceImage.getStackSize()>1) {
            IJ.error("This plugin does not work with stacks");
            return;
        }
         */
        String sLabel = sourceImage.getTitle();
        String sImLabel = "";
        int sz = sourceImage.getStackSize();
        DecimalFormat df = new DecimalFormat("0000");
        if (!showDialog())
            return;


        for (int n=1;n<=30;++n) {
            sourceImage.setSlice(n);   // activate next slice ***

            // Get current image processor from stack.  What ever is
            // used here should do a COPY pixels from old processor to
            // new. For instance, ImageProcessor.crop() returns copy.
            ImageProcessor ip = sourceImage.getProcessor(); // ***
            ImageProcessor newip = ip.createProcessor(ip.getWidth(),
                    ip.getHeight());
            newip.setPixels(ip.getPixelsCopy());

            // Create a suitable label, using the slice label if possible
            sImLabel = sourceImage.getStack().getSliceLabel(n);
            if (sImLabel == null || sImLabel.length() < 1) {
                sImLabel = "slice"+df.format(n)+"_"+sLabel;
            }
            // Create new image corresponding to this slice.
            ImagePlus im = new ImagePlus(sImLabel, newip);
            im.setCalibration(sourceImage.getCalibration());

            //gaussian
            Undo.setup(Undo.TYPE_CONVERSION, sourceImage);
            process();
            ImageProcessor dp = new FloatProcessor(sourceImage.getWidth(), sourceImage.getHeight(), data);
            dp = dp.convertToByte(false);
            if (!Prefs.blackBackground)
                dp.invertLut();
            sourceImage.setProcessor(dp);

            // Show this image.

        }

        ImagePlus firstImage = IJ.getImage();

        firstImage.setSlice(1);
        ImageProcessor ip = firstImage.getProcessor(); // ***
        ImageProcessor newip = ip.createProcessor(ip.getWidth(),
                ip.getHeight());
        newip.setPixels(ip.getPixelsCopy());

        // Create a suitable label, using the slice label if possible
        sImLabel = firstImage.getStack().getSliceLabel(2);
        if (sImLabel == null || sImLabel.length() < 2) {
            sImLabel = "slice"+df.format(2)+"_"+sLabel;
        }
        // Create new image corresponding to this slice.
        ImagePlus im = new ImagePlus(sImLabel, newip);
        im.setCalibration(firstImage.getCalibration());
        Overlay ovl = new Overlay();

        BufferedImage imbuf = im.getBufferedImage();
        Graphics2D g2 = imbuf.createGraphics();

        int pixel = imbuf.getRGB(0, 0);
        Color color = new Color(pixel, true);
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        int alpha = color.getAlpha();
        String s = "";
        s = s + " " + Integer.toString(alpha);
        s = s + " " + Integer.toString(red);
        s = s + " " + Integer.toString(green);
        s = s + " " + Integer.toString(blue);

        for (int n = 2; n <= 30;++n) {
            firstImage.setSlice(n);

            ImageProcessor sec = firstImage.getProcessor(); // ***
            ImageProcessor secip = sec.createProcessor(sec.getWidth(),
                    sec.getHeight());
            secip.setPixels(sec.getPixelsCopy());

            // Create a suitable label, using the slice label if possible
            sImLabel = firstImage.getStack().getSliceLabel(2);
            if (sImLabel == null || sImLabel.length() < 2) {
                sImLabel = "slice"+df.format(2)+"_"+sLabel;
            }
            // Create new image corresponding to this slice.
            ImagePlus im2 = new ImagePlus(sImLabel, secip);
            im2.setCalibration(firstImage.getCalibration());

            BufferedImage source = im2.getBufferedImage();

            // Image imageWithTransparency = makeColorTransparent(source, new Color(pixel));
            // BufferedImage transparentImage = imageToBufferedImage(imageWithTransparency);

            Image secondSlice = makeColorTransparent(source, new Color(pixel));
            g2.drawImage(secondSlice, 0,0, null);


            //ImageRoi roi = new ImageRoi(0,0, firstImage.getProcessor());
            //roi.setZeroTransparent(false);
            // roi.setOpacity(.5);
            //ovl.add(roi);
            // im.setOverlay(ovl);
        }
        g2.dispose();
        ImagePlus tmp = new ImagePlus(s, imbuf);
        tmp.show();
        imp=tmp;
        //imp=WindowManager.getCurrentImage();

        if (imp==null){
            IJ.error("Cell Detector Error\n"+"Please add an image\n"+"!!!");
            return;
        }

        if (imp.getBitDepth()>16){
            IJ.error("Cell  Detector only works on 8- or 16-bits images...");
            return;
        }

        width=imp.getWidth();
        height=imp.getHeight();
        nbSlices=imp.getStackSize();
        length=height*width*nbSlices;
        title=imp.getTitle();

        min=Math.pow(2, imp.getBitDepth());
        max=0;

        for (int i=1; i<=nbSlices; i++){
            imp.setSlice(i);
            ip=imp.getProcessor();
            min=Math.min(min, imp.getStatistics().min);
            max=Math.max(max, imp.getStatistics().max);
        }

        imp.setSlice((int)nbSlices/2);
        imp.resetDisplayRange();
        thr=ip.getAutoThreshold();
        ip.setThreshold(thr, max,ImageProcessor.RED_LUT);
        imp.updateAndDraw();

        minSize=(int) Prefs.get("3D-OC_minSize.double", 10);
        maxSize=length;
        excludeOnEdges=Prefs.get("3D-OC_excludeOnEdges.boolean", true);
        showObj=Prefs.get("3D-OC_showObj.boolean", true);
        showSurf=Prefs.get("3D-OC_showSurf.boolean", true);
        showCentro=Prefs.get("3D-OC_showCentro.boolean", true);
        showCOM=Prefs.get("3D-OC_showCOM.boolean", true);
        showStat=Prefs.get("3D-OC_showStat.boolean", true);
        showSummary=Prefs.get("3D-OC_summary.boolean", true);

        showMaskedImg=Prefs.get("3D-OC-Options_showMaskedImg.boolean", true);
        closeImg=Prefs.get("3D-OC-Options_closeImg.boolean", false);


        redirectTo=Prefs.get("3D-OC-Options_redirectTo.string", "none");
        redirect=!this.redirectTo.equals("none") && WindowManager.getImage(this.redirectTo)!=null;

        if (redirect){
            ImagePlus imgRedir=WindowManager.getImage(this.redirectTo);
            if (!(imgRedir.getWidth()==this.width && imgRedir.getHeight()==this.height && imgRedir.getNSlices()==this.nbSlices) || imgRedir.getBitDepth()>16){
                redirect=false;
                showMaskedImg=false;
                //IJ.log("Redirection canceled: images should have the same size and a depth of 8- or 16-bits.");
            }
            if (imgRedir.getTitle().equals(this.title)){
                redirect=false;
                showMaskedImg=false;
                //IJ.log("Redirection canceled: both images have the same title.");
            }
        }

        if (!redirect){
            Prefs.set("3D-OC-Options_redirectTo.string", "none");
            Prefs.set("3D-OC-Options_showMaskedImg.boolean", false);
        }

        GenericDialog gd=new GenericDialog("Cell Detector v2.0");

        gd.addSlider("Threshold", min, max, thr);
        gd.addSlider("Slice", 1, nbSlices, nbSlices/2);

        sliders=gd.getSliders();
        ((Scrollbar)sliders.elementAt(0)).addAdjustmentListener(this);
        ((Scrollbar)sliders.elementAt(1)).addAdjustmentListener(this);
        values = gd.getNumericFields();
        ((TextField)values.elementAt(0)).addFocusListener(this);
        ((TextField)values.elementAt(1)).addFocusListener(this);

        gd.addMessage("Size filter: ");
        gd.addNumericField("Min.",minSize, 0);
        gd.addNumericField("Max.", maxSize, 0);
        gd.addCheckbox("Exclude_objects_on_edges", excludeOnEdges);

        if (redirect) gd.addMessage("\nRedirection:\nImage used as a mask: "+this.title+"\nMeasures will be done on: "+this.redirectTo+(showMaskedImg?"\nMasked image will be shown":"")+".");
        if (closeImg) gd.addMessage("\nCaution:\nImage(s) will be closed during the processing\n(see 3D-OC options to change this setting).");

        gd.showDialog();

        if (gd.wasCanceled()){
            ip.resetThreshold();
            imp.updateAndDraw();
            return;
        }

        thr=(int) gd.getNextNumber();
        gd.getNextNumber();
        minSize=(int) gd.getNextNumber();
        maxSize=(int) gd.getNextNumber();
        excludeOnEdges=gd.getNextBoolean();
        showObj=true;
        showSurf=false;
        showCentro=false;
        showCOM=false;
        showStat=true;
        showSummary= false;

        Prefs.set("3D-OC_minSize.double", minSize);
        Prefs.set("3D-OC_excludeOnEdges.boolean", excludeOnEdges);
        Prefs.set("3D-OC_showObj.boolean", showObj);
        Prefs.set("3D-OC_showSurf.boolean", showSurf);
        Prefs.set("3D-OC_showCentro.boolean", showCentro);
        Prefs.set("3D-OC_showCOM.boolean", showCOM);
        Prefs.set("3D-OC_showStat.boolean", showStat);
        Prefs.set("3D-OC_summary.boolean", showSummary);
        if (!redirect) Prefs.set("3D-OC-Options_redirectTo.string", "none");

        ip.resetThreshold();
        imp.updateAndDraw();

        Counter3D OC=new Counter3D(imp, thr, minSize, maxSize, excludeOnEdges, redirect);

        dotSize=(int) Prefs.get("3D-OC-Options_dotSize.double", 5);
        fontSize=(int) Prefs.get("3D-OC-Options_fontSize.double", 10);
        showNb=Prefs.get("3D-OC-Options_showNb.boolean", true);
        whiteNb=Prefs.get("3D-OC-Options_whiteNb.boolean", true);

        if (showObj){OC.getObjMap(showNb, fontSize).show(); IJ.run("Fire");}
        if (showSurf){OC.getSurfPixMap(showNb, whiteNb, fontSize).show(); IJ.run("Fire");}
        if (showCentro){OC.getCentroidMap(showNb, whiteNb, dotSize, fontSize).show(); IJ.run("Fire");}
        if (showCOM){OC.getCentreOfMassMap(showNb, whiteNb, dotSize, fontSize).show(); IJ.run("Fire");}

        newRT=Prefs.get("3D-OC-Options_newRT.boolean", true);

        if (showStat) OC.showStatistics(newRT);

        if (showSummary) OC.showSummary();
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        updateImg();
    }

    @Override
    public void focusGained(FocusEvent e) {

    }

    public void focusLost(FocusEvent e) {
        if (e.getSource().equals(values.elementAt(0))){
            int val=(int) Tools.parseDouble(((TextField)values.elementAt(0)).getText());
            val=(int) Math.min(max, Math.max(min, val));
            ((TextField)values.elementAt(0)).setText(""+val);
        }

        if (e.getSource().equals(values.elementAt(1))){
            int val=(int) Tools.parseDouble(((TextField)values.elementAt(1)).getText());
            val=(int) Math.min(max, Math.max(min, val));
            ((TextField)values.elementAt(1)).setText(""+val);
        }

        updateImg();
    }
    public void process() {
        width = sourceImage.getWidth();
        height = sourceImage.getHeight();
        picsize = width * height;
        initArrays();
        readLuminance();
        if (contrastNormalized) normalizeContrast();
        computeGradients(gaussianKernelRadius, gaussianKernelWidth);
        int low = Math.round(lowThreshold * MAGNITUDE_SCALE);
        int high = Math.round( highThreshold * MAGNITUDE_SCALE);
        performHysteresis(low, high);
        thresholdEdges();
    }

    private void readLuminance() {
        ImageProcessor ip = sourceImage.getProcessor();
        ip = ip.convertToByte(true);
        for (int i=0; i<ip.getPixelCount(); i++)
            data[i] = ip.get(i);
    }
    private void initArrays() {
        if (data == null || picsize != data.length) {
            data = new int[picsize];
            magnitude = new int[picsize];

            xConv = new float[picsize];
            yConv = new float[picsize];
            xGradient = new float[picsize];
            yGradient = new float[picsize];
        }
    }

    private void normalizeContrast() {
        int[] histogram = new int[256];
        for (int i = 0; i < data.length; i++) {
            histogram[data[i]]++;
        }
        int[] remap = new int[256];
        int sum = 0;
        int j = 0;
        for (int i = 0; i < histogram.length; i++) {
            sum += histogram[i];
            int target = sum*255/picsize;
            for (int k = j+1; k <=target; k++) {
                remap[k] = i;
            }
            j = target;
        }

        for (int i = 0; i < data.length; i++) {
            data[i] = remap[data[i]];
        }
    }

    private void thresholdEdges() {
        for (int i = 0; i < picsize; i++) {
            data[i] = data[i] > 0 ? 255 : 0;
            //data[i] = data[i] > 0 ? -1 : 0xff000000;
        }
    }
    private void performHysteresis(int low, int high) {
        //NOTE: this implementation reuses the data array to store both
        //luminance data from the image, and edge intensity from the processing.
        //This is done for memory efficiency, other implementations may wish
        //to separate these functions.
        Arrays.fill(data, 0);

        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (data[offset] == 0 && magnitude[offset] >= high) {
                    follow(x, y, offset, low);
                }
                offset++;
            }
        }
    }

    private float hypot(float x, float y) {
        return (float) Math.hypot(x, y);
    }

    private float gaussian(float x, float sigma) {
        return (float) Math.exp(-(x * x) / (2f * sigma * sigma));
    }

    private boolean showDialog() {
        if (!IJ.isMacro()) {
            gaussianKernelRadius = sGaussianKernelRadius;
            lowThreshold = sLowThreshold;
            highThreshold = sHighThreshold;
        }
        GenericDialog gd = new GenericDialog("Canny Edge Detector");
        gd.addNumericField("Gaussian kernel radius: ", gaussianKernelRadius, 1);
        gd.addNumericField("Low threshold:", lowThreshold, 1);
        gd.addNumericField("High threshold:", highThreshold, 1);
        //gd.addNumericField("Gaussian Kernel width:", gaussianKernelWidth, 0);
        gd.addCheckbox("Normalize contrast ", contrastNormalized);
        gd.showDialog();
        if (gd.wasCanceled())
            return false;
        gaussianKernelRadius = (float)gd.getNextNumber();
        if (gaussianKernelRadius<0.1f)
            gaussianKernelRadius = 0.1f;
        lowThreshold = (float)gd.getNextNumber();
        if (lowThreshold<0.1f)
            lowThreshold = 0.1f;
        highThreshold = (float)gd.getNextNumber();
        if (highThreshold<0.1f)
            highThreshold = 0.1f;
        //gaussianKernelWidth = (int)gd.getNextNumber();
        contrastNormalized = gd.getNextBoolean();
        if (!IJ.isMacro()) {
            sGaussianKernelRadius = gaussianKernelRadius;
            sLowThreshold = lowThreshold;
            sHighThreshold = highThreshold;
        }
        return true;
    }

    private void computeGradients(float kernelRadius, int kernelWidth) {
        //generate the gaussian convolution masks
        float kernel[] = new float[kernelWidth];
        float diffKernel[] = new float[kernelWidth];
        int kwidth;
        for (kwidth = 0; kwidth < kernelWidth; kwidth++) {
            float g1 = gaussian(kwidth, kernelRadius);
            if (g1 <= GAUSSIAN_CUT_OFF && kwidth >= 2) break;
            float g2 = gaussian(kwidth - 0.5f, kernelRadius);
            float g3 = gaussian(kwidth + 0.5f, kernelRadius);
            kernel[kwidth] = (g1 + g2 + g3) / 3f / (2f * (float) Math.PI * kernelRadius * kernelRadius);
            diffKernel[kwidth] = g3 - g2;
        }

        int initX = kwidth - 1;
        int maxX = width - (kwidth - 1);
        int initY = width * (kwidth - 1);
        int maxY = width * (height - (kwidth - 1));

        //perform convolution in x and y directions
        for (int x = initX; x < maxX; x++) {
            for (int y = initY; y < maxY; y += width) {
                int index = x + y;
                float sumX = data[index] * kernel[0];
                float sumY = sumX;
                int xOffset = 1;
                int yOffset = width;
                for(; xOffset < kwidth ;) {
                    sumY += kernel[xOffset] * (data[index - yOffset] + data[index + yOffset]);
                    sumX += kernel[xOffset] * (data[index - xOffset] + data[index + xOffset]);
                    yOffset += width;
                    xOffset++;
                }
                yConv[index] = sumY;
                xConv[index] = sumX;
            }
        }

        for (int x = initX; x < maxX; x++) {
            for (int y = initY; y < maxY; y += width) {
                float sum = 0f;
                int index = x + y;
                for (int i = 1; i < kwidth; i++)
                    sum += diffKernel[i] * (yConv[index - i] - yConv[index + i]);

                xGradient[index] = sum;
            }
        }

        for (int x = kwidth; x < width - kwidth; x++) {
            for (int y = initY; y < maxY; y += width) {
                float sum = 0.0f;
                int index = x + y;
                int yOffset = width;
                for (int i = 1; i < kwidth; i++) {
                    sum += diffKernel[i] * (xConv[index - yOffset] - xConv[index + yOffset]);
                    yOffset += width;
                }

                yGradient[index] = sum;
            }

        }

        initX = kwidth;
        maxX = width - kwidth;
        initY = width * kwidth;
        maxY = width * (height - kwidth);
        for (int x = initX; x < maxX; x++) {
            for (int y = initY; y < maxY; y += width) {
                int index = x + y;
                int indexN = index - width;
                int indexS = index + width;
                int indexW = index - 1;
                int indexE = index + 1;
                int indexNW = indexN - 1;
                int indexNE = indexN + 1;
                int indexSW = indexS - 1;
                int indexSE = indexS + 1;

                float xGrad = xGradient[index];
                float yGrad = yGradient[index];
                float gradMag = hypot(xGrad, yGrad);

                //perform non-maximal supression
                float nMag = hypot(xGradient[indexN], yGradient[indexN]);
                float sMag = hypot(xGradient[indexS], yGradient[indexS]);
                float wMag = hypot(xGradient[indexW], yGradient[indexW]);
                float eMag = hypot(xGradient[indexE], yGradient[indexE]);
                float neMag = hypot(xGradient[indexNE], yGradient[indexNE]);
                float seMag = hypot(xGradient[indexSE], yGradient[indexSE]);
                float swMag = hypot(xGradient[indexSW], yGradient[indexSW]);
                float nwMag = hypot(xGradient[indexNW], yGradient[indexNW]);
                float tmp;
                /*
                 * An explanation of what's happening here, for those who want
                 * to understand the source: This performs the "non-maximal
                 * supression" phase of the Canny edge detection in which we
                 * need to compare the gradient magnitude to that in the
                 * direction of the gradient; only if the value is a local
                 * maximum do we consider the point as an edge candidate.
                 *
                 * We need to break the comparison into a number of different
                 * cases depending on the gradient direction so that the
                 * appropriate values can be used. To avoid computing the
                 * gradient direction, we use two simple comparisons: first we
                 * check that the partial derivatives have the same sign (1)
                 * and then we check which is larger (2). As a consequence, we
                 * have reduced the problem to one of four identical cases that
                 * each test the central gradient magnitude against the values at
                 * two points with 'identical support'; what this means is that
                 * the geometry required to accurately interpolate the magnitude
                 * of gradient function at those points has an identical
                 * geometry (upto right-angled-rotation/reflection).
                 *
                 * When comparing the central gradient to the two interpolated
                 * values, we avoid performing any divisions by multiplying both
                 * sides of each inequality by the greater of the two partial
                 * derivatives. The common comparand is stored in a temporary
                 * variable (3) and reused in the mirror case (4).
                 *
                 */
                if (xGrad * yGrad <= (float) 0 /*(1)*/
                        ? Math.abs(xGrad) >= Math.abs(yGrad) /*(2)*/
                        ? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * neMag - (xGrad + yGrad) * eMag) /*(3)*/
                        && tmp > Math.abs(yGrad * swMag - (xGrad + yGrad) * wMag) /*(4)*/
                        : (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * neMag - (yGrad + xGrad) * nMag) /*(3)*/
                        && tmp > Math.abs(xGrad * swMag - (yGrad + xGrad) * sMag) /*(4)*/
                        : Math.abs(xGrad) >= Math.abs(yGrad) /*(2)*/
                        ? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * seMag + (xGrad - yGrad) * eMag) /*(3)*/
                        && tmp > Math.abs(yGrad * nwMag + (xGrad - yGrad) * wMag) /*(4)*/
                        : (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * seMag + (yGrad - xGrad) * sMag) /*(3)*/
                        && tmp > Math.abs(xGrad * nwMag + (yGrad - xGrad) * nMag) /*(4)*/
                ) {
                    magnitude[index] = gradMag >= MAGNITUDE_LIMIT ? MAGNITUDE_MAX : (int) (MAGNITUDE_SCALE * gradMag);
                    //NOTE: The orientation of the edge is not employed by this
                    //implementation. It is a simple matter to compute it at
                    //this point as: Math.atan2(yGrad, xGrad);
                } else {
                    magnitude[index] = 0;
                }
            }
        }
    }
    public Image makeColorTransparent(final BufferedImage im, final Color color)
    {
        final ImageFilter filter = new RGBImageFilter()
        {
            // the color we are looking for (white)... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() | 0xFFFFFFFF;

            public int filterRGB( int x,  int y, int rgb)
            {
                if ((rgb | 0xFF000000) == markerRGB)
                {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                }
                else
                {
                    // nothing to do
                    return rgb;
                }
            }
        };
        final ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    private void follow(int x1, int y1, int i1, int threshold) {
        int x0 = x1 == 0 ? x1 : x1 - 1;
        int x2 = x1 == width - 1 ? x1 : x1 + 1;
        int y0 = y1 == 0 ? y1 : y1 - 1;
        int y2 = y1 == height -1 ? y1 : y1 + 1;

        data[i1] = magnitude[i1];
        for (int x = x0; x <= x2; x++) {
            for (int y = y0; y <= y2; y++) {
                int i2 = x + y * width;
                if ((y != y1 || x != x1)
                        && data[i2] == 0
                        && magnitude[i2] >= threshold) {
                    follow(x, y, i2, threshold);
                    return;
                }
            }
        }
    }

    private void updateImg(){
        thr=((Scrollbar)sliders.elementAt(0)).getValue();
        imp.setSlice(((Scrollbar)sliders.elementAt(1)).getValue());
        imp.resetDisplayRange();
        ip.setThreshold(thr, max, ImageProcessor.RED_LUT);
    }
}
