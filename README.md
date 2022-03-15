# CalciumSignal
#### An ImageJ plugin for calcium signal cell analysis
Contributors: Emily Oldham, Sean McMann, Gia Bugieda, Akshat Katoch, Emma Adelmann, Sohan Gadiraju, Jonathan Zhang


## Installation Instructions
1. If necessary, download [Python 3](https://www.python.org/downloads/) for your system. Ensure that Python has been added to PATH (there should be an option to do this during the installation process).
2. If you are a Windows user, please install [Microsoft Visual C++](https://visualstudio.microsoft.com/visual-cpp-build-tools/) 14.0 or higher (if you don't already have it on your system). Choose Desktop Environment with C++. You may be required to restart the computer after this step.
3. If you are a Linux or Mac OS user, ensure that pip is installed as your package manager for Python. You may also have to run `sudo apt-get install python3-tk` or `sudo apt-get install python-tk` before running the installation script (on a Mac, those commands are `brew install python-tk` or `brew install python3-tk`).
4. Download the zip file, unzip it inside the *Fiji.app* directory, and run "CalciumSignal Installer.py" from inside the unzipped folder. The script will create the *CalciumSignal* folder within the *plugins* folder. This is where the files used by the program will go.

## Usage
After launching Fiji, navigate to *Plugins* -> *Calcium Signal* -> *Run Calcium Signal...*.

Allow a few moments for the image registration and edge detection to complete. Then, make corrections as needed in the ROI Manager dialogue.

After this is done, the peak analysis phase will begin. You will find the peak analysis outputs in *Fiji.app/plugins/CalciumSignal/pythonscript/cell_data*.


## Credits
PoorMan3DReg by Michael Liebling
