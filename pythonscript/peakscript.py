import math
import pandas as pd
import numpy as np
import csv
import os
import peakutils
import matplotlib.pyplot as plt
import matplotlib
import sys
from scipy.signal import find_peaks, peak_prominences, filtfilt, butter
from matplotlib.backend_bases import MouseButton


"""Reads in all csv files in folder and creates an array of pandas dataframes
   returns array of dfs"""
def read_csvs():
    cellData = []
    # when we have a folder of, files, read in from directory path
    filename = "realResults.csv" #file outputted by ROI manager
    path = "plugins/CalciumSignal/pythonscript/cell_data/"
    df = pd.read_csv((path + filename))
    df = df.filter(regex="Mean")
    df = df.dropna(axis="columns")  ##eliminate columns with NaN values
    return df

"""Stores all relevant graph data to a csv for the ImageJ plugin to use"""
def write_csv(df):
    # when we have a folder of, files, read in from directory path
    path = "plugins/CalciumSignal/pythonscript/cell_data/"
    df.to_csv(os.path.join(path, "graph_data.csv"))
    return

""" Stores peak locations at the correct frame # in dataframe.
If there is a peak, value will be 1, if no peak detected, value is -1."""
def writePeaksToDf(peakIndx,df, cellnum):
    peaks = [-1] * len(df)
    colName = "Cell" + str(cellnum) + "_Peaks"
    for peakFrame in peakIndx:
        peaks[peakFrame] = 1
    newDf = df.copy()
    newDf[colName] = peaks
    return newDf

"""Finds first rough baseline from data
   Looks at all elements below the average and averages them
   returns the base"""
def findBaseline(avg, intensities, cellDf):
    for elem in intensities:
        if elem > avg:
            intensities.remove(elem)
    base = sum(intensities) / len(intensities)
    cellDf['baseline'] = base
    return (base)


"""Creates a new df column with normalized data"""
"""TODO: maybe later changed so can use findBaseline function instead"""
"""For now, only use when passing in normalized data"""
def findNormalizedBase(ndata, df):
    #ndata -> normalized data
    average = ndata.mean()
    df["ndata"] = ndata
    baselineArray = df["ndata"].values.tolist()
    for elem in baselineArray:
        if elem > average:
            baselineArray.remove(elem)
    newBase = sum(baselineArray) / len(baselineArray)
    df['normalbaseline'] = newBase



"""ONLY USE WHEN NOT SMOOTHING THE DATA"""
"""Normalizes the baseline for the original data"""
def normalizeData(base1, df, cellMean):
    y = df[cellMean] #list of intensities
    base2 = peakutils.baseline(y, math.floor(base1))
    normalizedData = y - base2
    findNormalizedBase(normalizedData, df) #new normbaseline column created
    return base2


"""Smooths data points so signal is more clean
Calls findNormalizedBase to find baseline of smoothed data
"""
def smoothDataPoints(normalBase, df, cellMean):
    data = df[cellMean].values.tolist()
    c, d = butter(3, 0.1, 'lowpass') #.3 for less smoothed data
    filteredLowPass = filtfilt(c, d, data)
    newbase = peakutils.baseline(filteredLowPass, math.floor(normalBase))
    findNormalizedBase(filteredLowPass-newbase,df)
    return filteredLowPass, newbase

"""
This function is for testing only
"""
def plotPeakCellData(x,y,df):
    plt.figure()
    plt.xlabel("Video Frame (#)")
    plt.ylabel("Normalized Calcium Intensity")
    plt.title("Calcium Intensity Over Time; Normalized and Smoothed Data with Peaks")
    plt.plot(y)
    plt.plot(x,y[x],"x")
    plt.plot(df["normalbaseline"],color='red',label="baseline")


def plotOriginalCellData(y, figure):
    plt.title("Original Calcium Intensity Over Time")
    plt.xlabel("Video Frame (#)")
    plt.ylabel("Calcium Intensity")
    figure.gca().plot(y)


