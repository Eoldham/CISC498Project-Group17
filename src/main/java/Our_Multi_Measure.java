import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.List;
import java.util.zip.*;
import javax.swing.table.*;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.PlugInFrame;
import java.awt.datatransfer.*;

/** Bob Dougherty.  This is a copy of Wayne Rasband's ROI Manager with
 one new method added: multi() performs measurements for several ROI's in a stack
 and arranges the results with one line per slice.  By constast, the measure() method
 produces several lines per slice.  The results from multi() may be easier to import into
 a spreadsheet program for plotting or additional analysis.  This capability was
 requested by Kurt M. Scudder

 Version 0 6/24/2002
 Version 1 6/26/2002 Updated to Wayne's version of ImageJ 1.28k
 Version 2 11/24/2002 Made some changes suggested by Tony Collins
 Version 3 7/20/2004  Added "Add Particles"
 Version 3.1 7/21 Fixed bugs spotted by Andreas Schleifenbaum
 Version 3.2 3/12/2005 Updated save method.  Requires ImageJ 1.33 for IJ.saveAs.
 Version 4   1/13/2006 Applied many enhancements, including JTable, provided by Ulrik Stervbo.
 Added option for labeling slices.

 */
/*	License:
 Copyright (c) 2002, 2006, OptiNav, Inc.
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 Neither the name of OptiNav, Inc. nor the names of its contributors
 may be used to endorse or promote products derived from this software
 without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class Our_Multi_Measure implements PlugIn {
    MultiMeasure mm;

    public void run(String arg) {
        if (IJ.versionLessThan("1.33"))return;
        mm = new MultiMeasure();
    }
}

/**
 */
