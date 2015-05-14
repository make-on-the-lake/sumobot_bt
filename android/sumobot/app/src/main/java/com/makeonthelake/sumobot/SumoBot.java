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

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class SumoBot {
    private final static String DEBUG_TAG = SumoBot.class.getName();

    public final static int SCAN_PERIOD = 10000;

    public final static int IDLE_SPEED = 512;

    public final static int DISCONNECTED = 1000;
    public final static int CONNECTED = 1001;
    public final static int SCANNING = 1002;
    public final static int FOUND = 1003;

    private static final String SERVICE_ID = "ffe0";
    private static final String WRITE_ID = "ffe1";

    private static final byte START = (byte) 0xAB;
    private static final byte MOTOR_ID = (byte) 0x01;
    private static final byte BUTTON_ID = (byte) 0x02;
    private static final byte MOTOR_PADDING[] = new byte[]{0x00, 0x00, 0x00, 0x00};
    private static final byte BUTTON_PADDING[] = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final byte EMPTY = (byte) 0x00;
    private static final byte END = (byte) 0xEF;

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
    int leftSpeed = 512;
    int rightSpeed = 512;

    Runnable driveRunable = new Runnable() {
        @Override
        public void run() {
            drive();
        }
    };

    BluetoothGattCallback bluetoothCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bluetoothAdapter.stopLeScan(btleScanCallback);
                setupConnection(gatt);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = DISCONNECTED;
                Log.e(DEBUG_TAG, "-------------------------Disconnected from GATT server.");
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
                bluetoothAdapter.stopLeScan(btleScanCallback);
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
            if ((status & BluetoothGatt.GATT_SUCCESS) != 0) {
                Log.d(DEBUG_TAG, "message was a success");
            }
            if ((status & BluetoothGatt.GATT_WRITE_NOT_PERMITTED) != 0) {
                Log.d(DEBUG_TAG, "write not permitted");
            }
            if ((status & BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED) != 0) {
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
        BluetoothGattService service = bluetoothGatt.getService(bluetoothGattService.getUuid());
        writeCharacteristic = service.getCharacteristic(writeCharacteristic.getUuid());


        if (connected() && writeCharacteristic != null) {
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.put((byte) 0xAB);
            buffer.put((byte) 0x01);

            ByteBuffer leftBuffer = ByteBuffer.allocate(4);
            leftBuffer.order(ByteOrder.LITTLE_ENDIAN);
            leftBuffer.putInt(leftSpeed);
            buffer.put(leftBuffer.array());

            ByteBuffer rightBuffer = ByteBuffer.allocate(4);
            rightBuffer.order(ByteOrder.LITTLE_ENDIAN);
            rightBuffer.putInt(rightSpeed);
            buffer.put(rightBuffer.array());

            buffer.put(new byte[]{0x00, 0x00, 0x00, 0x00});
            buffer.put((byte) 0x00);
            buffer.put((byte) 0xEF);

            byte[] command = buffer.array();

            String output = "";
            for (int index = 0; index < command.length; index++) {
                output += Integer.toHexString(command[index]);
                output += " ";
            }
            Log.e(DEBUG_TAG, output);
            writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            writeCharacteristic.setValue(command);
            if (!bluetoothGatt.writeCharacteristic(writeCharacteristic)) {
                Log.e(DEBUG_TAG, "write was not issued correctly");
            } else {
                Log.e(DEBUG_TAG, "write!");
            }
            driveHandler.postDelayed(driveRunable, 100);
        } else {
            bluetoothGatt = device.connectGatt(context, true, bluetoothCallback);
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

    public void moveRightWheelForward(int percentage) {
        int speed = (int) ceil((((float) percentage) / 100) * IDLE_SPEED);
        Log.d(DEBUG_TAG, "speed: " + speed);
        rightSpeed = max(IDLE_SPEED - speed, 0);
        Log.d(DEBUG_TAG, "right speed: " + rightSpeed);
    }

    public void moveLeftWheelForward(int percentage) {
        int speed = (int) ceil((((float) percentage) / 100) * IDLE_SPEED);
        Log.d(DEBUG_TAG, "speed: " + speed);
        leftSpeed = max(IDLE_SPEED - speed, 0);
        Log.d(DEBUG_TAG, "left speed: " + leftSpeed);
    }

    public void moveRightWheelBackward(int percentage) {
        int speed = (int) ceil((((float) percentage) / 100) * IDLE_SPEED);
        rightSpeed = min(IDLE_SPEED + speed, IDLE_SPEED * 2);
        Log.d(DEBUG_TAG, "right speed: " + rightSpeed);
    }

    public void moveLeftWheelBackward(int percentage) {
        int speed = (int) ceil((((float) percentage) / 100) * IDLE_SPEED);
        leftSpeed = min(IDLE_SPEED + speed, IDLE_SPEED * 2);
        Log.d(DEBUG_TAG, "left speed: " + leftSpeed);
    }

    public void stopLeftWheel() {
        leftSpeed = IDLE_SPEED;
    }

    public void stopRightWheel() {
        rightSpeed = IDLE_SPEED;
    }

    public void connect() {
        if (!bluetoothAdapter.isEnabled()) {
            sumoBotConnectionListener.onSumoBotRequestsBluetoothEnabled();
        } else {
            if (connectionState != CONNECTED || connectionState != SCANNING) {
                scanForSumobot(true);
            }
        }
    }

    public void disConnect() {
        bluetoothGatt.disconnect();
        bluetoothGatt.close();
        connectionState = DISCONNECTED;
        sumoBotConnectionListener.onSumoBotDisconnected();
    }

    public void gpioPressed() {
        if (connected()) {

            BluetoothGattService service = bluetoothGatt.getService(bluetoothGattService.getUuid());
            writeCharacteristic = service.getCharacteristic(writeCharacteristic.getUuid());


            if (writeCharacteristic != null) {
                byte[] command = ByteBuffer.allocate(16)
                        .put(START)
                        .put(BUTTON_ID)
                        .put(BUTTON_PADDING)
                        .put(EMPTY)
                        .put(END)
                        .array();

                String output = "";
                for (int index = 0; index < command.length; index++) {
                    output += Integer.toHexString(command[index]);
                    output += " ";
                }
                Log.e(DEBUG_TAG, output);
                writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                writeCharacteristic.setValue(command);
                if (!bluetoothGatt.writeCharacteristic(writeCharacteristic)) {
                    Log.e(DEBUG_TAG, "write was not issued correctly");
                } else {
                    Log.e(DEBUG_TAG, "write!");
                }
            }
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
