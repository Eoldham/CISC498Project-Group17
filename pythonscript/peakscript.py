import math
import pandas as pd
import numpy as np
import csv
import os
import peakutils
import matplotlib.pyplot as plt
from scipy.signal import find_peaks, peak_prominences, filtfilt, butter

"""Reads in all csv files in folder and creates an array of pandas dataframes
   returns array of dfs"""
def read_csvs():
    cellData = []
    #when we have a folder of, files, read in from directory path
    path = "./cell_data/"
    for filename in os.listdir(path):
        df = pd.read_csv((path+filename))
        cellData.append(df)
    return cellData


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
def normalizeData(base1, df):
    y = df["Mean1"] #list of intensities
    base2 = peakutils.baseline(y, math.floor(base1))
    normalizedData = y - base2
    #print(normalizedData)
    findNormalizedBase(normalizedData, df) #new normbaseline column created
    return base2

"""Smooths data points so signal is more clean
Calls findNormalizedBase to find baseline of smoothed data
"""
def smoothDataPoints(normalBase, df):
    data = df["Mean1"].values.tolist()
    c, d = butter(3, 0.1, 'lowpass') #.3 for less smoothed data
    filteredLowPass = filtfilt(c, d, data)
    newbase = peakutils.baseline(filteredLowPass, math.floor(normalBase))
    findNormalizedBase(filteredLowPass-newbase,df)
    return filteredLowPass, newbase


def plotPeakCellData(x,y,df):
    plt.figure()
    plt.xlabel("Video Frame (#)")
    plt.ylabel("Normalized Calcium Intensity")
    plt.title("Calcium Intensity Over Time; Normalized and Smoothed Data with Peaks")
    plt.plot(y)
    plt.plot(x,y[x],"x")
    plt.plot(df["normalbaseline"],color='red',label="baseline")
    plt.show()

def plotOriginalCellData(y):
    plt.figure()
    plt.title("Original Calcium Intensity Over Time")
    plt.xlabel("Video Frame (#)")
    plt.ylabel("Calcium Intensity")
    plt.plot(y)
    plt.show()


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

def plotPeaksOnOriginalData(peaks,data):
    plt.figure()
    plt.title("Original Calcium Intensity Over Time with Peaks")
    plt.xlabel("Video Frame (#)")
    plt.ylabel("Calcium Intensity")
    plt.plot(data)
    for idx in peaks:
        plt.plot(idx, data[idx],"x")
    plt.show()

def main():
    #read in files
    cellDataList = read_csvs()
    for cell in cellDataList:
        videoFrames = len(cell)
        average = cell.Mean1.mean()
        originalIntensities = cell["Mean1"].values.tolist()
        # find baseline
        firstBaseline = findBaseline(average, list(originalIntensities), cell)
        #normalize Data - don't need to use for now
        #normalBase = normalizeData(firstBaseline, cell)
        smoothedData, smoothedBase = smoothDataPoints(firstBaseline,cell)
        #plot graph
        refinedData = smoothedData - smoothedBase

        peaks, properties = find_peaks(refinedData, prominence=(5))
        print(peaks)
        plotOriginalCellData(originalIntensities)
        plotPeakCellData(peaks,refinedData,cell)
        print(originalIntensities)
        peakIndices = matchRefinedPeakToActualPeak(peaks,originalIntensities)
        plotPeaksOnOriginalData(peakIndices,originalIntensities)


if __name__ == "__main__":
    main()