class MultiMeasure extends PlugInFrame implements ActionListener, ItemListener,
        ClipboardOwner, KeyListener, Runnable {

    Panel panel;
    static Frame instance;
    //java.awt.List list;
    //Hashtable rois = new Hashtable();
    JTable table;
    MultiMeasureTableModel tmodel;
    Checkbox coordinates, center, slices;
    CheckboxGroup labelType;
    boolean done = false;
    Canvas previousCanvas = null;
    Thread thread;


    public MultiMeasure() {
        super("Multi Measure");
        if (instance!=null) {
            instance.toFront();
            return;
        }
        instance = this;
        setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
        //int rows = 28;//was 25
        //list = new List(rows, true);
        //list.add("012345678901234567");
        //list.addItemListener(this);
        //add(list);
        int twidth = 150;
        int theight = 450;

        tmodel = new MultiMeasureTableModel();
        table = new JTable(tmodel);
        table.setPreferredScrollableViewportSize(new Dimension(twidth, theight));
        table.setShowGrid(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        // Set the width of the first column
        table.getColumnModel().getColumn(0).setPreferredWidth(25);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);

        ListSelectionModel rowSM = table.getSelectionModel();
        rowSM.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                if (lsm.isSelectionEmpty()) {// Do nothing
                } else {
                    int selectedRow = lsm.getMinSelectionIndex();
                    restore(selectedRow);
                }
            }
        });

        panel = new Panel();
        panel.setLayout(new GridLayout(21, 1, 1, 1));// was GridLayout(16, 1, 1, 1))
        addButton("Add <SP>");
        addButton("Add+Draw<CR>");
        addButton("Add Particles");
        addButton("Update");
        addButton("Delete");
        addButton("Open");
        addButton("Open All");
        addButton("Save");
        addButton("Toggle Select All");// was 'Select All'
        addButton("Measure");
        addButton("Multi");
        addButton("Draw");
        addButton("Fill");
        addButton("Label All ROIs");
        addButton("Copy List");

        //Checkboxes

        labelType = new CheckboxGroup();
        panel.add(new Label("Labels:"));

        center = new Checkbox("Center");
        center.setCheckboxGroup(labelType);
        panel.add(center);

        coordinates = new Checkbox("Coord.");
        coordinates.setCheckboxGroup(labelType);
        panel.add(coordinates);
        center.setState(Prefs.get("multimeasure.center", true));
        coordinates.setState(!Prefs.get("multimeasure.center", true));

        panel.add(new Label("Multi Option:"));
        slices = new Checkbox("Label Slices");
        panel.add(slices);
        slices.setState(Prefs.get("multimeasure.slices", false));


        add(panel);

        pack();
        //list.delItem(0);
        GUI.center(this);
        show();
        thread = new Thread(this, "Multi_Measure");
        thread.start();
    }

    public void run() {
        while (!done) {
            try {Thread.sleep(500);}
            catch(InterruptedException e) {}
            ImagePlus imp = WindowManager.getCurrentImage();
            if (imp != null){
                ImageWindow win = imp.getWindow();
                ImageCanvas canvas = win.getCanvas();
                if (canvas != previousCanvas){
                    if(previousCanvas != null)
                        previousCanvas.removeKeyListener(this);
                    canvas.addKeyListener(this);
                    previousCanvas = canvas;
                }
            }
        }
    }

    public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        done = true;
    }



    void addButton(String label) {
        Button b = new Button(label);
        b.addActionListener(this);
        panel.add(b);
    }

    public void actionPerformed(ActionEvent e) {
        String label = e.getActionCommand();
        if (label==null)
            return;
        String command = label;
        if (command.equals("Add <SP>"))
            add();
        if (command.equals("Add+Draw<CR>"))
            addAndDraw();
        if (command.equals("Add Particles"))
            addParticles();
        else if (command.equals("Update"))
            updateActiveRoi();
        else if (command.equals("Delete"))
            delete();
        else if (command.equals("Open"))
            open(null);
        else if (command.equals("Open All"))
            openAll();
        else if (command.equals("Save"))
            save();
        else if (command.equals("Toggle Select All"))// was 'Select All'
            selectAll();
        else if (command.equals("Measure"))
            measure();
        else if (command.equals("Multi"))
            multi();
        else if (command.equals("Draw"))
            draw();
        else if (command.equals("Fill"))
            fill();
        else if (command.equals("Label All ROIs"))
            labelROIs();
        else if (command.equals("Copy List"))
            copyList();
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange()==ItemEvent.SELECTED
                && WindowManager.getCurrentImage()!=null) {
            int index = 0;
            try {index = Integer.parseInt(e.getItem().toString());}
            catch (NumberFormatException ex) {}
            restore(index);
        }
    }

    boolean add() {
        ImagePlus imp = getImage();
        if (imp==null)
            return false;
        Roi roi = imp.getRoi();
        if (roi==null) {
            error("The active image does not have an ROI.");
            return false;
        }
        String type = null;
        switch (roi.getType()) {
            case Roi.RECTANGLE: type ="R"; break;
            case Roi.OVAL: type = "O"; break;
            case Roi.POLYGON: type = "PG"; break;
            case Roi.FREEROI: type = "FH"; break;
            case Roi.TRACED_ROI: type = "T"; break;
            case Roi.LINE: type = "L"; break;
            case Roi.POLYLINE: type = "PL"; break;
            case Roi.FREELINE: type = "FL"; break;
        }
        if (type==null)
            return false;
        Rectangle r = roi.getBoundingRect();
        //String label = type+" ("+(r.x+r.width/2)+","+(r.y+r.height/2)+")";

        String label = null;
        if(center.getState())
            label = type+(r.x+r.width/2)+"-"+(r.y+r.height/2);
        else
            label =  type+".x"+ (r.x)+".y"+(r.y)+".w"+(r.width)+".h"+(r.height);
        //list.add(label);
        //rois.put(label, roi.clone());
        tmodel.addRoi(label, roi.clone());

        return true;
    }
    boolean addParticles() {
        ImagePlus imp = getImage();
        if (imp==null)
            return false;
        ResultsTable rt = Analyzer.getResultsTable();
        if(rt == null){
            IJ.showMessage("Add Particles requres Analyze -> Analyze Particles to \n"+
                    "be run first with Record Stats selected.");
            return false;
        }
        int nP = rt.getCounter();
        if(nP == 0)
            return false;
        int xCol = rt.getColumnIndex("XStart");
        int yCol = rt.getColumnIndex("YStart");
        if((xCol == ResultsTable.COLUMN_NOT_FOUND)||(yCol == ResultsTable.COLUMN_NOT_FOUND)){
            IJ.showMessage("Add Particles requres Analyze -> Analyze Particles to \n"+
                    "be run first with Record Stats selected.");
            return false;
        }
        ImageProcessor ip = imp.getProcessor();
        int tMin = (int)ip.getMinThreshold();
        for (int i = 0; i < nP; i++){
            Wand w = new Wand(ip);
            int xStart = (int)rt.getValue("XStart", i);
            int yStart = (int)rt.getValue("YStart", i);
            if (tMin==ip.NO_THRESHOLD)
                w.autoOutline(xStart, yStart);
            else
                w.autoOutline(xStart, yStart, (int)tMin, (int)ip.getMaxThreshold());
            if (w.npoints>0) {
                Roi roi = new PolygonRoi(w.xpoints, w.ypoints, w.npoints, Roi.TRACED_ROI);
                Rectangle r = roi.getBoundingRect();

                String label = null;
                if(center.getState())
                    label = "PG"+(r.x+r.width/2)+"-"+(r.y+r.height/2);
                else
                    label =  "PG"+".x"+ (r.x)+".y"+(r.y)+".w"+(r.width)+".h"+(r.height);
                //list.add(label);
                //rois.put(label, roi.clone());
                tmodel.addRoi(label, roi.clone());
            }
        }
        return true;
    }

    void addAndDraw() {
        boolean roiadded = add();
        if (roiadded) {
            //list.select(//list.getItemCount()-1);
            int i = table.getRowCount() - 1;
            table.setRowSelectionInterval(i,i);
            draw();
        }
        IJ.run("Restore Selection");
    }

    boolean delete() {
        if(table.getRowCount() == 0)
            // was if (list.getItemCount()==0)
            return error("The ROI list is empty.");
        int index[] = table.getSelectedRows(); // was list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI in the list must be selected.");
        for (int i=index.length-1; i>=0; i--) {
            String label = (String)tmodel.getValueAt(index[i],1);
            //rois.remove(label);
            //list.delItem(index[i]);
            tmodel.removeRoi(index[i]);
        }
        return true;
    }

    boolean restore(int index) {
        //String label = list.getItem(index);
        //String label = (String)tmodel.getValueAt(index,1);
        //Roi roi = (Roi)rois.get(label);
        Roi roi = (Roi)tmodel.getValueAt(index,2);
        ImagePlus imp = getImage();
        if (imp==null)
            return error("No image selected.");
        Rectangle r = roi.getBoundingRect();
        if (r.x+r.width>imp.getWidth() || r.y+r.height>imp.getHeight())
            return error("This ROI does not fit the current image.");
        imp.setRoi(roi);
        return true;
    }
    /* These open and save methods are replaced by the methods of the integrated ROI manager of ImageJ.
     * They provide better functionality and should ease merging this manager into ImageJ
     */
