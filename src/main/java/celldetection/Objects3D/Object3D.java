package celldetection.Objects3D;

import ij.measure.*;

import java.util.*;

public class Object3D {
    /**Stores coordinates and intensities of the current Object3D*/
    public Vector<Voxel> obj_voxels=new Vector<Voxel>();

    /**Vector containing the 3D coordinates of all surface pixels**/
    public Vector<Coordinates3D> surf_voxelsCoord=new Vector<Coordinates3D>();
    
    /**Mean intensity*/
    public float mean_gray;
    
    /**Median intensity*/
    public float median;
    
    /**SD of the intensity*/
    public float SD;
    
    /**Minimum intensity*/
    public int min;
    
    /**Maximum intensity*/
    public int max;
    
    /**Integrated density / summed intensity*/
    public  float int_dens=0;
    
    /**Mean distance to the surface (distance calibrated measure)*/
    public float mean_dist2surf;
    
    /**Median distance to the surface (distance calibrated measure)*/
    public float median_dist2surf;
    
    /**SD of the distance to the surface (distance calibrated measure)*/
    public float SD_dist2surf;
        
    /**Number of pixels/voxels on the surface*/
    public int surf_size;
    
    /**True calibrated surface measurement*/
    public float surf_cal;
    
     /**Coordinates of the centro�d (centroid[0:x, 1:y, 2:z]*/
    public float[] centroid;
    
    /**Coordinates of the centre of mass (c_mass[0:x, 1:y, 2:z]*/
    public float[] c_mass;
    
    /**Bounding cube top-left corner coordinates (bound_cube_TL[0:x, 1:y, 2:z])*/
    public int[] bound_cube_TL;
    
    /**Bounding cube bottom-right corner coordinates (bound_cube_BR[0:x, 1:y, 2:z])*/
    public int[] bound_cube_BR;
    
    
    /**Width of the bounding cube*/
    public int bound_cube_width;
    
    /**Height of the bounding cube*/
    public int bound_cube_height;
    
    /**Depth of the bounding cube*/
    public int bound_cube_depth;
    
    /**Calibration of the Object3D*/
    public Calibration cal;
    
    int currIndex;
    
    /** Creates a new instance of Object3D (distances will be calibrated according to the input calibration)
     *  @param cal calibration to be used for distance/volume measurements
     */
    public Object3D(Calibration cal) {
        currIndex=-1;
        this.cal=cal;
        surf_size=0;
        surf_cal=0;
    }
    
    /** Creates a new instance of Object3D
     */
    public Object3D() {
        this(new Calibration()); // use of "new Calibration()" gives 1x1x1 calibration in pixels instead of null which gives nothing...
    }
    
