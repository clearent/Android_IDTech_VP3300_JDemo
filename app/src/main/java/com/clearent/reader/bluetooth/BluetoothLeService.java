/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.clearent.reader.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

public class BluetoothLeService implements BluetoothScanListener {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothScanCallback bluetoothScanCallback;
    private BluetoothScanListener bluetoothScanListener;
    private boolean bluetoothScanningInProcess = false;
    private Handler mHandler;
    private long scanPeriodInMs;

    public BluetoothLeService(BluetoothScanListener bluetoothScanListener, long scanPeriodInMs) {
        this.bluetoothScanListener = bluetoothScanListener;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.scanPeriodInMs = scanPeriodInMs;
        mHandler = new Handler();
    }

    public void scan(String searchDeviceName) {
        bluetoothScanCallback = new BluetoothScanCallback(this, searchDeviceName);
        scanLeDevice(true, searchDeviceName);
    }

    public void stopScan() {
        bluetoothScanningInProcess = false;
        bluetoothAdapter.getBluetoothLeScanner().stopScan(bluetoothScanCallback);
        bluetoothScanListener.handle(BluetoothScanMessage.SCAN_STOPPED);
    }

    private void scanLeDevice(final boolean enable, String searchDeviceName) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                }
            }, scanPeriodInMs);

            bluetoothScanningInProcess = true;

            List<ScanFilter> scanFilters = createScanFilter(searchDeviceName);
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build();

            bluetoothAdapter.getBluetoothLeScanner().startScan(scanFilters, settings, bluetoothScanCallback);
        } else {
            bluetoothScanningInProcess = false;
            stopScan();
        }
    }

    private List<ScanFilter> createScanFilter(String searchDeviceName) {
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter scanFilter = null;
        if(searchDeviceName != null
                && !"".equals(searchDeviceName)
                && searchDeviceName.length() > 5
                && searchDeviceName.substring(searchDeviceName.length() - 5).matches("\\d+(?:\\.\\d+)?")) {
       // if (searchDeviceName != null && !"".equals(searchDeviceName)  && searchDeviceName.matches("\\d+(?:\\.\\d+)?")) {
            scanFilter = new ScanFilter.Builder().setDeviceName(searchDeviceName).build();
        } else {
            scanFilter = new ScanFilter.Builder().build();
        }
        scanFilters.add(scanFilter);
        return scanFilters;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public boolean isBluetoothScanningInProcess() {
        return bluetoothScanningInProcess;
    }

    @Override
    public void handle(BluetoothDevice bluetoothDevice) {
        if (this.bluetoothScanCallback.getSearchDeviceName() != null
                && bluetoothDevice.getName() != null
                && bluetoothDevice.getName().equals(bluetoothScanCallback.getSearchDeviceName())) {
            stopScan();
            bluetoothScanListener.handle(BluetoothScanMessage.DEVICE_FOUND_WITH_LAST5);
        }
        bluetoothScanListener.handle(bluetoothDevice);
    }

    @Override
    public void handle(BluetoothScanMessage bluetoothScanMessage) {
        if (BluetoothScanMessage.SCAN_STOPPED.equals(bluetoothScanMessage) && bluetoothScanCallback.getSearchDeviceName() != null) {
            bluetoothScanListener.handle(BluetoothScanMessage.DEVICE_NOT_FOUND_WITH_LAST5);
        } else {
            bluetoothScanListener.handle(bluetoothScanMessage);
        }

    }
}
