package com.makeonthelake.sumobot;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SumoBot {
    private final static String DEBUG_TAG = SumoBot.class.getName();
    public final static int SCAN_PERIOD = 10000;

    public final static int DISCONNECTED = 1000;
    public final static int CONNECTED = 1001;
    public final static int SCANNING = 1002;
    public final static int FOUND = 1003;

    private static final String SERVICE_ID = "ffe0";
    private static final String WRITE_ID = "ffe1";

    private static final char START[] = {0xAB};
    private static final char MOTOR_ID[] = {0x01};
    private static final char BUTTON_ID[] = {0x02};
    private static final char MOTOR_PADDING[] = {0x00, 0x00, 0x00, 0x00};
    private static final char BUTTON_PADDING[] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final char EMPTY[] = {0x00};
    private static final char END[] = {0xEF};

    Handler connectionHandler = new Handler();
    Handler driveHandler = new Handler();
    String name = "";
    int connectionState = DISCONNECTED;
    BluetoothAdapter bluetoothAdapter;
    BluetoothGattService bluetoothGattService;
    BluetoothGatt bluetoothGatt;
    BluetoothGattCharacteristic writeCharacteristic;
    SumoBotConnectionListener sumoBotConnectionListener;
    BluetoothDevice device;
    Context context;

    Runnable driveRunable = new Runnable() {
        @Override
        public void run() {
            drive();
        }
    };

    BluetoothGattCallback

            bluetoothCallback = new BluetoothGattCallback() {
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

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(DEBUG_TAG, "onServicesDiscovered");
            Log.d(DEBUG_TAG, "looking for: " + SERVICE_ID);
            for (BluetoothGattService service : gatt.getServices()) {
                Log.d(DEBUG_TAG, "Found a Service UUID: " + service.getUuid().toString());
                if (service.getUuid().toString().contains(SERVICE_ID)) {
                    bluetoothGattService = service;
                    connectionState = CONNECTED;
                    sumoBotConnectionListener.onSumoBotConnected();
                    Log.d(DEBUG_TAG, "Found Service: " + service.getUuid().toString());
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        if (characteristic.getUuid().toString().contains(WRITE_ID)) {
                            Log.d(DEBUG_TAG, "Found characeristic: " + characteristic.getUuid().toString());
                            writeCharacteristic = characteristic;
                            drive();
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(DEBUG_TAG, "onCharacteristicChanged");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(DEBUG_TAG, "onCharacteristicWrite --- status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(DEBUG_TAG, "message was a success");
            } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
                Log.d(DEBUG_TAG, "write not permitted");
            } else if (status == BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED) {
                Log.d(DEBUG_TAG, "gatt request not supported");
            }


        }
    };

    private void setupConnection(BluetoothGatt gatt) {
        bluetoothGatt = gatt;
        Log.i(DEBUG_TAG, "Connected to GATT server.");
        Log.i(DEBUG_TAG, "Attempting to start service discovery:" + bluetoothGatt.discoverServices());
        bluetoothGatt.discoverServices();

    }

    private void drive() {
        StringBuilder builder = new StringBuilder();
        builder.append(START);
        builder.append(MOTOR_ID);
        builder.append("0512");
        builder.append("1024");
        builder.append(MOTOR_PADDING);
        builder.append(EMPTY);
        builder.append(END);
        Log.i(DEBUG_TAG, "Drive Command Len: " + builder.toString().length());
        Log.i(DEBUG_TAG, "Drive Command:" + builder.toString());
        driveHandler.postDelayed(driveRunable, 100);
        if (connectionState == CONNECTED && writeCharacteristic != null) {
            //writeCharacteristic.setValue(builder.toString());
            //bluetoothGatt.writeCharacteristic(writeCharacteristic);
        }
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
        /*
        if (!bluetoothAdapter.isEnabled()) {
            sumoBotConnectionListener.onSumoBotRequestsBluetoothEnabled();
        } else {
            if (connectionState != DISCONNECTED || connectionState != SCANNING)
                scanForSumobot(true);
        }
        */
        drive();
    }

    public void disConnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    private void pairWithSumoBot(BluetoothDevice device) {
        Log.d(DEBUG_TAG, "Pair with sumoBot");
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