/*	void open() {
		Macro.setOptions(null);
		OpenDialog od = new OpenDialog("Open ROI...", "");
		String directory = od.getDirectory();
		String name = od.getFileName();
		String label;

		if(name.endsWith(".roi")){ label = name.substring(0, name.lastIndexOf(".roi")); }
		else{ label = name; }

		if (name==null)
			return;
		String path = directory + name;
		Opener o = new Opener();
		Roi roi = o.openRoi(path);
		if (roi!=null) {
			//list.add(name);
			//rois.put(name, roi);
			tmodel.addRoi(label, roi);
		}
	}


	void openAll() {
		Macro.setOptions(null);
		Macro.setOptions(null);
		OpenDialog od = new OpenDialog("Select a file in the folder...", "");
		if (od.getFileName()==null) return;
		String dir  = od.getDirectory();
		String[] files = new File(dir).list();
		String name, label;
		if (files==null) return;
		for (int i=0; i<files.length; i++) {
			File f = new File(dir+files[i]);
			if (!f.isDirectory() && files[i].endsWith(".roi")) {
				Roi roi = new Opener().openRoi(dir+files[i]);
				if (roi!=null) {
					name = files[i];
					if(name.endsWith(".roi")){ label = name.substring(0, name.lastIndexOf(".roi")); }
					else{ label = name; }
					//list.add(files[i]);
					//rois.put(files[i], roi);
					tmodel.addRoi(label, roi);
				}
			}
		}
	}

	boolean save() {
		if(table.getRowCount() == 0)
			// was if (list.getItemCount()==0)
			return error("The ROI list is empty.");
		int index[] = table.getSelectedRows();//list.getSelectedIndexes();
		if (index.length==0)
			return error("At least one ROI in the list must be selected.");
		String name = (String)tmodel.getValueAt(index[0],1);
		// was String name = list.getItem(index[0]);
		Macro.setOptions(null);
		SaveDialog sd = new SaveDialog("Save ROI...", name, ".roi");
		name = sd.getFileName();
		if (name == null)
			return false;
		String dir = sd.getDirectory();
		for (int i=0; i<index.length; i++) {
			if (restore(index[i])) {
				if (index.length>1)
					name = (String)tmodel.getValueAt(index[i],1) + ".roi";
				//was list.getItem(index[i])+".roi";
				//IJ.run("ROI...", "path='"+dir+name+"'");
				IJ.saveAs("Selection", dir+name);
			} else
				break;
		}
		return true;
	}*/

    /*
     * These open save methods are taken from the integrated ROI manager of ImageJ written by Wayne Rasband
     */
    void open(String path) {
        Macro.setOptions(null);
        String name = null;
        if (path==null) {
            OpenDialog od = new OpenDialog("Open Selection(s)...", "");
            String directory = od.getDirectory();
            name = od.getFileName();
            if (name==null)
                return;
            path = directory + name;
        }
        //if (Recorder.record) Recorder.record("roiManager", "Open", path);
        if (path.endsWith(".zip")) {
            openZip(path);
            return;
        }
        Opener o = new Opener();
        if (name==null) name = o.getName(path);
        Roi roi = o.openRoi(path);
        if (roi!=null) {
            if (name.endsWith(".roi"))
                name = name.substring(0, name.length()-4);
            name = getUniqueName(name);
            //list.add(name);
            //rois.put(name, roi);
            tmodel.addRoi(name, roi);
        }
    }
    /*
     * Quite a few things have been changed by Ulrik Stervbo in this method compared to the original
     * The original method is found below this method
     *
     * This is what I've done:
     *      Only read .roi files in the zip-file
     *      Do not delete existing entrie in the manager
     *
     * If we delete the current entries one cannot open rois from different sources (co-workers and such)
     */
    void openZip(String path) {
        ZipInputStream in = null;
        ByteArrayOutputStream out;
        boolean noFilesOpened = true; // we're pessimistic and expect that the zip file dosent contain any .roi
        try {
            in = new ZipInputStream(new FileInputStream(path));
            byte[] buf = new byte[1024];
            int len;
            // The original while was: while(true) do something which is not very good
            ZipEntry entry = in.getNextEntry();
            while (entry!=null) {
                /* If we try to open a non-roi file an error is thrown and nothing is opened into
                 * the Roi manager - not a very nice thing to do! Of course we'd expect the zip file to
                 * contain nothing but .roi files, but who knows what users do?
                 *
                 * The easy solution to this problem is to open only .roi files in the zip file.
                 * Another solution is to play with the getRoi of the RoiDecoder. This solution is more
                 * difficult and may not better in a general perspective.
                 *
                 * At any rate I'm a lazy b'stard - I only open files if they end with '.roi'
                 */

                String name = entry.getName();
                if (name.endsWith(".roi")) {
                    out = new ByteArrayOutputStream();
                    while ((len = in.read(buf)) > 0)
                        out.write(buf, 0, len);
                    out.close();
                    byte[] bytes = out.toByteArray();
                    RoiDecoder rd = new RoiDecoder(bytes, name);
                    Roi roi = rd.getRoi();
                    if (roi!=null) {
                        name = name.substring(0, name.length()-4);

                        name = getUniqueName(name);
                        tmodel.addRoi(name, roi);
                        noFilesOpened = false; // We just added a .roi
                    }
                }
                entry = in.getNextEntry();
            }
            in.close();
        } catch (IOException e) { error(e.toString()); }
        if(noFilesOpened){ error("This ZIP archive does not appear to contain \".roi\" files"); }
    }

    /*
     * The original openZip methig before Ulrik started playing and messing about
     */
