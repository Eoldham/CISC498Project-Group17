import ij.IJ;
import ij.ImageJ;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.sun.java.accessibility.util.AWTEventMonitor.addKeyListener;

public class custom_roiManager extends PlugInFrame implements ActionListener, KeyListener {

    Panel panel;
    RoiManager rm;
    int cellMin;
    int cellMax;
    private final String PYTHONSCRIPT_PATH;
    JFormattedTextField minField;
    JFormattedTextField maxField;
    Set<Roi> allRois = new HashSet<Roi>();

    custom_roiManager(String scriptPath){
        super("Custom RoiManager");
        PYTHONSCRIPT_PATH = scriptPath;
        rm = new RoiManager();
        cellMin = 20;
        cellMax = 200;


        ImageJ ij = IJ.getInstance();
        addKeyListener(ij);
        WindowManager.addWindow(this);
        //setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
        setLayout(new BorderLayout());

        panel = new Panel();
//        addButton("Add [t]");
//        addButton("Delete");
        addButton("Measure");
        addTextFields();

        add(panel);

        pack();
        //list.delItem(0);
        GUI.center(this);
        show();


    }

    void addTextFields(){
        NumberFormat format = NumberFormat.getInstance();
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setMinimum(0);
        formatter.setMaximum(200);
        formatter.setAllowsInvalid(false);
        formatter.setCommitsOnValidEdit(true);

        JLabel min = new JLabel("Min Cell Size: ");
        minField = new JFormattedTextField(formatter);
        minField.setColumns(10);
        minField.setActionCommand("cellMin");
        minField.addActionListener(this);

        JLabel max = new JLabel("Max Cell Size: ");
        maxField = new JFormattedTextField(formatter);
        maxField.setColumns(10);
        maxField.setActionCommand("cellMax");
        maxField.addActionListener(this);

        panel.add(min);
        panel.add(minField);
        panel.add(max);
        panel.add((maxField));

        addButton("Update Cells");

    }

    void addButton(String label) {
        Button b = new Button(label);
        b.addActionListener(this);
        b.addKeyListener(IJ.getInstance());
        panel.add(b);
    }

    void updateCellSize(String minSize, String maxSize){
        int min = Integer.parseInt(minSize);
        int max = Integer.parseInt(maxSize);

        cellMin = min;
        cellMax = max;

        Roi [] rois = rm.getRoisAsArray();
        allRois.addAll(Arrays.asList(rois));

        rm.close();
        RoiManager newRm = new RoiManager();
        rm = newRm;

        for(Roi roi: allRois){
            allRois.add(roi);
            if (roi.getBounds().getHeight() >= min && roi.getBounds().getWidth() >= min && roi.getBounds().getWidth() <= max && roi.getBounds().getWidth() <= max){
                addRoi(roi);
            }
        }


    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals("Add [t]")){
            rm.runCommand("add");
        }else if (command.equals("Delete")) {
            //rm.runCommand("Delete");
        }else if (command.equals("Measure")) {
            rm.runCommand("multi-measure");
            String path = PYTHONSCRIPT_PATH + "/cell_data/realResults.csv";
            ResultsTable results = ij.measure.ResultsTable.getResultsTable();
            try {
                results.saveAs(path);
            } catch (IOException f) {
                f.printStackTrace();
            }

            rm.close();
        }else if (command.equals("Update Cells")){
            updateCellSize(minField.getText(),maxField.getText());
        }
    }

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyChar();
        if (key == 'Q') //Q
            rm.runCommand("Delete");
    }
    public void keyReleased (KeyEvent e) {}
    public void keyTyped (KeyEvent e) {}

    public void addRoi(Roi roi){
        rm.addRoi(roi);
        rm.runCommand("Show All");
    }

}

