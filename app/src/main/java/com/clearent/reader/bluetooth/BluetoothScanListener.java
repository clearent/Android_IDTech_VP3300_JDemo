package com.clearent.reader.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface BluetoothScanListener {
    void handle(BluetoothDevice bluetoothDevice);
    void handle(BluetoothScanMessage bluetoothScanMessage);
}
