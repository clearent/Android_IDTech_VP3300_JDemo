package com.clearent.payment;

import android.content.Context;

import com.clearent.ApplicationContext3In1;
import com.clearent.Constants;
import com.clearent.idtech.android.PublicOnReceiverListener;
import com.clearent.idtech.android.domain.ClearentPaymentRequest;
import com.clearent.idtech.android.family.ApplicationContext;
import com.clearent.idtech.android.family.DeviceFactory;
import com.clearent.idtech.android.family.reader.VP3300;
import com.idtechproducts.device.ReaderInfo;
import com.idtechproducts.device.ResDataStruct;

public class CardReaderService {

    private VP3300 device;

    public CardReaderService(ReaderInfo.DEVICE_TYPE deviceType, PublicOnReceiverListener publicOnReceiverListener, Context context, String paymentsBaseUrl, String paymentsPublicKey, boolean enableLogging) {
        ApplicationContext applicationContextContact = new ApplicationContext3In1(deviceType, publicOnReceiverListener, context, paymentsBaseUrl, paymentsPublicKey, null);
        device = DeviceFactory.getVP3300(applicationContextContact);
        if(enableLogging) {
            device.log_setVerboseLoggingEnable(true);
            device.log_setSaveLogEnable(true);
        }
    }

    public void applyClearentConfiguration() {
        device.applyClearentConfiguration();
    }

    public String device_getResponseCodeString(int ret) {
        return device.device_getResponseCodeString(ret);
    }

    public boolean device_setDeviceType(ReaderInfo.DEVICE_TYPE deviceType) {
        return device.device_setDeviceType(deviceType);
    }

    public boolean device_isConnected() {
        return device.device_isConnected();
    }

    public int device_startTransaction(ClearentPaymentRequest clearentPaymentRequest) {
        return device.device_startTransaction(clearentPaymentRequest);
    }

    public int emv_startTransaction(ClearentPaymentRequest clearentPaymentRequest) {
        return device.emv_startTransaction(clearentPaymentRequest.getAmount(), clearentPaymentRequest.getAmtOther(),clearentPaymentRequest.getType(), clearentPaymentRequest.getTimeout(),clearentPaymentRequest.getTags(), false);
    }

    public void registerListen() {
        device.registerListen();
    }

    public ReaderInfo.DEVICE_TYPE device_getDeviceType() {
        return device.device_getDeviceType();
    }

    public void release() {
        device.release();
    }

    public void unregisterListen() {
        device.unregisterListen();
    }

    public int emv_cancelTransaction(ResDataStruct resData) {
        return device.emv_cancelTransaction(resData);
    }

    public String getStoredDeviceSerialNumberOfConfiguredReader() {
        return device.getStoredDeviceSerialNumberOfConfiguredReader();
    }

    public int device_cancelTransaction() {
        return device.device_cancelTransaction();
    }

    public void device_configurePeripheralAndConnect() {
        device.device_configurePeripheralAndConnect();
    }

    public void setAutoConfiguration(boolean b) {
        device.setAutoConfiguration(b);
    }

    public void setContactlessConfiguration(boolean b) {
        device.setContactlessConfiguration(b);
    }

    public void setContactless(boolean b) {
        device.setContactless(b);
    }

    public void setReaderConfiguredSharedPreference(boolean b) {
        device.setReaderConfiguredSharedPreference(b);
    }

    public void setReaderContactlessConfiguredSharedPreference(boolean b) {
        device.setReaderContactlessConfiguredSharedPreference(b);
    }

    public void addRemoteLogRequest(String clientSoftwareVersion, String message) {
        device.addRemoteLogRequest(clientSoftwareVersion, message);
    }
}
