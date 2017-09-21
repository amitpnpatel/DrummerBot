package com.thoughtworks.drummerbot.drummerbot;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.CharacterPickerDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.thoughtworks.drummerbot.networking.TcpClientThread;

import java.util.ArrayList;
import java.util.Set;

public class BaseActivity extends AppCompatActivity {
    BaseActivity activity = this;
    public ThreadBlueToothConnect mBlueToothConnectThread;
    public TcpClientThread tcpClientThread;
    private BluetoothAdapter blueAdapter = null;
    private BluetoothDevice bdevice = null;
    Handler statusHandler;
    TextView textView;
    EditText editText;
    Button btSend, btStartStop, btReset, btClearList;
    ListView lv_debug;
    public Boolean currentDeviceState = false;
    public BeatStream beatStream = new BeatStream();
    private SharedPreferences sharedPreferences;
    public ArrayList<String> debugCommentList = new ArrayList<>();


    //IP
    int portNumberOfServer = 5001;
    String ipAddressOfServer = "10.131.126.43";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        sharedPreferences = getApplicationContext().getSharedPreferences("myPreference", MODE_PRIVATE);

        ipAddressOfServer = sharedPreferences.getString("serverIpAddress", "10.131.126.43");
        portNumberOfServer = sharedPreferences.getInt("serverPortNumber", 5001);
        initializeDebugListView();

        textView = (TextView) findViewById(R.id.textview);
        editText = (EditText) findViewById(R.id.edittext);
        btClearList = (Button) findViewById(R.id.bt_clearList);

        btClearList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugCommentList = new ArrayList<String>();
                initializeDebugListView();
            }
        });


        btStartStop = (Button) findViewById(R.id.bt_startStop);
        btStartStop.setBackgroundColor(Color.RED);
        btStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBlueToothConnectThread == null) {
                    return;
                }
                mBlueToothConnectThread.setDeviceStatus(!currentDeviceState);
                btStartStop.setBackgroundColor(Color.YELLOW);
            }
        });

        btReset = (Button) findViewById(R.id.bt_reset);
        btReset.setBackgroundColor(Color.GREEN);
        btReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBlueToothConnectThread == null) {
                    return;
                }
                mBlueToothConnectThread.resetDevice(beatStream.beatFrameLength);
                btReset.setBackgroundColor(Color.YELLOW);
                btStartStop.setBackgroundColor(Color.YELLOW);
            }
        });

        btSend = (Button) findViewById(R.id.bt_send);
        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBlueToothConnectThread == null) {
                    return;
                }
                mBlueToothConnectThread.writeStringToBTModule(editText.getText().toString());
            }
        });

        statusHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String messageFromHandler = msg.getData().getString("message");
                if (messageFromHandler.equals("Update status")) {
                    updateStatusButton();
                    tcpClientThread.sendCommandAsJSON(tcpClientThread.getJSONObjectForCommand("set", "state", currentDeviceState));

                } else if (messageFromHandler.equals("Update reset button")) {
                    updateResetButton();
                    tcpClientThread.sendCommandAsJSON(tcpClientThread.getJSONObjectForCommand("set", "reset", 2));
                }
                textView.setText(messageFromHandler + " " + currentDeviceState);
                addDebugCommentToList(messageFromHandler);
            }
        };
    }

    private void addDebugCommentToList(String messageFromHandler) {
        debugCommentList.add(messageFromHandler);
        lv_debug.invalidate();
//        lv_debug.setSelection(debugCommentList.size()-1);
        return;
    }

    private void updateResetButton() {
        updateStatusButton();
        btReset.setBackgroundColor(Color.GREEN);
    }


    public void updateStatusButton() {
        if (currentDeviceState) {
            btStartStop.setBackgroundColor(Color.GREEN);
            return;
        }
        btStartStop.setBackgroundColor(Color.RED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_baseactivity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        makeToast(item.toString());
        if (item.getItemId() == R.id.connect_bluetooth) {
            showBluetoothSelectionDialog();
            return true;
        }
        if (item.getItemId() == R.id.connect_tcp) {
            showTCPSelectionDialog();
        }
        return true;
    }

    private void makeToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    private void showTCPSelectionDialog() {
        final Dialog tcpDialog = new Dialog(activity);
        tcpDialog.setContentView(R.layout.tcp_dialog);
        tcpDialog.setTitle("TCP Connection Setting");
        tcpDialog.show();

        final Button bt_startServer = (Button) tcpDialog.findViewById(R.id.bt_startServer);
        final EditText et_serverIP = (EditText) tcpDialog.findViewById(R.id.et_serverIP);
        final EditText et_serverPort = (EditText) tcpDialog.findViewById(R.id.et_serverPort);

        et_serverIP.setText(ipAddressOfServer);
        et_serverPort.setText("" + portNumberOfServer);

        bt_startServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipAddressOfServer = et_serverIP.getText().toString();
                portNumberOfServer = Integer.parseInt(et_serverPort.getText().toString());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("serverIpAddress", ipAddressOfServer);
                editor.putInt("serverPortNumber", portNumberOfServer);
                editor.commit();
                connectTCPServer();
                tcpDialog.dismiss();
            }
        });
    }

    private void initializeDebugListView() {
        lv_debug = (ListView) findViewById(R.id.lv_debugList);
        ArrayAdapter adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_list_item_1,
                debugCommentList);
        lv_debug.setAdapter(adapter);
    }


    private void showBluetoothSelectionDialog() {
        final Dialog deviceList = new Dialog(activity);
        //setting custom layout to dialog
        deviceList.setContentView(R.layout.devicelist_dialog);
        deviceList.setTitle("Select Device");
        ListView listView = (ListView) deviceList.findViewById(R.id.lv_devices);

        blueAdapter = BluetoothAdapter.getDefaultAdapter();

        if (blueAdapter == null) {
            return;
        }
        Set<BluetoothDevice> pairedDevices = blueAdapter.getBondedDevices();
        final ArrayList<BluetoothDevice> devicelist = new ArrayList<>();
        ArrayList<String> devicenamelist = new ArrayList<>();
        devicelist.add(null);
        devicenamelist.add("demo");
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                devicelist.add(device);
                devicenamelist.add(device.getName());

            }
        }
        ArrayAdapter adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_list_item_1,
                devicenamelist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                bdevice = devicelist.get(position);
                deviceList.dismiss();
                connectBluetooth();
            }
        });
        deviceList.show();
    }

    private void connectBluetooth() {
        mBlueToothConnectThread = new ThreadBlueToothConnect(bdevice, statusHandler, activity);
        mBlueToothConnectThread.start();
    }

    private void connectTCPServer() {
        System.out.println("Trying to Connect TCP ");
        tcpClientThread = new TcpClientThread(ipAddressOfServer, portNumberOfServer, statusHandler, activity);
    }
}

