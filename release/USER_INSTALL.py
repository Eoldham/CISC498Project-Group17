import os
import sys
import subprocess


# Assuming we are in user's Fiji.app directory, inside an unzipped folder called CS_files
print("Creating plugins/CalciumSignal directory...\n")
base = "../plugins/CalciumSignal"
os.mkdir(base)
print("Done.\n")

# Directories needed by peak finding script
print("Setting up Python environment...\n")
pythonscript = os.path.join(base, "pythonscript")
os.mkdir(pythonscript)
celldata = os.path.join(pythonscript, "cell_data")
os.mkdir(celldata)

os.rename("peakscript.py", os.path.join(pythonscript, "peakscript.py"))

subprocess.check_call([sys.executable, '-m', 'pip', 'install', '-r' 'requirements.txt'])

print("Done.\n")

# Directories needed by ROI Manager
print("Setting up additional directories...\n")
edgedata = os.path.join(base, "edge_data")
os.mkdir(edgedata)
print("Done.\n")