def matchRefinedPeakToActualPeak(peaks, originalData):
    # since data was smoothed when peaks were detected, look for highest point around frame
    # where peak was detected in the original data based on an error deviation
    peakIndices = []
    for peak in peaks:
        highPointIndex = peak
        for value in range(peak - 30, peak + 30):
            if originalData[value] > originalData[highPointIndex]:
                highPointIndex = value
        peakIndices.append(highPointIndex)
    return peakIndices

##GLOBAL VARIABLES###
cellData = read_csvs()
cellID = 0
fig = plt.figure()
max = len(cellData.columns)
##GLOBAL VARIABLES###

def plotPeaksOnOriginalData(peaks,data,cellnum,figure):
    plt.title("Original Calcium Intensity Over Time with Peaks")
    plt.xlabel("Video Frame (#)")
    plt.ylabel("Calcium Intensity")

    for idx in peaks:
        figure.gca().plot(idx, data[idx],"x")

def replot_cell(figure):
    print("HERE")
    figure.canvas.manager.set_window_title("Cell %d" %(cellID + 1))
    peakCol = "Cell" + str(cellID + 1) + "_Peaks"
    dataCol = "Mean" + str(cellID + 1)

    plotOriginalCellData(cellData[dataCol].values.tolist(), figure)
    if peakCol in cellData.columns:
            for i in range(0,len(cellData[peakCol])):
                if cellData[peakCol][i] == 1:
                    print("peak")
                    figure.gca().plot(i,cellData[dataCol][i],marker="x",color="red")

    #cellData = writePeaksToDf(peakIndices,cellData,cellID)

    #return cellData

def plot_cell(figure):
    global cellID
    global cellData

    # we're really starting from Cell 0 because of indices. but it's easier for the client to start from 1
    figure.canvas.manager.set_window_title("Cell %d" %(cellID + 1))
    # figure.canvas.toolbar.pack_forget()
    cell = cellData.columns[cellID]
    videoFrames = len(cellData)
    average = cellData[cell].mean()
    originalIntensities = cellData[cell].values.tolist()
    # find baseline
    firstBaseline = findBaseline(average, list(originalIntensities), cellData)
    # normalize Data - don't need to use for now
    # normalBase = normalizeData(firstBaseline, cell, cellMean)
    smoothedData, smoothedBase = smoothDataPoints(firstBaseline,cellData,cell)
    # plot graph
    refinedData = smoothedData - smoothedBase

    peaks, properties = find_peaks(refinedData, prominence=(5))
    #plotOriginalCellData(originalIntensities, figure)
    #plotPeakCellData(peaks,refinedData,cell)
    peakIndices = matchRefinedPeakToActualPeak(peaks,originalIntensities)
    #plotPeaksOnOriginalData(peakIndices,originalIntensities,cellID,figure)
    cellData = writePeaksToDf(peakIndices,cellData,cellID)
    #return cellData
"""
    peakCol = "Cell" + str(cellID + 1) + "_Peaks"
    dataCol = "Mean" + str(cellID + 1)

    if peakCol in cellData.columns:
        for i in range(0,len(cellData[peakCol])):
            if cellData[peakCol][i] == 1:
                plt.plot(i,cellData[dataCol][i],marker="x",color="red")
"""



# key event listener for switching between cell graphs
def on_press(event):
    global cellData
    global cellID
    global fig
    global max

    # right arrow key to advance, left to go back (WASD scheme used as a backup)
    # graphs should wrap if you go past the last cell or before the first one -- hence, "carousel view"
    if event.key in ['right', 'left', 'd', 'a']:
        if event.key == 'right' or event.key == 'd':
            cellID += 1
            if cellID >= max:
                cellID = 0
        if event.key == 'left' or event.key == 'a':
            if cellID > 0:
                cellID -= 1
            elif cellID <= 0:
                cellID = max - 1

        fig.clear()
        event.canvas.figure.clear()
        replot_cell(event.canvas.figure)
        event.canvas.draw()