/*    void openZip(String path) {
        ZipInputStream in = null;
        ByteArrayOutputStream out;
        try {
            in = new ZipInputStream(new FileInputStream(path));
            byte[] buf = new byte[1024];
            int len;
            boolean firstTime = true;
            while (true) {
                ZipEntry entry = in.getNextEntry();
                if (entry==null)
                    {in.close(); return;}
                String name = entry.getName();
                if (!name.endsWith(".roi")) {
                    error("This ZIP archive does not appear to contain \".roi\" files");
                }
                out = new ByteArrayOutputStream();
                while ((len = in.read(buf)) > 0)
                    out.write(buf, 0, len);
                out.close();
                byte[] bytes = out.toByteArray();
                RoiDecoder rd = new RoiDecoder(bytes, name);
                Roi roi = rd.getRoi();
                if (roi!=null) {
                    if (firstTime) {
                        if (list.getItemCount()>0) delete(true);
                        if (canceled)
                            {in.close(); return;}
                        firstTime = false;
                    }
                    if (name.endsWith(".roi"))
                        name = name.substring(0, name.length()-4);
                    name = getUniqueName(name);
                    list.add(name);
                    rois.put(name, roi);
                }
            }
        } catch (IOException e) {
            error(""+e);
        }
    }*/

    void openAll() {
        IJ.setKeyUp(KeyEvent.VK_ALT);
        Macro.setOptions(null);
        // The original code contained a bug around here
        // my solution was to remove
        // String dir  = IJ.getDirectory("Open All...");
        // and add:
        OpenDialog od = new OpenDialog("Select a file in the folder...", "");
        String dir  = od.getDirectory();
        if (dir==null) return;
        String[] files = new File(dir).list();
        if (files==null) return;
        for (int i=0; i<files.length; i++) {
            File f = new File(dir+files[i]);
            if (!f.isDirectory() && files[i].endsWith(".roi")) {
                Roi roi = new Opener().openRoi(dir+files[i]);
                if (roi!=null) {
                    String name = files[i];
                    if (name.endsWith(".roi"))
                        name = name.substring(0, name.length()-4);
                    name = getUniqueName(name);
                    //list.add(name);
                    //rois.put(name, roi);
                    tmodel.addRoi(name, roi);
                }
            }
        }
    }

    String getUniqueName(String name) {
        String name2 = name;
        int n = 1;
        int rownum = tmodel.getRowNumber(name2,1);

        while(rownum != -1){
            name2 = name+"-"+n;
            n++;
            rownum = tmodel.getRowNumber(name2,1);
        }
/*		int n = 1;
		Roi roi2 = (Roi)rois.get(name2);
		while (roi2!=null) {
			roi2 = (Roi)rois.get(name2);
			if (roi2!=null)
				name2 = name+"-"+n;
			n++;
			roi2 = (Roi)rois.get(name2);
		}*/
        return name2;
    }

    boolean save() {
        if (table.getRowCount() == 0) // was if (list.getItemCount()==0)
            return error("The list is empty."); // was "The selection list is empty"
        int indexes[] = table.getSelectedRows();//list.getSelectedIndexes();
        // I dont get this - first we say: if nothing is selected, say so and then we select all items.
        // what is the point in that?
        // if (indexes.length==0)
        //	indexes = getAllIndexes();
        if(indexes.length==0){ error("At least one ROI must be selected from the list."); }
        if (indexes.length>1)
            return saveMultiple(indexes, null);
        String name = (String)tmodel.getValueAt(indexes[0],1); // was list.getItem(indexes[0]);
        Macro.setOptions(null);
        SaveDialog sd = new SaveDialog("Save Selection...", name, ".roi");
        String name2 = sd.getFileName();
        if (name2 == null)
            return false;
        // The user has changed the name of the file so the name of the ROI should also change!
        String dir = sd.getDirectory();
        String newName;
        // If the new name ends with '.roi' it must be removed
        if(name2.endsWith(".roi")){ newName = name2.substring(0, name2.length()-4); }
        else{ newName = name2; }
        int rownum = tmodel.getRowNumber(name,1);
        // If nothing was found a -1 is returned - better make some use of it!
        if(rownum == -1){ return error("No entry matching " + name + " was found."); }

        // OK - we're all good! Update the entry
        tmodel.updateRoi(rownum, newName, tmodel.getValueAt(rownum, 2));

        // Before I changed things is looked like this
        //Roi roi = (Roi)rois.get(name);
        //rois.remove(name);
        //if (!name2.endsWith(".roi")) name2 = name2+".roi";
        //String newName = name2.substring(0, name2.length()-4);
        //rois.put(newName, roi);
        //roi.setName(newName);
        //list.replaceItem(newName, indexes[0]);
        if (restore(indexes[0]))
            IJ.run("Selection...", "path='"+dir+newName+".roi'");
        return true;
    }

    boolean saveMultiple(int[] indexes, String path) {
        Macro.setOptions(null);
        if (path==null) {
            SaveDialog sd = new SaveDialog("Save ROIs...", "RoiSet", ".zip");
            String name = sd.getFileName();
            if (name == null)
                return false;
            String dir = sd.getDirectory();
            path = dir+name;
        }
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            RoiEncoder re = new RoiEncoder(out);
            String name = "";
            Roi roi = null;
            for (int i=0; i<indexes.length; i++) {
                //String label = list.getItem(indexes[i]);
                //Roi roi = (Roi)rois.get(label);
                //if (!label.endsWith(".roi")) label += ".roi";
                //zos.putNextEntry(new ZipEntry(label));
                // Get the name of the roi and the roi
                name = (String)tmodel.getValueAt(indexes[i],1);
                roi = (Roi)tmodel.getValueAt(indexes[i],2);
                zos.putNextEntry(new ZipEntry(name + ".roi"));
                re.write(roi);
                out.flush();
            }
            out.close();
        }
        catch (IOException e) {
            error(""+e);
            return false;
        }
        //if (Recorder.record) Recorder.record("roiManager", "Save", path);
        return true;
    }

    void selectAll(){
        int selcount = table.getSelectedRowCount();  // was list.getItemCount();
        int totcount = table.getRowCount();

        if(selcount < totcount){ table.selectAll(); }
        else{ table.clearSelection(); }

        //  		for (int i=0; i<count; i++) {
        //  			if (!list.isIndexSelected(i))
        //  				allSelected = false;
        //  		}
        //  		for (int i=0; i<count; i++) {
        //  			if (allSelected)
        //  				list.deselect(i);
        //  			else
        //  				list.select(i);
        //  		}
    }


    boolean measure() {
        ImagePlus imp = getImage();
        if (imp==null)
            return false;
        int[] index = table.getSelectedRows();//list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI must be selected from the list.");

        int setup = IJ.setupDialog(imp, 0);
        if (setup==PlugInFilter.DONE)
            return false;
        int nSlices = setup==PlugInFilter.DOES_STACKS?imp.getStackSize():1;
        int currentSlice = imp.getCurrentSlice();
        for (int slice=1; slice<=nSlices; slice++) {
            imp.setSlice(slice);
            for (int i=0; i<index.length; i++) {
                if (restore(index[i]))
                    IJ.run("Measure");
                else
                    break;
            }
        }
        imp.setSlice(currentSlice);
        if (index.length>1)
            IJ.run("Select None");
        return true;
    }

    boolean multi() {
        Prefs.set("multimeasure.center", center.getState());
        Prefs.set("multimeasure.slices", slices.getState());
        ImagePlus imp = getImage();
        if (imp==null)
            return false;
        int[] index = table.getSelectedRows();//list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI must be selected from the list.");

        int setup = IJ.setupDialog(imp, 0);
        if (setup==PlugInFilter.DONE)
            return false;
        int nSlices = setup==PlugInFilter.DOES_STACKS?imp.getStackSize():1;
        int currentSlice = imp.getCurrentSlice();

        int measurements = Analyzer.getMeasurements();
        Analyzer.setMeasurements(measurements);
        Analyzer aSys = new Analyzer(); //System Analyzer
        ResultsTable rtSys = Analyzer.getResultsTable();
        ResultsTable rtMulti = new ResultsTable();
        Analyzer aMulti = new Analyzer(imp,Analyzer.getMeasurements(),rtMulti); //Private Analyzer

        for (int slice=1; slice<=nSlices; slice++) {
            int sliceUse = slice;
            if(nSlices == 1)sliceUse = currentSlice;
            imp.setSlice(sliceUse);
            rtMulti.incrementCounter();
            int roiIndex = 0;
            if(slices.getState())
                rtMulti.addValue("Slice",sliceUse);
            for (int i=0; i<index.length; i++) {
                if (restore(index[i])){
                    roiIndex++;
                    Roi roi = imp.getRoi();
                    ImageStatistics stats = imp.getStatistics(measurements);
                    aSys.saveResults(stats,roi); //Save measurements in system results table;
                    for (int j = 0; j < ResultsTable.MAX_COLUMNS; j++){
                        float[] col = rtSys.getColumn(j);
                        String head = rtSys.getColumnHeading(j);
                        if ((head != null)&&(col != null))
                            rtMulti.addValue(head+roiIndex,rtSys.getValue(j,rtSys.getCounter()-1));
                    }
                }
                else
                    break;
            }
            aMulti.displayResults();
            aMulti.updateHeadings();
        }

        imp.setSlice(currentSlice);
        if (index.length>1)
            IJ.run("Select None");
        return true;
    }

    boolean copyList(){
        String s="";
        if(table.getRowCount() == 0)
            //was if (list.getItemCount()==0)
            return error("The ROI list is empty.");
        int index[] = table.getSelectedRows();//list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI in the list must be selected.");
        int numPad = numMeasurements() - 2;
        for (int i=0; i<index.length; i++) {
            if (restore(index[i])) {
                // I dont understand the purpose of this if-statement
                // seems to me that one cannot copy one single item
                //if (index.length>1){
                s +=  (String)tmodel.getValueAt(index[i],1);
                // was list.getItem(index[i]);
                if (i < (index.length-1) )
                    s += "\t";
                for (int j = 0; j < numPad; j++)
                    s += "\t";
                //}

            } else
                break;
        }
        Clipboard clip = getToolkit().getSystemClipboard();
        if (clip==null) return error("System clipboard missing");
        StringSelection cont = new StringSelection(s);
        clip.setContents(cont,this);
        return true;
    }

    public void lostOwnership (Clipboard clip, Transferable cont) {}

    int numMeasurements(){
        ResultsTable rt = Analyzer.getResultsTable();
        String headings = rt.getColumnHeadings();
        int len = headings.length();
        if (len == 0) return 0;
        int count = 0;
        for (int i = 0; i < len; i++)
            if (headings.charAt(i) == '\t') count++;
        return count;
    }

    boolean fill() {
        int[] index = table.getSelectedRows(); // was list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI must be selected from the list.");
        ImagePlus imp = WindowManager.getCurrentImage();
        Undo.setup(Undo.COMPOUND_FILTER, imp);
        for (int i=0; i<index.length; i++) {
            if (restore(index[i])) {
                IJ.run("Fill");
                IJ.run("Select None");
            } else
                break;
        }
        Undo.setup(Undo.COMPOUND_FILTER_DONE, imp);
        return true;
    }

    boolean draw() {
        int[] index = table.getSelectedRows(); // was list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI must be selected from the list.");
        ImagePlus imp = WindowManager.getCurrentImage();
        Undo.setup(Undo.COMPOUND_FILTER, imp);
        for (int i=0; i<index.length; i++) {
            if (restore(index[i])) {
                IJ.run("Draw");
                IJ.run("Select None");
            } else
                break;
        }
        Undo.setup(Undo.COMPOUND_FILTER_DONE, imp);
        return true;
    }

    public boolean labelROIs(){
        tmodel.reindexRois();
        table.selectAll(); // Select everything - otherwise the numbers in the table will not match the numbers put on the image as labels
        int[] index = table.getSelectedRows(); // was list.getSelectedIndexes();
        if (index.length==0)
            return error("At least one ROI must be selected from the list.");
        ImagePlus imp = WindowManager.getCurrentImage();
        Undo.setup(Undo.COMPOUND_FILTER, imp);

        IJ.run("Clear Results");

        for (int i=0; i<index.length; i++) {
            if (restore(index[i])) {
                IJ.run("Measure");
                IJ.run("Label");
                IJ.run("Select None");
            } else
                break;
        }
        table.clearSelection();
        Undo.setup(Undo.COMPOUND_FILTER_DONE, imp);
        return true;
    }

    public boolean updateActiveRoi(){
        int[] index = table.getSelectedRows();
        ImagePlus imp = getImage();
        if (imp==null)
            return false;
        Roi roi = imp.getRoi();
        if (roi==null){ return error("The active image does not have an ROI."); }

        if(index.length == 0){
            return error("At least one ROI must be selected from the list.");
        }
        else if(index.length > 1){
            return error("No more than one ROI at the time can be updated.");
        }
        else if(index.length == 1){
            /*
             * This is not the nicest way (for the user) to experience an update but it's dead easy
             * and should in far the most cases ensure that all ROIs have unique labels (and thus
             * unique file names) - this is not nessecary the case when for instance the result of
             * a Particle Analysis is added to the ROI manager
             */
            // First delete the current entry
            delete();
            // Then add the active ROI
            add();
            return true;
        }
        else{ return false; }
    }

    int[] getAllIndexes() {
        int count = table.getRowCount(); // was list.getItemCount();
        int[] indexes = new int[count];
        for (int i=0; i<count; i++)
            indexes[i] = i;
        return indexes;
    }

    ImagePlus getImage() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp==null) {
            error("There are no images open.");
            return null;
        } else
            return imp;
    }

    boolean error(String msg) {
        new MessageDialog(this, "Multi Measure", msg);
        return false;
    }

    public void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID()==WindowEvent.WINDOW_CLOSING) {
            instance = null;
        }
    }

    /** Returns a reference to the ROI Manager
     or null if it is not open. */
    public static MultiMeasure getInstance() {
        return (MultiMeasure)instance;
    }

    /** Returns the ROI Hashtable. */
    public Hashtable getROIs() {
        // This is here only for possible backwards compability
        Hashtable rois = new Hashtable();
        int size = table.getRowCount();
        String label = "";
        Object roi = null;
        for(int i = 0; i <= size; i++){
            label = (String)tmodel.getValueAt(i,1);
            roi = tmodel.getValueAt(i,2);
            rois.put(label, roi);
        }
        return rois;
    }

    /** Returns the ROI list. */
    //public List getList() {
    //	return list;
    //}


    public void keyPressed(KeyEvent e) {
        final int SPACE = 32;
        final int CR = 10;
        int keyCode = e.getKeyCode();
        if (keyCode == SPACE)
            add();
        else if (keyCode == CR)
            addAndDraw();
    }

    public void keyReleased (KeyEvent e) {}
    public void keyTyped (KeyEvent e) {}


}

