package com.thoughtworks.drummerbot.networking;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.thoughtworks.drummerbot.drummerbot.BaseActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class TcpClientThread implements Runnable {
    private Socket socket = null;
    public Thread thread = null;
    private DataOutputStream streamOut = null;
    private DataInputStream streamIn = null;
    public ObjectOutputStream objectoutstream = null;
    private String servername;
    private int serverPort;
    private Handler status_h;
    private String TAG = "Tcp Client Thread";
    private boolean toRun = true;
    private BaseActivity activity;

    public TcpClientThread(String _serverName, int _serverPort, Handler _status_h, BaseActivity _activity) {
        this.servername = _serverName;
        this.serverPort = _serverPort;
        this.status_h = _status_h;
        activity = _activity;
        start();
    }

    @Override
    public void run() {
        try {
            socket = new Socket(servername, serverPort);
            Log.d(TAG, "new socket created");
            streamOut = new DataOutputStream(socket.getOutputStream());
            streamIn = new DataInputStream(socket.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
            sendStatusMessage("Not able to connect TCP server");
            Log.d(TAG, "not able to create socket");
            toRun = false;
            return;
        }

        sendStatusMessage("Server Connected");

        while (toRun) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                toRun = false;
            }

            String incomingMessage = getNextStringFromInputStream();
            sendStatusMessage("TCP In: " + incomingMessage);
            handleIncomingMessage(incomingMessage);
        }
        sendStatusMessage("TCP connection Stopped ");
        activity.mBlueToothConnectThread.setDeviceStatus(false);
        stop();
    }

    //TODO: Command Pattern

    public void handleIncomingMessage(String message) {
        if (message == null) {
            toRun = false;
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(message);
            String command = jsonObject.getString("command");
            if (command.equals("set")) {
                processSetCommand(jsonObject);
            } else if (command.equals("get")) {
                processGetCommand(jsonObject);
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processSetCommand(JSONObject jsonObject) {
        try {
            System.out.println("SET Command: " + jsonObject.toString());
            String requestType = jsonObject.getString("requestType");
            if (requestType.equals("beat")) {
                JSONArray jsonArrayOfBeats = jsonObject.getJSONArray("data");
                int[][] beats = new int[jsonArrayOfBeats.length()][4];
                for (int i = 0; i < jsonArrayOfBeats.length(); ++i) {
                    JSONArray jsonArray = jsonArrayOfBeats.getJSONArray(i);
                    for (int counter = 0; counter < 4; counter++) {
                        beats[i][counter] = jsonArray.optInt(counter);
                    }
                }

                activity.beatStream.setBeats(beats);
                if (activity.mBlueToothConnectThread != null) {
                    activity.mBlueToothConnectThread.resetDevice(activity.beatStream.beatFrameLength);
                }
                sendCommandAsJSON(getJSONObjectForCommand("set", "reset", 1));

                //TODO: reset arduino

            } else if (requestType.equals("reset")) {
                if (activity.mBlueToothConnectThread != null) {
                    activity.mBlueToothConnectThread.setDeviceStatus(false);
                }
                sendCommandAsJSON(getJSONObjectForCommand("set", "reset", 0));
                sendCommandAsJSON(getJSONObjectForCommand("get", "beat", 0));

            } else if (requestType.equals("state")) {
                if (activity.mBlueToothConnectThread != null) {
                    boolean desiredState = jsonObject.getBoolean("data");
                    activity.mBlueToothConnectThread.setDeviceStatus(desiredState);
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void processGetCommand(JSONObject jsonObject) {
        try {
            String requestType = jsonObject.getString("requestType");

            if (requestType.equals("state")) {
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void sendCommandAsJSON(JSONObject jsonObjectForCommand) {

        if (jsonObjectForCommand != null) {
            send(jsonObjectForCommand.toString());
        }
    }

    public JSONObject getJSONObjectForCommand(String command, String requestType, int data) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("command", command);
            jsonObject.put("requestType", requestType);
            jsonObject.put("data", new Integer(data));
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getJSONObjectForCommand(String command, String requestType, boolean data) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("command", command);
            jsonObject.put("requestType", requestType);
            jsonObject.put("data", new Boolean(data));
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        toRun = false;
        try {
            if (streamOut != null) {
                streamOut.close();
            }
            if (streamIn != null) {
                streamIn.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ioe) {
            // System.out.println("Error closing ...");
        }
    }

    public synchronized void send(String string) {
        if (streamOut == null) {
            return;
        }
        try {
            streamOut.write((string + '\n').getBytes());
            streamOut.flush();
            sendStatusMessage("TCP Out: " + string);
        } catch (IOException ex) {
            stop();
        }
    }

    private String getNextStringFromInputStream() {
        String nextString = null;
        try {
            System.out.println("About to readString");
            nextString = streamIn.readLine();
            System.out.println("Incoming string: " + nextString);
        } catch (SocketException e) {
            e.printStackTrace();
            stop();
        } catch (IOException ex) {
            stop();
        }
        return nextString;
    }

    public void setToRun(boolean bl) {
        this.toRun = bl;
    }

    private void sendStatusMessage(String sr) {
        Message m = status_h.obtainMessage();
        Bundle bl = new Bundle();
        bl.putString("message", sr);
        m.setData(bl);
        status_h.sendMessage(m);
    }
}
