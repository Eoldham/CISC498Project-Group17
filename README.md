# CalciumSignal
#### An ImageJ plugin for calcium signal cell analysis
Contributors: Emily Oldham, Sean McMann, Gia Bugieda, Akshat Katoch, Emma Adelmann, Sohan Gadiraju


## Installation Instructions
1. If necessary, download [Python 3](https://www.python.org/downloads/) for your system. Ensure that ```python``` has been added to PATH (there should be an option to do this during the installation process).
2. Download the zip file, unzip it inside the *Fiji.app* directory, and run USER_INSTALL.py (from a command line, navigate to the directory and type ```python USER_INSTALL.py```) from inside the unzipped folder. The script will create the *CalciumSignal* folder within the *plugins* folder. This is where the files used by the program will go.

## Usage
After launching Fiji, navigate to *Plugins* -> *Calcium Signal* -> *Run Calcium Signal...*.

Allow a few moments for the image registration and edge detection to complete. Then, make corrections as needed in the ROI Manager dialogue.

After this is done, the peak analysis phase will begin. You will find the peak analysis outputs in *Fiji.app/plugins/CalciumSignal/pythonscript/cell_data*.


## Credits
PoorMan3DReg by Michael Liebling