/**
 */
class MultiMeasureTableModel extends AbstractTableModel{
    protected static int NUM_COLUMNS = 2;
    protected static int START_NUM_ROWS = 0;
    protected int nextEmptyRow = 0;
    protected int numRows = 0;

    static final public String LABELINDEX = "#";
    static final public String LABEL = "Label";
    static final public String ROI  = "Roi";

    protected Vector data = null;

    public MultiMeasureTableModel(){
        data = new Vector();
    }

    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return LABELINDEX;
            case 1:
                return LABEL;
            case 2:
                return ROI;
        }
        return "";
    }

    public int getColumnCount() {
        return NUM_COLUMNS;
    }

    public int getRowCount() {
        if (numRows < START_NUM_ROWS) {
            return START_NUM_ROWS;
        } else {
            return numRows;
        }
    }

    public Object getValueAt(int row, int column){
        try {
            MultiMeasureRoi mmr = (MultiMeasureRoi)data.elementAt(row);
            switch(column){
                case 0:
                    return new Integer(Integer.toString(mmr.getLabelindex()));
                case 1:
                    return mmr.getLabel();
                case 2:
                    return mmr.getRoi();
            }
        } catch (Exception e) {
            //TODO catch the exception
        }
        return "";
    }

    public int getRowNumber(Object obj, int column){
        int foundindex = -1;
        Object searchobj = obj;
        Object currobj = null;

        for(int i = 0; i < numRows; i++){
            currobj = getValueAt(i, column);
            if(searchobj.equals(currobj)){
                foundindex = i;
                break;
            }
        }

        return foundindex;
    }

    //public int getColumnNumber(String colname){ return -1; }

    public void updateRoi(int index, String label, Object roi){
        MultiMeasureRoi currmmr = null;
        currmmr = (MultiMeasureRoi)data.elementAt(index);
        currmmr.setLabel(label);
        currmmr.setRoi(roi);
        data.setElementAt(currmmr,index);

        fireTableRowsUpdated(index, index);
    }

    public void addRoi(String label, Object roi){
        int lastelement = data.size() - 1;
        MultiMeasureRoi lastmmr = null;
        int lastlabelindex = 0;
        int index = -1;

        if(lastelement >= 0){
            lastmmr = (MultiMeasureRoi)data.elementAt(lastelement);
            lastlabelindex = lastmmr.getLabelindex();
        }

        if (numRows <= nextEmptyRow) {
            //add arow
            numRows++;
        }

        index = nextEmptyRow;

        nextEmptyRow++;

        data.add(new MultiMeasureRoi(lastlabelindex + 1, label, roi));

        fireTableRowsInserted(index, index);
    }

    public void reindexRois(){
        int dl = data.size();
        Vector tmp = new Vector();
        MultiMeasureRoi currmmr = null;

        for(int i = 0; i < dl; i++){
            currmmr = (MultiMeasureRoi)data.elementAt(i);
            currmmr.setLabelindex(i + 1);
            data.setElementAt(currmmr,i);
        }
        fireTableRowsUpdated(dl, dl);
    }

    public void removeRoi(int index){
        data.remove(index);
        // Delete a row
        numRows--;
        fireTableRowsDeleted(index, index);
    }
}

class MultiMeasureRoi{
    private int _labelindex = 0;
    private String _label = "";
    private Object _roi = null;

    public MultiMeasureRoi(int labelindex, String label){
        _labelindex = labelindex;
        _label = label;
    }

    public MultiMeasureRoi(int labelindex, String label, Object roi){
        _labelindex = labelindex;
        _label = label;
        _roi = roi;
    }

    public MultiMeasureRoi(String label){
        _label = label;
    }

    public MultiMeasureRoi(String label, Object roi){
        _label = label;
        _roi = roi;
    }

    public int getLabelindex(){ return _labelindex; }
    public String getLabel(){ return _label; }
    public Object getRoi(){ return _roi; }

    public void setLabelindex(int labelindex){ _labelindex = labelindex; }
    public void setLabel(String label){ _label = label; }
    public void setRoi(Object roi){ _roi = roi; }
}
