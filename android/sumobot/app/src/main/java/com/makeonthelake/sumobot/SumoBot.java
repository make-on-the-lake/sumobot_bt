package com.makeonthelake.sumobot;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SumoBot {
    private final static String DEBUG_TAG = SumoBot.class.getName();
    public final static int SCAN_PERIOD = 10000;

    public final static int DISCONNECTED = 1000;
    public final static int CONNECTED = 1001;
    public final static int SCANNING = 1002;
    public final static int FOUND = 1003;

    private static final char START[] = {0xAB};
    private static final char MOTOR_ID[] = {0x01};
    private static final char BUTTON_ID[] = {0x02};
    private static final char MOTOR_PADDING[] = {0x00, 0x00, 0x00, 0x00};
    private static final char BUTTON_PADDING[] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final char EMPTY[] = {0x00};
    private static final char END[] = {0xEF};
    private static final double TRANSMIT_INTERVAL_SEC = 0.1;

    String name = "";
    int connectionState = DISCONNECTED;
    BluetoothAdapter bluetoothAdapter;
    SumoBotConnectionListener sumoBotConnectionListener;
    BluetoothDevice device;
    Context context;
    BluetoothGatt bluetoothGatt;

    BluetoothGattCallback bluetoothCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                setupConnection(gatt);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = DISCONNECTED;
                Log.i(DEBUG_TAG, "Disconnected from GATT server.");
                sumoBotConnectionListener.onSumoBotDisconnected();
            }
        }
    };

    private void setupConnection(BluetoothGatt gatt) {
        connectionState = CONNECTED;
        Log.i(DEBUG_TAG, "Connected to GATT server.");
        Log.i(DEBUG_TAG, "Attempting to start service discovery:" +
                bluetoothGatt.discoverServices());
        sumoBotConnectionListener.onSumoBotConnected();


    }

    BluetoothAdapter.LeScanCallback btleScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            if (name.equals(device.getName())) {
                connectionState = FOUND;
                pairWithSumoBot(device);
            }
        }
    };

    Handler connectionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    public SumoBot(Context context, BluetoothAdapter bluetoothAdapter, SumoBotConnectionListener sumoBotConnectionListener) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.sumoBotConnectionListener = sumoBotConnectionListener;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean connected() {
        return connectionState == CONNECTED;
    }

    public void connect() {
        if (!bluetoothAdapter.isEnabled()) {
            sumoBotConnectionListener.onSumoBotRequestsBluetoothEnabled();
        } else {
            if (connectionState != DISCONNECTED || connectionState != SCANNING)
                scanForSumobot(true);
        }
    }

    public void disConnect() {
        if (device != null) {
            device = null;
            sumoBotConnectionListener.onSumoBotDisconnected();
        }
    }

    private void pairWithSumoBot(BluetoothDevice device) {
        this.device = device;
        bluetoothGatt = device.connectGatt(context, true, bluetoothCallback);
    }


    private void scanForSumobot(boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            connectionHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    connectionState = SCANNING;
                    sumoBotConnectionListener.onSumoBotScanStarted();
                    bluetoothAdapter.stopLeScan(btleScanCallback);
                }
            }, SCAN_PERIOD);

            connectionState = SCANNING;
            sumoBotConnectionListener.onSumoBotScanStarted();
            bluetoothAdapter.startLeScan(btleScanCallback);
        } else {
            connectionState = DISCONNECTED;
            bluetoothAdapter.stopLeScan(btleScanCallback);
        }
    }


}
