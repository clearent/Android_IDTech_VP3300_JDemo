package com.clearent.reader.bluetooth;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.util.List;

public class BluetoothScanCallback extends ScanCallback {

    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private String searchDeviceName;
    private BluetoothScanListener bluetoothScanListener;

    public BluetoothScanCallback(BluetoothScanListener bluetoothScanListener, String searchDeviceName) {
        this.bluetoothScanListener = bluetoothScanListener;
        this.searchDeviceName = searchDeviceName;
    }

    @Override
    public void onScanResult(int callbackType, final ScanResult scanResult) {
        if (scanResult == null || scanResult.getDevice() == null || scanResult.getDevice().getName() == null) {
            Log.d(TAG, "Empty bluetooth scan result");
        } else if (searchDeviceName != null && !"".equals(searchDeviceName) && scanResult.getDevice().getName().equals(searchDeviceName)) {
            Log.d(TAG, "Bluetooth scan success. Match " + scanResult.getDevice().getName());
            bluetoothScanListener.handle(scanResult.getDevice());
        } else if (searchDeviceName != null && !"".equals(searchDeviceName) && scanResult.getDevice().getName().contains(searchDeviceName)) {
            Log.d(TAG, "Bluetooth scan success. Partial " + scanResult.getDevice().getName());
            bluetoothScanListener.handle(scanResult.getDevice());
        } else {
            Log.d(TAG, "Skip bluetooth device" + scanResult.getDevice().getName());
        }
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);
        for (ScanResult sr : results) {
            Log.d(TAG, sr.toString());
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        bluetoothScanListener.handle(BluetoothScanMessage.SCAN_FAILED);
        Log.d(TAG, "Error Code: " + errorCode);
    }

    public String getSearchDeviceName() {
        return searchDeviceName;
    }
};
