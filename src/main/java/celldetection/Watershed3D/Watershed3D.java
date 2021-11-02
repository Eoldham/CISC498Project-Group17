package celldetection.Watershed3D;

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.process.*;
import java.awt.Color;


public class Watershed3D {
    boolean[] imgArray;
    int[] edm, uep;
    int[] histogram, levelStart, histoWater;
    int[][] coord, centres;
    int width, height, nbSlices, length, maxDim, maxEDM, maxEDMTD, ArraySize, nbCentres=1, nbNuclei;
    double pixW=1, pixH=1, pixD=1;
    Calibration cal;
    String unit, title;
    public static final int one=41, sqrt2=58, sqrt5=92;
    boolean doWatershed=false;
    int[] IDcount;
    int nbObj;
    
    /** Creates a new instance of Watershed3D.
     * @param img ImagePlus from which Watershed3D will be generated.
     * @param threshold threshold value (Integer).
     * @exception Watershed3D expects a 8- or 16-bits image only.
     */
    public Watershed3D(ImagePlus img, int threshold) {
        width=img.getWidth();
        height=img.getHeight();
        nbSlices=img.getNSlices();
        title=img.getTitle();
        
        cal=img.getCalibration();
        pixW=this.cal.pixelWidth;
        pixH=this.cal.pixelHeight;
        pixD=this.cal.pixelDepth;
        unit=this.cal.getUnit();
                
        length=this.width*this.height*this.nbSlices;
        
        if (img.getBitDepth()>16) throw new IllegalArgumentException("Watershed3D expects a 8- or 16-bits image only");
        //2 problems: Math.pow (x, 2) is really too slow+Math round also: so I use x*x and cast double+.5 to int .5 is requiered to be sure 1.8 is casted to 2 (1.8+.5=2.3) instead of 1
        maxDim=(int) (Math.sqrt(this.width*this.width+this.height*this.height+this.nbSlices*this.nbSlices)+.5);
        
        int index=0;
        imgArray=new boolean[this.length];
        for (int z=1; z<=this.nbSlices; z++){
            img.setSlice(z);
            for (int y=0; y<this.height; y++){
                for (int x=0; x<this.width; x++){
                    this.imgArray[index]=img.getProcessor().getPixel(x, y)<threshold?false:true;
                    index++;
                }
            }
        }
        img.close();
    }
    
    /** Generates the Euclidian Distance Map (EDM) for the current Watershed3D object.
     * Retrieves the minimum distance between the current pixel and the nearest background pixel.
     * The minimum distance is checked over the 26 neighbours and the additional 8 locates at a normalized distance of sqrt(5).
     * The EDM is generated through 2 passes (up-left to down-right then down-right to up-left) and further EDM filtering.
     * This strategy is a copy of the one already implemented in ImageJ for 2D images, extended to the 3D case.
     * @param filter enables/disables the filtering of the EDM (boolean), used to minimise the number of local maxima.
     */
    public void EDM(boolean filter){
        int index=0;
        this.edm=new int[this.length];
        
        for (int i=0; i<this.length; i++) this.edm[i]=((imgArray[i])?1:0)*one*65535;
        
        for (int z=1; z<=this.nbSlices; z++){
            for (int y=0; y<this.height; y++){
                for (int x=0; x<this.width; x++){
                    if (this.edm[index]>0){
                        setValEDM(x, y, z);
                    }
                    index++;
                }
            }
            IJ.showStatus("Creating EDM...");
            IJ.showProgress(z,2*this.nbSlices);
        }
        index--;
        for (int z=this.nbSlices; z>=1; z--){
            for (int y=this.height-1; y>=0; y--){
                for (int x=this.width-1; x>=0; x--){
                    if (this.edm[index]>0){
                        setValEDM(x, y, z);
                    }
                    index--;
                }
            }
            IJ.showStatus("Creating EDM...");
            IJ.showProgress(2*this.nbSlices-z,2*this.nbSlices);
        }
        IJ.showStatus("");
        
        maxEDM=0;
        for (int i=0; i<this.length; i++){
            this.edm[i]=(this.edm[i]+one/2)/one;
            maxEDM=Math.max(maxEDM, this.edm[i]);
        }
        
        if (filter){
            filterEDM(true);
            filterEDM(false);
        }
    }
    
