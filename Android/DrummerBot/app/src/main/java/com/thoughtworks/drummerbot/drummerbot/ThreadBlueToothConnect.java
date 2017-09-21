package com.thoughtworks.drummerbot.drummerbot;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Amit on 1/25/2015.
 */
public class ThreadBlueToothConnect extends Thread {
    public BluetoothSocket mmSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    public boolean torun = true;
    private Handler statusHandler;
    public DataOutputStream streamOut = null;
    private DataInputStream streamIn = null;
    private final BluetoothDevice mmDevice;
    private BaseActivity activity;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public ThreadBlueToothConnect(BluetoothDevice device, Handler _status_h, BaseActivity activity) {
        this.statusHandler = _status_h;
        this.mmDevice = device;
        this.activity = activity;

    }

    public void run() {

        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            mmSocket.connect();
            mOutputStream = mmSocket.getOutputStream();
            mInputStream = mmSocket.getInputStream();
            streamOut = new DataOutputStream(new BufferedOutputStream(mOutputStream));
            streamIn = new DataInputStream(new BufferedInputStream(mInputStream));
            sendStatusMessage("BL created connection");
        } catch (IOException connectException) {
            sendStatusMessage("BL connection fail");
            close();
            torun = false;
            return;
        }
        //TODO: design pattern
        while (torun) {
            try {
                char inputChar = (char) streamIn.readByte();
                if (inputChar == 'G') {
                    char commandChar = (char) streamIn.readByte();
                    if (commandChar == 'B') {
                        byte indexToSendByteZero = streamIn.readByte();
                        byte indexToSendByteOne = streamIn.readByte();

                        short indexToSend = (short) ((indexToSendByteOne * 128) + indexToSendByteZero);
                        sendStatusMessage("BL In: get beat " + indexToSend);
                        System.out.println("Index requested: " + indexToSend + " " + activity.beatStream.beatFrameLength);
                        if (indexToSend < activity.beatStream.beatFrameLength) {
                            setBeat(activity.beatStream.getBeat(indexToSend));
                        } else {
//                            resetDevice(activity.beatStream.beatFrameLength);
//                            setDeviceStatus(false);
                        }
                    } else if (commandChar == 'S') {
                        setDeviceStatus(activity.currentDeviceState);
                        sendStatusMessage("BL In: SET Arduino Status: " + activity.currentDeviceState);
                    }
                } else if (inputChar == 'S') {
                    char commandChar = (char) streamIn.readByte();
                    if (commandChar == 'B') {
//                        short currentPlayingBeatIndex = streamIn.readShort();
//                        sendStatusMessage("Current beat index: " + currentPlayingBeatIndex);

                    } else if (commandChar == 'S') {
                        if ((char) streamIn.readByte() == 'T') {
                            activity.currentDeviceState = true;
                        } else {
                            activity.currentDeviceState = false;
                        }
                        sendStatusMessage("Update status");
                        sendStatusMessage("BL In Status " + activity.currentDeviceState);

                    } else if (commandChar == 'R') {
                        activity.currentDeviceState = false;
                        setDeviceStatus(false);
                        sendStatusMessage("Update reset button");
                        sendStatusMessage("BL In Reset ");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sendStatusMessage("BL connection Stopped ");
        close();
        return;
    }

    public void close() {
        torun = false;
        try {
            if (streamOut != null) {
                streamOut.close();
            }
            if (streamIn != null) {
                streamIn.close();
            }
            if (mmSocket != null) {
                mmSocket.close();
            }
        } catch (IOException ioe) {
            // System.out.println("Error closing ...");
        }
    }

    //    public boolean writeCharToBTModule(byte str){
//        if( streamOut!=null){
//            try {
//                streamOut.write(str);
//                streamOut.write('\n');
//                streamOut.flush();
//            } catch (IOException e) {
//                e.printStackTrace();
//                close();
//                return false;
//            }
//        }else{
//            return false;
//        }
//        return true;
//    }
    public boolean writeStringToBTModule(String str) {
        if (streamOut != null) {
            try {
                streamOut.write(str.getBytes());
                streamOut.write('\n');
                streamOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
                close();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public boolean writeBytearrayToBTModule(byte[] barray) {
        if (streamOut != null) {
            try {
                streamOut.write(barray);
                streamOut.write('\n');
                streamOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
                close();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public boolean setDeviceStatus(boolean status) {
        if (streamOut != null) {
            try {

                streamOut.write('S');
                streamOut.write('S');
                if (status) {
                    streamOut.write('T');
                } else {
                    streamOut.write('F');
                }
                streamOut.write('\n');
                streamOut.flush();
                sendStatusMessage("BL Out: SS " + status);
            } catch (IOException e) {
                e.printStackTrace();
                close();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public boolean resetDevice(int beatFrameLength) {
        if (streamOut != null) {
            try {
                streamOut.write('S');
                streamOut.write('R');
                streamOut.write(getTwoBytesFromAnInteger(beatFrameLength));
                streamOut.write('\n');
                streamOut.flush();
                sendStatusMessage("BL Out: SR " + beatFrameLength);
            } catch (IOException e) {
                e.printStackTrace();
                close();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    private byte[] getTwoBytesFromAnInteger(int beatFrameLength) {
        byte[] result = new byte[2];
        result[0] = (byte) (beatFrameLength % 128);
        result[1] = (byte) (beatFrameLength / 128);
        return result;
    }

    public boolean setBeat(byte[] beat) {
        if (streamOut != null) {
            try {
                streamOut.write('S');
                streamOut.write('B');
                streamOut.write(beat);
                streamOut.write('\n');
                streamOut.flush();
                String message = "BL OUT: SB ";
                for (int byteCounter = 0; byteCounter < beat.length; byteCounter++) {
                    message += beat[byteCounter] + " ";
                }
                sendStatusMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
                close();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    private void sendStatusMessage(String sr) {
        Message m = statusHandler.obtainMessage();
        Bundle bl = new Bundle();
        bl.putString("message", sr);
        m.setData(bl);
        statusHandler.sendMessage(m);
    }

    public void setToRun(boolean b) {
        this.torun = b;
    }


}
