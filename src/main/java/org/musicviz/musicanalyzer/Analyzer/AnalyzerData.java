package org.musicviz.musicanalyzer.Analyzer;

import java.util.Arrays;

public class AnalyzerData {
    private int sampleRate;
    private double[] leftFFTData;
    private double[] rightFFTData;

    public AnalyzerData(int sampleRate, double[] leftFFTData, double[] rightFFTData) {
        this.sampleRate = sampleRate;
        this.leftFFTData = leftFFTData;
        this.rightFFTData = rightFFTData;
    }

    public void optimize(AnalyzerData lastOne) {
        //TODO: add optimizing
    }

    private void flattenWithLastOne(AnalyzerData lastOne) {
        for (int i = 0; i < this.leftFFTData.length; i++) {
            this.leftFFTData[i] = (this.leftFFTData[i] + lastOne.leftFFTData[i]) / 2;
        }
    }

    private void boostValues() {
        for (int i = 0; i < leftFFTData.length; i++) {
            leftFFTData[i] *= 5;
        }
    }

    private void moveLowFrequenciesToTheMiddle() {
        double[] tmp = Arrays.copyOf(leftFFTData, leftFFTData.length);
        for (int i = 0; i < tmp.length; i++) {
            if (i <= 25) {
                leftFFTData[i] = tmp[99 - i];
            } else {
                leftFFTData[i] = tmp[i - 26];
            }
        }
    }

    private void makeMoreVisual() {
        double[] tmp = Arrays.copyOf(leftFFTData, leftFFTData.length);
        //double[] tmp2 = rightFFTData;
        for (int i = 1; i < tmp.length - 1; i++) {
            leftFFTData[i] = (tmp[i - 1] + tmp[i] + tmp[i] + tmp[i + 1]) / 4;
            //       rightFFTData[i] = (tmp2[i + 1] + tmp2[i] + tmp2[i] + tmp2[i + 1]) / 4;
        }
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public double[] getLeftFFTData() {
        return leftFFTData;
    }

    public void setLeftFFTData(double[] leftFFTData) {
        this.leftFFTData = leftFFTData;
    }

    public double[] getRightFFTData() {
        return rightFFTData;
    }

    public void setRightFFTData(double[] rightFFTData) {
        this.rightFFTData = rightFFTData;
    }
}
