package com.makeonthelake.sumobot;

public interface SumoBotConnectionListener {

    void onSumoBotScanStarted();
    void onSumoBotConnected();
    void onSumoBotDisconnected();
    void onSumoBotRequestsBluetoothEnabled();

}
