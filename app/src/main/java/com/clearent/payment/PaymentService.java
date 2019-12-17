package com.clearent.payment;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import com.clearent.ApplicationContext3In1;
import com.clearent.ApplicationContextContact;
import com.clearent.idtech.android.family.DeviceFactory;
import com.clearent.idtech.android.family.reader.VP3300;
import com.idtechproducts.device.Common;
import com.idtechproducts.device.ReaderInfo;
import com.idtechproducts.device.bluetooth.BluetoothLEController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PaymentService {
/*

    private PaymentActivity.PaymentFragment paymentFragment;

    private ReaderThread readerThread;

    private ApplicationContextContact applicationContextContact;
    private ApplicationContext3In1 applicationContext3In1;

    public PaymentService(PaymentActivity.PaymentFragment paymentFragment, ApplicationContextContact applicationContextContact) {
        this.paymentFragment = paymentFragment;
        this.applicationContextContact = applicationContextContact;
    }

    public PaymentService(PaymentActivity.PaymentFragment paymentFragment, ApplicationContext3In1 applicationContext3In1) {
        this.paymentFragment = paymentFragment;
        this.applicationContext3In1 = applicationContext3In1;
    }

    public synchronized void start() {
        if (readerThread == null) {
            readerThread = new ReaderThread();
            readerThread.start();
        }
    }

    public void unregisterListen() {
        readerThread.unregisterListen();
    }

    public void releaseSDK() {
        readerThread.releaseSDK();
    }

    public boolean isReady() {
        return readerThread.isReady();
    }

    public void setReady(boolean ready) {
        readerThread.setReady(ready);
    }

    public void addRemoteLogRequest(String clientSoftwareVersion, String message) {
        readerThread.addRemoteLogRequest(clientSoftwareVersion, message);
    }

    public VP3300 getDevice() {
        return readerThread.getDevice();
    }

    public boolean isBluetoothScanning() {
        return readerThread.isBluetoothScanning();
    }

    public void setBluetoothScanning(boolean bluetoothScanning) {
        readerThread.setBluetoothScanning(bluetoothScanning);
    }

    public boolean isBtleDeviceRegistered() {
        return readerThread.isBtleDeviceRegistered();
    }

    public void setBtleDeviceRegistered(boolean btleDeviceRegistered) {
        readerThread.setBtleDeviceRegistered(btleDeviceRegistered);
    }

    public void stopBluetoothScan() {
        readerThread.stopBluetoothScan();
    }

    public void scanforDevice() {
        readerThread.scanforDevice();
    }

    public void registerListen() {
        System.out.println("before readerthread" + Thread.currentThread().getId());
        readerThread.registerListen();
    }

    public boolean isDeviceConnected() {
        return readerThread.isDeviceConnected();
    }

    public boolean isDeviceTypeInitializedInIDTechFramework(ReaderInfo.DEVICE_TYPE deviceType) {
        return readerThread.getDevice().device_setDeviceType(deviceType);
    }

    private class ReaderThread extends Thread {

        private boolean btleDeviceRegistered = false;
        private BluetoothAdapter mBtAdapter;
        private final long BLE_ScanTimeout = 10000;
        private String btleDeviceAddress = null;

        private boolean isBluetoothScanning = false;
        private boolean isReady = false;

        private VP3300 device;

        public ReaderThread() {
            if (applicationContextContact != null) {
                device = DeviceFactory.getVP3300(applicationContextContact);
            } else {
                device = DeviceFactory.getVP3300(applicationContext3In1);
            }

            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        public void run() {

            device.log_setVerboseLoggingEnable(true);
            device.log_setSaveLogEnable(true);

            if (mBtAdapter == null) {
                Toast.makeText(paymentFragment.getActivity(), "Bluetooth LE is not available\r\n", Toast.LENGTH_LONG).show();
                return;
            }

            if (!mBtAdapter.isEnabled()) {
                paymentFragment.requestBluetooth();
            }

        }

        public void cancel() {
            releaseSDK();
        }

        public void scanforDevice() {
            scanDevice();
        }

        private void scanDevice() {
            if (device.device_getDeviceType() != ReaderInfo.DEVICE_TYPE.DEVICE_VP3300_BT) {
                return;
            }

            isReady = false;

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            btleDeviceRegistered = false;
            paymentFragment.setupBluetoothScanTimeout();
            List<ScanFilter> scanFilters = createScanFilter();

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .setReportDelay(0)
                    .build();
            mBtAdapter.getBluetoothLeScanner().startScan(scanFilters, settings, scanCallback);
        }

        public void stopBluetoothScan() {
            mBtAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            isBluetoothScanning = false;
        }

        public boolean isDeviceConnected() {
            return device.device_isConnected();
        }

        private List<ScanFilter> createScanFilter() {
            List<ScanFilter> scanFilters = new ArrayList<>();
            ScanFilter scanFilter = null;
            String last5 = Common.getBLEDeviceName();

            if (btleDeviceAddress != null) {
                scanFilter = new ScanFilter.Builder().setDeviceAddress(btleDeviceAddress).build();
            } else if (last5 != null && !"".equals(last5)) {
                scanFilter = new ScanFilter.Builder().setDeviceName("IDTECH-VP3300-" + last5).build();
            } else {
                scanFilter = new ScanFilter.Builder().build();
            }
            scanFilters.add(scanFilter);
            return scanFilters;
        }

        private ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                String last5 = Common.getBLEDeviceName();
                String searchString = last5 != null && !"".equals(last5) ? "IDTECH-VP3300-" + last5 : "IDTECH";
                if (result == null || result.getDevice() == null || result.getDevice().getName() == null) {
                    Log.i("SCAN", "Skipping weirdness ");
                } else if (!btleDeviceRegistered && result.getDevice().getName().contains(searchString)) {
                    paymentFragment.updateInfo("\nBluetooth Device found");
                    Log.i("SCAN", "Scan success " + result.getDevice().getName());
                    BluetoothLEController.setBluetoothDevice(result.getDevice());
                    btleDeviceAddress = result.getDevice().getAddress();
                    btleDeviceRegistered = true;
                    device.registerListen();
                } else {
                    Log.i("SCAN", "Skip " + result.getDevice().getName());
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                for (ScanResult sr : results) {
                    Log.i("ScanResult - Results", sr.toString());
                    paymentFragment.updateInfo("\nScanResult - Results" + sr.toString() + "\n");
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e("Scan Failed", "Error Code: " + errorCode);
                paymentFragment.updateInfo("\nScan Failed. Error Code: " + errorCode + "\n");
            }
        };


        public void unregisterListen() {
            device.unregisterListen();
        }

        public void releaseSDK() {
            if (device != null) {
                device.unregisterListen();
                device.release();
                isReady = false;
                btleDeviceRegistered = false;
                isBluetoothScanning = false;
            }
        }

        public boolean isReady() {
            return isReady;
        }

        public void setReady(boolean ready) {
            isReady = ready;
        }

        public void addRemoteLogRequest(String clientSoftwareVersion, String message) {
            device.addRemoteLogRequest(clientSoftwareVersion, message);
        }

        public VP3300 getDevice() {
            return device;
        }

        public boolean isBluetoothScanning() {
            return isBluetoothScanning;
        }

        public void setBluetoothScanning(boolean bluetoothScanning) {
            isBluetoothScanning = bluetoothScanning;
        }

        public boolean isBtleDeviceRegistered() {
            return btleDeviceRegistered;
        }

        public void setBtleDeviceRegistered(boolean btleDeviceRegistered) {
            this.btleDeviceRegistered = btleDeviceRegistered;
        }

        public void registerListen() {
            System.out.println("Inside readerthread" + Thread.currentThread().getId());
            device.registerListen();
        }
    }
*/


}

