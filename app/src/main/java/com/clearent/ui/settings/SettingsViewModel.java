package com.clearent.ui.settings;

import android.app.Application;

import com.clearent.util.LocalCache;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class SettingsViewModel extends AndroidViewModel {

    private MutableLiveData<Boolean> enableContactless;
    private MutableLiveData<String> readerConnected;

    private MutableLiveData<Boolean> enable2In1Mode;

    private MutableLiveData<Boolean> clearContactConfigurationCache;
    private MutableLiveData<Boolean> clearContactlessConfigurationCache;

    private MutableLiveData<Boolean> configureContact;
    private MutableLiveData<Boolean> configureContactless;

    private MutableLiveData<Integer> prodEnvironment;
    private MutableLiveData<Integer> sandboxEnvironment;

    private MutableLiveData<Integer> environment;

    private MutableLiveData<Integer> audioJackReader;
    private MutableLiveData<Integer> bluetoothReader;

    private MutableLiveData<String> apiKey;
    private MutableLiveData<String> publicKey;

    private MutableLiveData<String> last5OfBluetoothReader;

    public SettingsViewModel(Application app) {
        super(app);
        prodEnvironment = new MutableLiveData<>();
        prodEnvironment.setValue(LocalCache.getProdValue(getApplication()));
        sandboxEnvironment = new MutableLiveData<>();
        sandboxEnvironment.setValue(LocalCache.getSandboxValue(getApplication()));

        environment = new MutableLiveData<>();

        audioJackReader = new MutableLiveData<>();
        audioJackReader.setValue(LocalCache.getAudioJackValue(getApplication()));
        bluetoothReader = new MutableLiveData<>();
        bluetoothReader.setValue(LocalCache.getBluetoothReaderValue(getApplication()));

        last5OfBluetoothReader = new MutableLiveData<>();
        last5OfBluetoothReader.setValue(LocalCache.getSelectedBluetoothDeviceLast5(getApplication()));

        apiKey = new MutableLiveData<>();
        publicKey = new MutableLiveData<>();

        enableContactless = new MutableLiveData<>();
        enableContactless.setValue(LocalCache.getEnableContactlessValue(getApplication()));

        readerConnected = new MutableLiveData<>();

        enable2In1Mode = new MutableLiveData<>();
        enable2In1Mode.setValue(LocalCache.getEnable2InModeValue(getApplication()));

        clearContactConfigurationCache = new MutableLiveData<>();
        clearContactConfigurationCache.setValue(LocalCache.getClearContactConfigValue(getApplication()));

        clearContactlessConfigurationCache = new MutableLiveData<>();
        clearContactlessConfigurationCache.setValue(LocalCache.getClearContactlessConfigValue(getApplication()));

        configureContact = new MutableLiveData<>();
        configureContact.setValue(LocalCache.getConfigureContactValue(getApplication()));

        configureContactless = new MutableLiveData<>();
        configureContactless.setValue(LocalCache.getConfigureContactlessValue(getApplication()));

    }

    public MutableLiveData<String> getLast5OfBluetoothReader() {
        return last5OfBluetoothReader;
    }

    public void setLast5OfBluetoothReader(String last5OfBluetoothReader) {
        this.last5OfBluetoothReader.postValue(last5OfBluetoothReader );
    }

    public MutableLiveData<String> getReaderConnected() {
        return readerConnected;
    }

    public void updateReaderConnected(String message) {
        readerConnected.postValue(message);
    }

    public void setReaderConnected(MutableLiveData<String> readerConnected) {
        this.readerConnected = readerConnected;
    }

    public MutableLiveData<Integer> getEnvironment() {
        return environment;
    }

    public MutableLiveData<Integer> getProdEnvironment() {
        return prodEnvironment;
    }

    public MutableLiveData<Integer> getSandboxEnvironment() {
        return sandboxEnvironment;
    }

    public MutableLiveData<Boolean> getEnableContactless() {
        return enableContactless;
    }

    public MutableLiveData<Boolean> getEnable2In1Mode() {
        return enable2In1Mode;
    }

    public MutableLiveData<Boolean> getClearContactConfigurationCache() {
        return clearContactConfigurationCache;
    }

    public MutableLiveData<Boolean> getClearContactlessConfigurationCache() {
        return clearContactlessConfigurationCache;
    }

    public MutableLiveData<Boolean> getConfigureContact() {
        return configureContact;
    }

    public MutableLiveData<Boolean> getConfigureContactless() {
        return configureContactless;
    }

    public MutableLiveData<Integer> getAudioJackReader() {
        return audioJackReader;
    }

    public MutableLiveData<Integer> getBluetoothReader() {
        return bluetoothReader;
    }

    public MutableLiveData<String> getApiKey() {
        return apiKey;
    }

    public MutableLiveData<String> getPublicKey() {
        return publicKey;
    }

    @Override
    public String toString() {
        return "SettingsViewModel{" +
                "enableContactless=" + enableContactless.getValue() +
                ", clearContactConfigurationCache=" + clearContactConfigurationCache.getValue() +
                ", clearContactlessConfigurationCache=" + clearContactlessConfigurationCache.getValue() +
                ", configureContact=" + configureContact.getValue() +
                ", configureContactless=" + configureContactless.getValue() +
                ", prodEnvironment=" + prodEnvironment.getValue() +
                ", sandboxEnvironment=" + sandboxEnvironment.getValue() +
                ", environment=" + environment.getValue() +
                ", audioJackReader=" + audioJackReader.getValue() +
                ", bluetoothReader=" + bluetoothReader.getValue() +
                '}';
    }
}