    /** Set the EDM value for the current pixel.
     * Retrieves the minimum distance between the current pixel and the nearest background pixel.
     * The minimum distance is checked over the 26 neighbours and the additional 8 locates at a normalized distance of sqrt(5).
     * This strategy is a copy of the one already implemented in ImageJ for 2D images, extended to the 3D case.
     * @param x coordinate of the pixel for which value should be set.
     * @param y coordinate of the pixel for which value should be set.
     * @param z coordinate of the pixel for which value should be set.
     */
    private void setValEDM(int x, int y, int z){
        int index=0, min=maxDim*one, v;
        int offset=offset(x, y, z);
        for (int k=z-2; k<=z+2; k++){
            for (int j=y-2; j<=y+2; j++){
                for (int i=x-2; i<=x+2; i++){
                    if (i>=0 && j>=0 && k>=1 && i<this.width && j<this.height && k<=this.nbSlices){
                        //2 problems: Math.pow (x, 2) is really too slow+Math round also: so I use x*x and cast double+.5 to int .5 is requiered to be sure 1.8 is casted to 2 (1.8+.5=2.3) instead of 1
                        int dist= (int) (one*Math.sqrt((x-i)*(x-i)+(y-j)*(y-j)+(z-k)*(z-k))+.5);
                        if (((Math.abs(x-i)<=1 && Math.abs(y-j)<=1 && Math.abs(z-k)<=1) || (dist==92)) && (dist!=0)){
                            v=this.edm[i+j*this.width+(k-1)*this.width*this.height]+dist;
                            if (v<min) min=v;
                        }
                    }
                }
            }
        }
        this.edm[offset]=min;
    }
    
