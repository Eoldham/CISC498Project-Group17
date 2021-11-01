import ij.ImageJ;
import java.io.File;

public class Test {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.getProperties().setProperty("plugins.dir", System.getProperty("user.dir")+File.separator+"dist"+File.separator);
        ImageJ ij=new ImageJ();
        
        ij.exitWhenQuitting(true);
    }
}
