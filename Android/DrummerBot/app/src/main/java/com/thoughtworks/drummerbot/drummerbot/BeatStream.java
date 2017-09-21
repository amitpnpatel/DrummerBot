package com.thoughtworks.drummerbot.drummerbot;

import java.io.BufferedOutputStream;
import java.nio.ByteBuffer;
import java.sql.SQLOutput;

public class BeatStream {
    public short[] timeForNextBeat;//limitation on higher limit
    public short[] amplitudeForNextBeat;
    public short[] drumNumber;
    public int lastIndexSent = 0;
    public int beatFrameLength;
    private int[][] beats;

    public BeatStream() {
        setToDefault();
    }

    public void setToDefault() {
        this.timeForNextBeat = new short[1000];
        this.amplitudeForNextBeat = new short[1000];
        this.drumNumber = new short[1000];
        this.lastIndexSent = 0;
        for (int ii = 0; ii < timeForNextBeat.length; ii++) {
            timeForNextBeat[ii] = 5000;
            amplitudeForNextBeat[ii] = 4;
            drumNumber[ii] = 0;
        }
    }

    public byte[] getBeat(short index) {
        byte[] result = new byte[8];
        short beatIndex = index;
        result[0] = (byte) (index % 128);
        result[1] = (byte) (index / 128);

        System.out.println("Result[0], Result[1] : " + result[0] + ", " + result[1]);

        result[2] = (byte) (timeForNextBeat[beatIndex] % 128);
        result[3] = (byte) (timeForNextBeat[beatIndex] / 128);
        result[4] = (byte) (amplitudeForNextBeat[beatIndex] % 128);
        result[5] = (byte) (amplitudeForNextBeat[beatIndex] / 128);
        result[6] = (byte) (drumNumber[beatIndex] % 128);
        result[7] = (byte) (drumNumber[beatIndex] / 128);
        lastIndexSent = beatIndex;
        return result;
    }

    public void setBeat(int[] beat) {
        int index = beat[0];
        this.timeForNextBeat[index] = (short) beat[1];
        this.amplitudeForNextBeat[index] = (short) beat[2];
        this.drumNumber[index] = (short) beat[3];
    }

    public void setBeats(int[][] beats) {
        beatFrameLength = beats.length;
        for (int counter = 0; counter < beats.length; counter++) {
            setBeat(beats[counter]);
        }
    }
}
