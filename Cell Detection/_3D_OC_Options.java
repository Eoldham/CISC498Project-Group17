import ij.*;
import ij.plugin.*;
import ij.gui.*;

public class _3D_OC_Options implements PlugIn{
    public void run(String arg) {
    String[] label=new String[16];
    boolean[] state=new boolean[label.length];
    
    label[0]="Volume"; state[0]=Prefs.get("3D-OC-Options_volume.boolean", true);
    label[1]="Surface"; state[1]=Prefs.get("3D-OC-Options_surface.boolean", true);
    label[2]="Nb_of_Obj._voxels"; state[2]=Prefs.get("3D-OC-Options_objVox.boolean", true);
    label[3]="Nb_of_Surf._voxels"; state[3]=Prefs.get("3D-OC-Options_surfVox.boolean", true);
    label[4]="Integrated_Density"; state[4]=Prefs.get("3D-OC-Options_IntDens.boolean", true);
    label[5]="Mean_Gray_Value"; state[5]=Prefs.get("3D-OC-Options_mean.boolean", true);
    label[6]="Std_Dev_Gray_Value"; state[6]=Prefs.get("3D-OC-Options_stdDev.boolean", true);
    label[7]="Median_Gray_Value"; state[7]=Prefs.get("3D-OC-Options_median.boolean", true);
    label[8]="Minimum_Gray_Value"; state[8]=Prefs.get("3D-OC-Options_min.boolean", true);
    label[9]="Maximum_Gray_Value"; state[9]=Prefs.get("3D-OC-Options_max.boolean", true);
    label[10]="Centroid"; state[10]=Prefs.get("3D-OC-Options_centroid.boolean", true);
    label[11]="Mean_distance_to_surface"; state[11]=Prefs.get("3D-OC-Options_meanDist2Surf.boolean", true);
    label[12]="Std_Dev_distance_to_surface"; state[12]=Prefs.get("3D-OC-Options_SDDist2Surf.boolean", true);
    label[13]="Median_distance_to_surface"; state[13]=Prefs.get("3D-OC-Options_medDist2Surf.boolean", true);
    label[14]="Centre_of_mass"; state[14]=Prefs.get("3D-OC-Options_COM.boolean", true);
    label[15]="Bounding_box"; state[15]=Prefs.get("3D-OC-Options_BB.boolean", true);
    
    boolean closeImg=Prefs.get("3D-OC-Options_closeImg.boolean", false);
    boolean showMaskedImg=Prefs.get("3D-OC-Options_showMaskedImg.boolean", true);
    
    int dotSize=(int) Prefs.get("3D-OC-Options_dotSize.double", 5);
    int fontSize=(int) Prefs.get("3D-OC-Options_fontSize.double", 10);
    boolean showNb=Prefs.get("3D-OC-Options_showNb.boolean", true);
    boolean whiteNb=Prefs.get("3D-OC-Options_whiteNb.boolean", true);
    boolean newRT=Prefs.get("3D-OC-Options_newRT.boolean", true);
    
    String redirectTo=Prefs.get("3D-OC-Options_redirectTo.string", "none");
    
    //Manage the redirect to option
    int[] idList=WindowManager.getIDList();
    int listLength=1;
    if (idList!=null){
        listLength=idList.length+1;
    }
    String[] imgList=new String[listLength];
    imgList[0]="none";
    int index=0;
    for (int i=1; i<listLength; i++){
        imgList[i]=WindowManager.getImage(idList[i-1]).getTitle();
        if (imgList[i].equals(redirectTo)) index=i;
    }
    
    GenericDialog gd=new GenericDialog("3D-OC Set Measurements");
    gd.addMessage("Parameters to calculate:");
    gd.addCheckboxGroup(8, 2, label, state);
    gd.addMessage("");
    gd.addMessage("Image parameters:");
    gd.addCheckbox("Close_original_images_while_processing_(saves_memory)", closeImg);
    gd.addCheckbox("Show_masked_image_(redirection_requiered)", showMaskedImg);
    gd.addMessage("");
    gd.addMessage("Maps' parameters:");
    gd.addNumericField("Dots_size", dotSize, 0);
    gd.addNumericField("Font_size", fontSize, 0);
    gd.addCheckbox("Show_numbers", showNb);
    gd.addCheckbox("White_numbers", whiteNb);
    gd.addMessage("");
    gd.addMessage("ResultsTable parameters:");
    gd.addCheckbox("Store_results_within_a_table_named_after_the_image_(macro_friendly)", newRT);
    gd.addMessage("");
    gd.addChoice("Redirect_to:", imgList, imgList[index]);
    gd.showDialog();
    
    if (gd.wasCanceled()) return;
    
    Prefs.set("3D-OC-Options_volume.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_surface.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_objVox.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_surfVox.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_IntDens.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_mean.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_stdDev.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_median.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_min.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_max.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_centroid.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_meanDist2Surf.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_SDDist2Surf.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_medDist2Surf.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_COM.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_BB.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_closeImg.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_showMaskedImg.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_dotSize.double", (int) gd.getNextNumber());
    Prefs.set("3D-OC-Options_fontSize.double", (int) gd.getNextNumber());
    Prefs.set("3D-OC-Options_showNb.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_whiteNb.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_newRT.boolean", gd.getNextBoolean());
    Prefs.set("3D-OC-Options_redirectTo.string", gd.getNextChoice());
}

    
}