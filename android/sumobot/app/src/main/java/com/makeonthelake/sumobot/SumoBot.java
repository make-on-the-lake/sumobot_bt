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
import android.util.Log;

import java.nio.ByteBuffer/**/;
import java.nio.ByteOrder;

public class SumoBot {
    private final static String DEBUG_TAG = SumoBot.class.getName();


    public static byte[] hexStringToByteArray(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.replaceFirst("0x", "");
        }
        String[] bites = hexString.split("0x");
        byte[] data = new byte[bites.length];
        for (int i = 0; i < data.length; i++) {
            if (!bites[i].isEmpty()) {
                data[i] = (byte) (Integer.parseInt(bites[i].toLowerCase(), 16) & 0xff);
            }
        }
        return data;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {

            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public final static int SCAN_PERIOD = 10000;

    public final static int DISCONNECTED = 1000;
    public final static int CONNECTED = 1001;
    public final static int SCANNING = 1002;
    public final static int FOUND = 1003;

    private static final String SERVICE_ID = "ffe0";
    private static final String WRITE_ID = "ffe1";

    private static final byte START[] = hexStringToByteArray("0xAB");
    private static final byte MOTOR_ID[] = hexStringToByteArray("0x01");
    private static final byte BUTTON_ID[] = hexStringToByteArray("0x02");
    private static final byte MOTOR_PADDING[] = hexStringToByteArray("0x000x000x000x00");
    private static final byte BUTTON_PADDING[] = {(byte) 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final byte EMPTY[] = hexStringToByteArray("0x00");
    private static final byte END[] = hexStringToByteArray("0xEF");

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
                bluetoothAdapter.stopLeScan(btleScanCallback);
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
                    Log.d(DEBUG_TAG, "Saving Service: " + service.getUuid().toString());
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        Log.d(DEBUG_TAG, "Found characeristic: " + characteristic.getUuid().toString());
                        if (characteristic.getUuid().toString().contains(WRITE_ID)) {
                            Log.d(DEBUG_TAG, "Saving characeristic: " + characteristic.getUuid().toString());
                            writeCharacteristic = characteristic;
                            bluetoothGatt.setCharacteristicNotification(writeCharacteristic, true);
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
        Log.i(DEBUG_TAG, "Attempting to start service discovery:");
        bluetoothGatt.discoverServices();
    }

    private void drive() {
        Log.d(DEBUG_TAG, "has property write? " + (writeCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE));
        Log.d(DEBUG_TAG, "has property write no response? " + (writeCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE));
        Log.d(DEBUG_TAG, "has permissions? " + (writeCharacteristic.getPermissions()));

        if (connectionState == CONNECTED && writeCharacteristic != null) {
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.put((byte)0xAB);
            buffer.put((byte)0x01);

            ByteBuffer leftBuffer = ByteBuffer.allocate(4);
            leftBuffer.order(ByteOrder.LITTLE_ENDIAN);
            leftBuffer.putInt(1024);
            buffer.put(leftBuffer.array());

            ByteBuffer rightBuffer = ByteBuffer.allocate(4);
            rightBuffer.order(ByteOrder.LITTLE_ENDIAN);
            rightBuffer.putInt(512);
            buffer.put(rightBuffer.array());

            buffer.put(new byte[]{0x00, 0x00, 0x00, 0x00});
            buffer.put((byte)0x00);
            buffer.put((byte)0xEF);

            byte[] command = buffer.array();

            String output = "";
            for(int index = 0; index < command.length; index++) {
                output += Integer.toHexString(command[index]);
                output += " ";
            }
            Log.e(DEBUG_TAG, output);
            writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            writeCharacteristic.setValue(command);
            if (!bluetoothGatt.writeCharacteristic(writeCharacteristic)) {
                Log.e(DEBUG_TAG, "write was not issued correctly");
            }else{
                Log.e(DEBUG_TAG, "write!");
            }
            driveHandler.postDelayed(driveRunable, 100);
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
        if (!bluetoothAdapter.isEnabled()) {
            sumoBotConnectionListener.onSumoBotRequestsBluetoothEnabled();
        } else {
            if (connectionState != DISCONNECTED || connectionState != SCANNING)
                scanForSumobot(true);
        }
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