    /** Filters the EDM table.
     * Used to minimise the number of local maxima.
     * This strategy is a copy of the one already implemented in ImageJ for 2D images, extended to the 3D case.
     * @param smooth true: performs a mean filtering based on the 26 neighbours; false: removes the diagonal pixels of same EDM value (boolean).
     */
    public void filterEDM(boolean smooth){
        int index=0;
        int[] array=this.edm.clone();
        
        for (int z=1; z<=this.nbSlices; z++){
            for (int y=0; y<this.height; y++){
                for (int x=0; x<this.width; x++){
                    int v=this.edm[index];
                    if (v>1){
                        int count=1;
                        int sum=v;
                        int diagEq2CurrEDM=0;
                        int pixdiag=0;
                        boolean allVal2c=false;
                        v--;
                        for (int k=z-1; k<=z+1; k++){
                            for (int j=y-1; j<=y+1; j++){
                                for (int i=x-1; i<=x+1; i++){
                                        if ((i>=0 && i<this.width) && (j>=0 && j<this.height) && (k>=1 && k<=this.nbSlices) && !(i==x && j==y && k==z)){
                                        int val=this.edm[offset(i,j,k)];
                                        if (smooth){
                                            sum+=val;
                                            count++;
                                        }else{
                                            //2 problems: Math.pow (x, 2) is really too slow+Math round also: so I use x*x and cast double+.5 to int .5 is requiered to be sure 1.8 is casted to 2 (1.8+.5=2.3) instead of 1
                                            double dist= (Math.sqrt((x-i)*(x-i)+(y-j)*(y-j)+(z-k)*(z-k))+.5);
                                            if (dist>1){
                                                pixdiag++;
                                                if (val==v+1){
                                                    diagEq2CurrEDM++;
                                                    if (diagEq2CurrEDM>1){
                                                        i=x+3;
                                                        j=y+3;
                                                        k=z+3;
                                                    }
                                                }else{
                                                    if (val==v){
                                                        allVal2c=true;
                                                    }else{
                                                        allVal2c=false;
                                                        i=x+3;
                                                        j=y+3;
                                                        k=z+3;
                                                    }
                                                }
                                            }else{
                                                if (val==v){
                                                        allVal2c=true;
                                                    }else{
                                                        allVal2c=false;
                                                        i=x+3;
                                                        j=y+3;
                                                        k=z+3;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (smooth){
                            array[index]=sum/count;
                        }else{
                            if (diagEq2CurrEDM==1 && allVal2c) array[index]=v;
                        }
                    }
                    index++;
                }
            }
            IJ.showStatus("Filtering EDM...");
        }
        
        maxEDM=0;
        for (int i=0; i<this.length; i++){
            this.edm[i]=array[i];
            maxEDM=Math.max(maxEDM, this.edm[i]);
        }
        array=null;
        System.gc();
        IJ.showStatus("");
    }
    
    /** Returns an ImagePlus of the Euclidian Distance Map (EDM) for the current Watershed3D object.
     * In case the EDM has not been already calculated, getEDM calculates it first, with or without getting it filtered.
     * @param filter enables/disables the filtering of the EDM (boolean), used to minimise the number of local maxima.
     * @param title to attribute to the ImagePlus (string).
     * @return an ImagePlus of the EDM.
     */
    public ImagePlus getEDM(boolean filter, String title){
        if (this.edm==null) EDM(filter);
        return buildImg(this.edm, title);
    }
    
    /** Generates the Ultimate Erode Point (UEP) for the current Watershed3D object.
     * Only keeps the local maximum EDM regions, then connects the pixels forming those area and finally retrieves their geometrical centres.
     * In case the UEP and EDM have not been already calculated, getUEP calculates both of them first, with or without getting the EDM filtered.
     * @param filter enables/disables the filtering of the EDM (boolean), used to minimise the number of local maxima.
     */
    public void UEP(boolean filter){
        if (this.edm==null) EDM(filter);
        makeCoordArray();
        this.uep=this.edm;
        this.edm=null;
        System.gc();
        
        //Look for local max (not strict ie, 2 adjacent pixels with same intensity are both kept) 
        int index=0;
        for (int level=maxEDM; level>=1; level--){
            do{
                index=0;
                for (int l=0; l<this.histogram[level]; l++){
                    int coordOffset=this.levelStart[level]+l;
                    int x=this.coord[coordOffset][0];
                    int y=this.coord[coordOffset][1];
                    int z=this.coord[coordOffset][2];
                    int offset=offset(x,y,z);
                    boolean setPixel;
                    if (this.uep[offset]!=maxEDM+1){
                        setPixel=false;
                        for (int k=z-1; k<=z+1; k++){
                            for (int j=y-1; j<=y+1; j++){
                                for (int i=x-1; i<=x+1; i++){
                                    if ((i>=0 && i<this.width) && (j>=0 && j<this.height) && (k>=1 && k<=this.nbSlices) && !(i==x && j==y && k==z)){
                                        int val=this.uep[offset(i,j,k)];
                                        if (val>level) setPixel=true;
                                    }
                                }
                            }
                        }
                        if (setPixel){
                            this.uep[offset]=maxEDM+1;
                            index++;
                        }
                    }
                }
            } while(index!=0);
            IJ.showStatus("Creating UEP...");
        }
        
        for (int i=0; i<this.length; i++)  if (this.uep[i]==maxEDM+1) this.uep[i]=0;
        findMaxima(true);
        IJ.showStatus("");
     }
    
    /** Generates the array coord[nb points][0 to 2: x, y, z], levelStart[nb levels] and levelOffset[nb levels].
     * coord is an array containing all the local maxima coordinates, classifised from the highest EDM level to the lowest.
     * levelStart is an array containing the index of the first pixel for a specific EDM level.
     * levelOffset is an array containing the length of the subarray containing all pixel coordinates for a specific EDM level.
     * Retrieves the minimum distance between the current pixel and the nearest background pixel.
     */
    private void makeCoordArray(){
        buildHistogram();
        ArraySize=0;
        for (int i=0; i<this.histogram.length; i++) ArraySize+=this.histogram[i];
        
        this.coord=new int[ArraySize][3];
        int offset=0;
        this.levelStart=new int [this.histogram.length];
         for (int i=0; i<this.histogram.length; i++){
            this.levelStart[i]=offset;
            if (i>0) offset+=this.histogram[i];
        }
        
        int[] levelOffset=new int [this.histogram.length];
        
        int index=0;
        for (int z=1; z<=this.nbSlices; z++){
            for (int y=0; y<this.height; y++){
                for (int x=0; x<this.width; x++){
                    int val=this.edm[index];
                    if (val>0){
                        offset=this.levelStart[val]+levelOffset[val];
                        this.coord[offset][0]=x;
                        this.coord[offset][1]=y;
                        this.coord[offset][2]=z;
                        levelOffset[val]++;
                    }
                    index++;
                }
            }
        }
        levelOffset=null;
        System.gc();
    }
    
    /** Generates an histogram for the current EDM.
     */
    private void buildHistogram(){
        int max=this.edm[0];
        for (int i=1; i<this.length; i++) max=Math.max(max, this.edm[i]);
        this.histogram=new int[max+1];
        for (int i=0; i<this.length; i++) this.histogram[this.edm[i]]++;
    }
        
    /** Applies a connexity analysis on the current UEP and if the option is set, finally retrieves the geometrical centres of the local maximum areas.
     * @param findCentres only performs the connexity analysis if false, find the geometrical centres if true.
     */
    private void findMaxima(boolean findCentres){
        int currID=0;
        int currPos=0;
        int minID=0;
        int surfPix=0;
        this.IDcount=new int[this.length];
        
        for (int z=1; z<=this.nbSlices; z++){
            for (int y=0; y<this.height; y++){
                for (int x=0; x<this.width; x++){
                    if (minID==currID) currID++;
                    if (this.uep[currPos]!=0){
                        minID=currID;
                        minID=minPrevTagUEP(minID, x, y, z);
                        this.uep[currPos]=minID;
                        this.IDcount[minID]++;
                        }
                    currPos++;
                }
            }
        }
        
        currPos=0;
        minID=1;
        
        for (int z=1; z<=this.nbSlices; z++){
            for (int y=0; y<this.height; y++){
                for (int x=0; x<this.width; x++){
                    if (this.uep[currPos]!=0){
                        minID=this.uep[currPos];
                        //Find the minimum tag in the neighbours pixels
                        for (int neighbZ=z-1; neighbZ<=z+1; neighbZ++){
                            for (int neighbY=y-1; neighbY<=y+1; neighbY++){
                                for (int neighbX=x-1; neighbX<=x+1; neighbX++){
                                    //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
                                    if (neighbX>=0 && neighbX<this.width && neighbY>=0 && neighbY<this.height && neighbZ>=1 && neighbZ<=this.nbSlices){
                                        int offset=offset(neighbX, neighbY, neighbZ);
                                        if (this.uep[offset]!=0){
                                            int currPixID=this.uep[offset];
                                            if (currPixID<minID) minID=this.uep[offset];
                                        }
                                    }
                                }
                            }
                        }
                        
                        //Replacing tag by the minimum tag found
                        for (int neighbZ=z-1; neighbZ<=z+1; neighbZ++){
                            for (int neighbY=y-1; neighbY<=y+1; neighbY++){
                                for (int neighbX=x-1; neighbX<=x+1; neighbX++){
                                    //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
                                    if (neighbX>=0 && neighbX<this.width && neighbY>=0 && neighbY<this.height && neighbZ>=0 && neighbZ<=this.nbSlices){
                                        int offset=offset(neighbX, neighbY, neighbZ);
                                        if (this.uep[offset]!=0){
                                            int currPixID=this.uep[offset];
                                            if (currPixID!=minID) replaceID(currPixID, minID);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    currPos++;
                }
            }
            IJ.showStatus("Finding maxima");
        }
        IJ.showStatus("");
        
        if (findCentres){
            this.centres=new int[currID][3];
        
            for (int z=1; z<=this.nbSlices; z++){
                for (int y=0; y<this.height; y++){
                    for (int x=0; x<this.width; x++){
                        int val=this.uep[offset(x,y,z)];
                        if (val!=0){
                            this.centres[val][0]+=x;
                            this.centres[val][1]+=y;
                            this.centres[val][2]+=z;
                        }
                    }
                }
                IJ.showStatus("Retrieving centres");
            }

            for (int i=0; i<this.centres.length; i++) for (int j=0; j<3; j++) if (this.IDcount[i]!=0) this.centres[i][j]=(int) (this.centres[i][j]/this.IDcount[i]+.5);
            for (int i=0; i<this.uep.length; i++) this.uep[i]=0;

            for (int i=0; i<this.centres.length; i++){
                if (this.IDcount[i]!=0){
                    this.uep[offset(this.centres[i][0],this.centres[i][1],this.centres[i][2])]=nbCentres;
                    nbCentres++;
                }
            }
            this.centres=null;
            System.gc();
        }
        IJ.showStatus("");
    }
    
    /** Used for the connexity analysis on the UEP map: retrieves the minimum value tag over all the preciding 13 neighbours of the current pixel.
     * @param initialValue current pixel value (integer).
     * @param x coordinate of the pixel for which value should be calculated.
     * @param y coordinate of the pixel for which value should be calculated.
     * @param z coordinate of the pixel for which value should be calculated.
     * @return the mimimum value, different from zero, found on the previous 13 neighbouring pixels (integer).
     */
    private int minPrevTagUEP(int initialValue, int x, int y, int z){
        int min=initialValue;
        int currPos;
        
        for (int neighbY=y-1; neighbY<=y+1; neighbY++){
            for (int neighbX=x-1; neighbX<=x+1; neighbX++){
                //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
                if (neighbX>=0 && neighbX<this.width && neighbY>=0 && neighbY<this.height && z-1>=1 && z-1<=this.nbSlices){
                    currPos=offset(neighbX, neighbY, z-1);
                    if (this.uep[currPos]!=0) min=Math.min(min, this.uep[currPos]);
                }
            }
        }
        
        for (int neighbX=x-1; neighbX<=x+1; neighbX++){
            //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
            if (neighbX>=0 && neighbX<this.width && y-1>=0 && y-1<this.height && z>=1 && z<=this.nbSlices){
                currPos=offset(neighbX, y-1, z);
                if (this.uep[currPos]!=0) min=Math.min(min, this.uep[currPos]);
            }
        }
        
        //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
        if (x-1>=0 && x-1<this.width && y>=0 && y<this.height && z>=1 && z<=this.nbSlices){
            currPos=offset(x-1, y, z);
            if (this.uep[currPos]!=0 && x>=1 && y>=0 && z>=1) min=Math.min(min, this.uep[currPos]);
        }
        
        return min;
    }
    
    /** Replaces oldVal by newVal in UEP and updates the IDcount array, containing the count of the number of pixel for a specific ID.
     * @param oldVal pixel value to be replaced.
     * @param newVal pixel value to be replacedby.
     */
    private void replaceID(int oldVal, int newVal){
        int nbFoundPix=0;
        for (int i=0; i<this.uep.length; i++){
            if (this.uep[i]==oldVal){
                this.uep[i]=newVal;
                nbFoundPix++;
            }
            if (nbFoundPix==this.IDcount[oldVal]) i=this.uep.length;
        }
        this.IDcount[oldVal]=0;
        this.IDcount[newVal]+=nbFoundPix;
    }
    
     /** Returns an ImagePlus of the Ultimate Erode Point (UEP) for the current Watershed3D object.
     * In case the UEP and EDM have not been already calculated, getUEP calculates both of them first, with or without getting the EDM filtered.
     * @param filter enables/disables the filtering of the EDM (boolean), used to minimise the number of local maxima.
     * @param title to attribute to the ImagePlus (string).
     * @return an ImagePlus of the UEP.
     */
    public ImagePlus getUEP(boolean filter, String title){
        if (this.uep==null) UEP(filter);
        return buildImg(this.uep, title);
    }
    
    /** Returns an ImagePlus of the Ultimate Erode Point (UEP) for the current Watershed3D object.
     * In case the UEP and EDM have not been already calculated, getUEP calculates both of them first, with or without getting the EDM filtered.
     * @param min minimum size for particles, used for size based filtering.
     * @param max maximum size for particles, used for size based filtering.
     * @param getCount if true, logs the number of found structures.
     * @param getResults if true, returns a ResultsTable containing the number of pixels forming each structure.
     * @param getCentres if true, adds to the ResultsTable the coordinates of the centres of  each structure. Only applies if getResults is set to true.
     * @param showImage if true, returns an image contaning the segmented particles, each carrying an intensity equal to their ID in the connexity table and to the line of the ResultsTable containing their caracteristics.
     * @param showNumbers if true, annotates the image with the particles numbers, corresponding to the line of the ResultsTable containing their caracteristics.
     */
    public void doWatershed(int min, int max, boolean getCount, boolean getResults, boolean getCentres, boolean showImage, boolean showNumbers){
        this.doWatershed=true;
        if (this.uep==null) UEP(true);
        raiseBound();
        sizeAndHistogramFiltering(min, max);
        if (getCount) IJ.log("Image: "+title+" Count: "+this.nbObj);
        if (getResults){
            if (getCentres){
                this.centres=new int [this.nbObj+1][3];
                for (int z=1; z<=this.nbSlices; z++){
                    for (int y=0; y<this.height; y++){
                        for (int x=0; x<this.width; x++){
                            int val=this.uep[offset(x,y,z)];
                            if (val!=0){
                                this.centres[val][0]+=x;
                                this.centres[val][1]+=y;
                                this.centres[val][2]+=z;

                            }
                        }
                    }
                }
                for (int i=1; i<=this.nbObj; i++) for (int j=0; j<3; j++) this.centres[i][j]=(int) (this.centres[i][j]/this.IDcount[i]+.5);
            }
            
            ResultsTable rt=new ResultsTable();
            rt.setHeading(0, "Object ne");
            rt.setHeading(1, "Voxel count");
            rt.setHeading(2, "Area ("+this.unit+"e)");
            if (this.nbSlices!=1) rt.setHeading(2, "Volume ("+this.unit+"^3)");
            if (getCentres){
                rt.setHeading(3, "Centre X");
                rt.setHeading(4, "Centre Y");
                if (this.nbSlices!=1) rt.setHeading(5, "Centre Z");
            }
            double factor=this.pixW*this.pixH*this.pixD;
            for (int i=1; i<=this.nbObj; i++){
                    rt.incrementCounter();
                    rt.setValue(1,i-1,this.IDcount[i]);
                    rt.setValue(2,i-1,this.IDcount[i]*factor);
                    if (getCentres){
                        rt.setValue(3,i-1,this.centres[i][0]);
                        rt.setValue(4,i-1,this.centres[i][1]);
                        if (this.nbSlices!=1) rt.setValue(5,i-1,this.centres[i][2]);
                    }
                    
            }
            rt.show("Results for "+title);
        }
        
        this.coord=null;
        if (showImage){
            ImagePlus img=buildImg(this.uep, "Watershed for "+title);
            if (showNumbers){
                for (int i=1; i<=this.nbObj; i++){
                    img.setSlice(this.centres[i][2]);    
                    ImageProcessor ip=img.getProcessor();
                    ip.setColor(Color.WHITE);
                    ip.drawString(i+"", this.centres[i][0]-(5*((String) (i+"")).length()/2), this.centres[i][1]+5);
                }
            }
            img.show();
            IJ.run("Fire");
        }
        
        this.uep=null;
        System.gc();
    }
    
    /** Immerges (=dilates) the UEP map and raises the bounds between particles.
     */
    private void raiseBound(){
        for (int level=this.histogram.length-1; level>0; level--){
            for (int l=0; l<this.histogram[level]; l++) immerseZeMap(level, l);
            for (int l=this.histogram[level]-1; l>=0; l--) immerseZeMap(level, l);
            IJ.showStatus("Raising bounds...");
        }
        IJ.showStatus("");    
    }
    
    /** Immerses (=dilates) the UEP map.
     *@param level level to process.
     *@param l offset of the pixel to proceed.
     */
    private void immerseZeMap(int level, int l){
        int coordOffset=this.levelStart[level]+l;
        int x=this.coord[coordOffset][0];
        int y=this.coord[coordOffset][1];
        int z=this.coord[coordOffset][2];
        int offset=offset(x,y,z);
        int[] neigh=new int [27];
        int index=0;
        int max=0;

        if (this.uep[offset]==0){
            for (int k=z-1; k<=z+1; k++){
                for (int j=y-1; j<=y+1; j++){
                    for (int i=x-1; i<=x+1; i++){
                        if ((i>=0 && i<this.width) && (j>=0 && j<this.height) && (k>=1 && k<=this.nbSlices) && !(i==x && j==y && k==z)){
                            neigh[index]=this.uep[offset(i, j, k)];
                            max=Math.max(max, neigh[index]);
                        }
                        index++;
                    }
                }
            }

            boolean isBound=false;
            for (int i=0; i<13; i++){
                if (neigh[i]!=neigh[26-i] && neigh[i]*neigh[26-i]!=0)isBound=true;
            }

            if (isBound){
                this.uep[offset]=0;
            }else{
                this.uep[offset]=max;
            }

            if (this.uep[offset]!=0){
                neigh[13]=this.uep[offset];
                for (int i=0; i<27; i++){
                    if (neigh[13]!=neigh[i] && neigh[i]!=0){
                        this.uep[offset]=0;
                        i=27;
                    }
                }
            }
        }
        IJ.showStatus("");
    }
    
    /** Performs the filtering of particles based on their sizes (minSize<size<maxSize) and renumbers the remaining particles.
     * @param minSize minimum expected particles sizes.
     * @param maxSize maximum expected particles sizes.
     */
    private void sizeAndHistogramFiltering(int minSize, int maxSize){
        int max=this.uep[0];
        for (int i=1; i<this.length; i++) max=Math.max(max, this.uep[i]);
        this.histoWater=new int[max+1];
        for (int i=0; i<this.length; i++) this.histoWater[this.uep[i]]++;
        
        this.IDcount=this.histoWater.clone();
        
        this.nbObj=0;
        for (int i=0; i<this.histoWater.length; i++){
            if (this.histoWater[i]!=0 && (this.histoWater[i]>minSize && this.histoWater[i]<maxSize)){
                if (i!=this.nbObj) replaceID(i, this.nbObj);
                this.nbObj++;
            }else{
                if (i!=0) replaceID(i, 0);
            }
        }
        this.nbObj--;
      }
    
    /** Returns the index where to find the informations corresponding to pixel (x, y, z).
     * @param x coordinate of the pixel.
     * @param y coordinate of the pixel.
     * @param z coordinate of the pixel.
     * @return the index where to find the informations corresponding to pixel (x, y, z).
     */
    private int offset(int x, int y, int z){
        if (x+y*this.width+(z-1)*this.width*this.height>=this.width*this.height*this.nbSlices){
            return this.width*this.height*this.nbSlices-1;
        }else{
            if (x+y*this.width+(z-1)*this.width*this.height<0){
                return 0;
            }else{
                return x+y*this.width+(z-1)*this.width*this.height;
            }
        }
    }
    
    /** Generates the ImagePlus based on Watershed3D object width, height and number of slices, the imput array and title.
     * @param array containing the pixels intensities (integer array).
     * @param title to attribute to the ImagePlus (string).
     */
    private ImagePlus buildImg(int[] array, String title){
        int index=0;
        double min=array[0];
        double max=array[0];
        ImagePlus img=NewImage.createImage(title, this.width, this.height, this.nbSlices, 16, 1);
        
        for (int z=1; z<=this.nbSlices; z++){
            IJ.showStatus("Creating the image...");
            img.setSlice(z);
            for (int y=0; y<this.height; y++){
                for (int x=0; x<this.width; x++){
                    int currVal=array[index];
                    min=Math.min(min, currVal);
                    max=Math.max(max, currVal);
                    img.getProcessor().putPixel(x,y, currVal);
                    index++;
                }
            }
        }
        IJ.showStatus("");
        img.setCalibration(this.cal);
        img.getProcessor().setMinAndMax(min, max);
        return img;
    }

}
