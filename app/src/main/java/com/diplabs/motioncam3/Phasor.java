package com.diplabs.motioncam3;

public class Phasor {


    @Override
    public String toString() {
        return "Phasor{" +
                "amp=" + amp +
                ", freq=" + freq +
                ", phase=" + phase +

                '}';
    }
    public String toString2() {
        return
                amp + ","+
                freq + ","+
                phase
                ;
    }
    private double amp;
    private double freq;
    private double phase;

    public double getAmp() {
        return amp;
    }

    public void setAmp(double amp) {
        this.amp = amp;
    }

    public double getFreq() {
        return freq;
    }

    public void setFreq(double freq) {
        this.freq = freq;
    }

    public double getPhase() {
        return phase;
    }

    public void setPhase(double phase) {
        this.phase = phase;
    }

    Phasor(double amp, double freq, double phase){
        this.amp = amp;
        this.freq = freq;
        this.phase = phase;
    }


  }
