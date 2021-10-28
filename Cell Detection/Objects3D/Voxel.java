package Objects3D;

public class Voxel extends Coordinates3D{
    public int val;
    public boolean isSurf;

    public Voxel(int x, int y, int z, int val, boolean isSurf){
        this.x=x;
        this.y=y;
        this.z=z;
        this.val=val;
        this.isSurf=isSurf;
    }
    
    public Voxel(Coordinates3D coord3D, int val, boolean isSurf){
        x=coord3D.x;
        y=coord3D.y;
        z=coord3D.z;
        this.val=val;
        this.isSurf=isSurf;
    }



    public Coordinates3D getCoodinates3D(){
        return new Coordinates3D(x, y, z);
    }

}