    /** Adds a new voxel to the specified Object3D, at the next available slot in the object.
     *  @param x specifies the x coordinate of the voxel to be added.
     *  @param y specifies the y coordinate of the voxel to be added.
     *  @param z specifies the z coordinate of the voxel to be added.
     *  @param val specifies the intensity of the voxel to be added.
     *  @param isSurf specifies if the voxel to be added is on the surface of the object.
     */
    public void addVoxel(int x, int y, int z, int val, boolean isSurf, float surf){
        currIndex++;
        //if (currIndex>size-1) throw new IllegalArgumentException("The current Object3D is already full: resize it prior to call addVoxel");
        obj_voxels.add(new Voxel(x, y, z, val, isSurf));
        if (isSurf){
            surf_voxelsCoord.add(new Coordinates3D(x, y, z));
            surf_size++;
            surf_cal+=surf;
        }
    }
    
    
    /**Calculates the statistices of the current object, once all its voxels have been collected.
     */
    public void calcStats(){
         //Initialisation of the variables containing the stats
        centroid=new float[3];
        c_mass=new float[3];
        bound_cube_TL=new int[3];
        bound_cube_BR=new int[3];
        
        min=obj_voxels.elementAt(0).val;
        max=min;
        int_dens=0;
        mean_dist2surf=0;
        median_dist2surf=0;
        SD_dist2surf=0;

        int[] coord=obj_voxels.elementAt(0).getCoordinates();
        for (int i=0; i<3; i++){
            centroid[i]=0;
            c_mass[i]=0;
            bound_cube_TL[i]=coord[i];
            bound_cube_BR[i]=bound_cube_TL[i];
        }
        
        //Generation of the statistics
        for (int i=0; i<obj_voxels.size(); i++){
            Voxel currVox=obj_voxels.elementAt(i);
            int_dens+=currVox.val;
            min=Math.min(min, currVox.val);
            max=Math.max(max, currVox.val);
            coord=currVox.getCoordinates();
            for (int j=0; j<3; j++){
                centroid[j]+=coord[j];
                c_mass[j]+=coord[j]*currVox.val;
                bound_cube_TL[j]=Math.min(bound_cube_TL[j], coord[j]);
                bound_cube_BR[j]=Math.max(bound_cube_BR[j], coord[j]);
            }
        }
        
        
        bound_cube_width=bound_cube_BR[0]-bound_cube_TL[0]+1;
        bound_cube_height=bound_cube_BR[1]-bound_cube_TL[1]+1;
        bound_cube_depth=bound_cube_BR[2]-bound_cube_TL[2]+1;
        
        mean_gray=int_dens/obj_voxels.size();
        
        //medianTmp will temporarely store only the intensity values
        float[] medianTmp=new float[obj_voxels.size()];
        for (int i=0; i<obj_voxels.size(); i++) medianTmp[i]=obj_voxels.elementAt(i).val;
        median=median(medianTmp);
        
        SD=0;
        if (obj_voxels.size()!=1){
            for (int i=0; i<obj_voxels.size(); i++) SD+=(obj_voxels.elementAt(i).val-mean_gray)*(obj_voxels.elementAt(i).val-mean_gray); // faster than Math.pow(x, 2)
            SD=(float) Math.sqrt(SD/(obj_voxels.size()-1));
        }
        
        for (int i=0; i<3; i++){
            centroid[i]/=obj_voxels.size();
            c_mass[i]/=int_dens;
        }
        
        //mean, SD and med distance to surface
        float[] dist2surfArray=new float[surf_size];
        int index=0;
        for (int i=0; i<obj_voxels.size(); i++){
            Voxel currVoxel=obj_voxels.elementAt(i);
            if (currVoxel.isSurf){
                dist2surfArray[index]=(float) Math.sqrt(cal.pixelWidth*cal.pixelWidth*(currVoxel.x-centroid[0])*(currVoxel.x-centroid[0])+cal.pixelHeight*cal.pixelHeight*(currVoxel.y-centroid[1])*(currVoxel.y-centroid[1])+cal.pixelDepth*cal.pixelDepth*(currVoxel.z-centroid[2])*(currVoxel.z-centroid[2]));
                mean_dist2surf+=dist2surfArray[index];
                index++;
            }
        }
        mean_dist2surf/=surf_size;
        SD_dist2surf=0;
        if (surf_size!=1){
            for (int i=0; i<surf_size; i++) SD_dist2surf+=Math.pow(dist2surfArray[i]-mean_dist2surf, 2);
            median_dist2surf=median(dist2surfArray);
            SD_dist2surf=(float) Math.sqrt(SD_dist2surf/(surf_size-1));
        }
        
    }
    
    /**Calculates the med value of a float array.
     * @param array input float array.
     * @return the med value of the input float array (float).
     */
    public float median(float[] array){
        float med=0;
        int index=0;
                
        Arrays.sort(array);
        
        //If the number of values is odd, returns the central value, otherwise computes the mean of the two boarding values.
        if ((float) array.length/2-(int) array.length/2==0){
            index=((int) array.length/2)-1;
            med=(array[index]+array[index+1])/2;
            
        }else{
            index=((int) (array.length+1)/2)-1;
            med=array[index];
        }
        
        return med;
    }
}
