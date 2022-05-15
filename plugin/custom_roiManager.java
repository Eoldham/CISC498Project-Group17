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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.sun.java.accessibility.util.AWTEventMonitor.addKeyListener;

public class custom_roiManager extends PlugInFrame implements ActionListener {

    private final String PYTHONSCRIPT_PATH = "plugins/CalciumSignal/pythonscript";

    Panel panel;
    RoiManager rm;
    int cellMin;
    int cellMax;
    JFormattedTextField minField;
    JFormattedTextField maxField;
    Set<Roi> allRois = new HashSet<Roi>();

    custom_roiManager(){
        super("Custom RoiManager");
        rm = new RoiManager();
        cellMin = 20;
        cellMax = 200;


        ImageJ ij = IJ.getInstance();
        addKeyListener(ij);
        WindowManager.addWindow(this);
        //setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
        setLayout(new BorderLayout());

        panel = new Panel();
        addTextFields();
        addButton("Multi Measure");

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
        if (command.equals("Update Cells")){
            updateCellSize(minField.getText(),maxField.getText());
        }else if (command.equals("Multi Measure")){
            rm.runCommand("multi-measure");

            //Gets active table and saves
            String path =  "plugins/CalciumSignal/pythonscript/cell_data/realResults.csv";
            ResultsTable results = ij.measure.ResultsTable.getResultsTable();
            try {
                results.saveAs(path);
            } catch (IOException f) {
                f.printStackTrace();
            }

            rm.close();
            setVisible(false);
            peakFinding();
        }
    }

    public void addRoi(Roi roi){
        rm.addRoi(roi);
        rm.runCommand("Show All");
    }

    public void peakFinding(){
        /*
        -- PEAK FINDING --
         */
        try {
            // Attempt to find the preferred command or path for python 3
            String systemPath = System.getenv("PATH");
            String[] pathLines = systemPath.split(":");
            String exePath = "python";
            String os = System.getProperty("os.name");

            if (os.contains("Windows")) {
                for (String entry : pathLines) {
                    boolean pyPath = entry.contains("python") || entry.contains("python3");

                    if (pyPath) {
                        if (entry.contains("python3")) {
                            exePath = entry.substring(entry.indexOf(System.getProperty("file.separator")) + 1);
                        }
                    }
                }
            } else {
                String[] bin = new File("/usr/bin").list();
                for (String cmdName : bin) {
                    if (cmdName.equals("python") || cmdName.equals("python3")) {
                        exePath = cmdName;
                    }
                }
                String[] local = new File("/usr/local/bin").list();
                for (String cmdName : local) {
                    if (cmdName.equals("python") || cmdName.equals("python3")) {
                        // Prefer local distribution (/usr/local/bin)...use full path
                        exePath = "/usr/local/bin/" + cmdName;
                    }
                }
            }

            // RELATIVE TO LOCATION OF FIJI EXECUTABLE
            ProcessBuilder processBuilder = new ProcessBuilder(exePath, PYTHONSCRIPT_PATH + "/peakscript.py");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            BufferedReader errout = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;

            while ((line = errout.readLine()) != null) {
                IJ.log(line);
            }

            // Use for debugging only

            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line2;

            while ((line2 = input.readLine()) != null) {
                IJ.log(line2);
            }


        } catch (Exception ex) {
            IJ.log(ex.getMessage());
        }
    }

}

