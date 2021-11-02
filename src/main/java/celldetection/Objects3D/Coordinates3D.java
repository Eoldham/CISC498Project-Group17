package celldetection.Objects3D;

public class Coordinates3D {
    public int x;
    public int y;
    public int z;

    public Coordinates3D(){}

    public Coordinates3D(int x, int y, int z){
        this.x=x;
        this.y=y;
        this.z=z;
    }
    
    public int[] getCoordinates(){
        return new int[]{x, y, z};
    }
}