def on_click(event):
    print("on_click")
    if event.button is MouseButton.LEFT:
        print("LEFT")  # normal click
        # call add peak function
        user_addPeak(event)
    elif event.button is MouseButton.RIGHT:
        print("Right")  # right click - remove
        # call remove peak function
        user_removePeak(event)


def user_removePeak(event):
    global cellData
    global fig

    print("remove peak from graph function")
    ##register x,y coordinate of mouse click
    #determine closest peak in df (based on frame range) to mouse click
    #remove this point from df (make it -1)
    #replot any peaks
    peakCol = "Cell" + str(cellID + 1) + "_Peaks"
    dataCol = "Mean" + str(cellID + 1)
    if event.inaxes:  # checks to see if user clicked on the plotted graph
        ax = event.inaxes  # the axes instance
        x = int(event.xdata)
        y = int(event.ydata)
        print('data coords %f %f' % (x, y))

        removeIdx = x
        diff = x
        for data in range(x - 10, x + 10):  # original was 30
            try:
                if cellData[peakCol][data] == 1:
                    if abs(cellData[dataCol][data] - cellData[dataCol][removeIdx]) < diff:
                        removeIdx = data
                        diff = abs(cellData[dataCol][data] - cellData[dataCol][removeIdx])
            except:
                continue  # ignore indexes that are out of range

        #cellData[peakCol][removeIdx] = -1
        cellData.loc[removeIdx,peakCol] = -1

        fig.clear()
        event.canvas.figure.clear()
        replot_cell(event.canvas.figure)

        event.canvas.draw()

        # print("DONE")
        plt.show()


def user_addPeak(event):
    global cellData
    global fig

    print("add peak to graph function")
    #following two lines should be in another function before carosel view
    peakCol = "Cell" + str(cellID + 1) + "_Peaks"
    dataCol = "Mean" + str(cellID + 1)
    if event.inaxes: # checks to see if user clicked on the plotted graph
        ax = event.inaxes  # the axes instance
        x = int(event.xdata)
        y = int(event.ydata)
        print('data coords %f %f' % (x, y))

        maxValIdx = x
        for data in range(x - 10, x + 10):  # original was 30
            try:
                if cellData[dataCol][data] > cellData[dataCol][maxValIdx]:
                    maxValIdx = data
            except:
                continue  # ignore indexes that are out of range
        #print(cellData[peakCol][maxValIdx])
        cellData.loc[maxValIdx,peakCol] = 1
        print(cellData.loc[maxValIdx, peakCol])
        print("x: " + str(maxValIdx))
        print("y: " + str(cellData[dataCol][maxValIdx]))

        fig.clear()
        event.canvas.figure.clear()
        replot_cell(event.canvas.figure)
        event.canvas.draw()

        # print("DONE")
        plt.show()


def main():
    # uncomment below line for debugging only (and be sure to close stdout at the end)
    # this redirects print() output to output.txt, which you will find in the Fiji.app directory after program finishes
    sys.stdout = open('output.txt', 'w')

    # sorry about the globals. it's for a good cause, I promise.
    global cellData
    global cellID
    global fig

    fig.canvas.mpl_connect('key_press_event', on_press)
    fig.canvas.mpl_connect('button_press_event', on_click)

    numColumns = len(cellData.columns)
    for col in range(0,numColumns):
        cellID = col
        plot_cell(fig)
    #plt.show()
    cellID = 0
    # write to csv at the end (after window is closed)!
    write_csv(cellData)
    path = "plugins/CalciumSignal/pythonscript/cell_data/"
    cellData = pd.read_csv((path + "graph_data.csv"))
    replot_cell(fig)
    plt.show()

    # uncomment below for debugging only (also see output.txt at the start of main)
    sys.stdout.close()

if __name__ == "__main__":
    main()