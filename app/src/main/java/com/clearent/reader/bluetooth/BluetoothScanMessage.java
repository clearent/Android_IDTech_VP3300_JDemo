package com.clearent.reader.bluetooth;

public enum BluetoothScanMessage {

    SCAN_FAILED("Bluetooth Scan Failed"),
    SCAN_STOPPED("Bluetooth Scanning Stopped"),
    DEVICE_NOT_FOUND_WITH_LAST5("Device not found with last 5"),
    DEVICE_FOUND_WITH_LAST5("Device found");

    private String displayMessage;

    BluetoothScanMessage(String displayMessage) {
        this.displayMessage = displayMessage;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }

    public void setDisplayMessage(String displayMessage) {
        this.displayMessage = displayMessage;
    }


}
