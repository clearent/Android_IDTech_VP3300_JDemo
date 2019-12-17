package com.clearent.ui.tools;

import android.app.Application;

import com.clearent.Constants;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ConfigureViewModel extends AndroidViewModel {

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

    public ConfigureViewModel(Application app) {
        super(app);
        prodEnvironment = new MutableLiveData<>();
        prodEnvironment.setValue(0);
        sandboxEnvironment = new MutableLiveData<>();
        sandboxEnvironment.setValue(1);

        environment = new MutableLiveData<>();

        audioJackReader = new MutableLiveData<>();
        audioJackReader.setValue(0);
        bluetoothReader = new MutableLiveData<>();
        bluetoothReader.setValue(1);

        apiKey = new MutableLiveData<>();
        publicKey = new MutableLiveData<>();

        enableContactless = new MutableLiveData<>();
        enableContactless.setValue(true);

        readerConnected = new MutableLiveData<>();

        enable2In1Mode = new MutableLiveData<>();
        enable2In1Mode.setValue(false);

        clearContactConfigurationCache = new MutableLiveData<>();
        clearContactConfigurationCache.setValue(false);

        clearContactlessConfigurationCache = new MutableLiveData<>();
        clearContactlessConfigurationCache.setValue(false);

        configureContact = new MutableLiveData<>();
        configureContact.setValue(false);

        configureContactless = new MutableLiveData<>();
        configureContactless.setValue(false);

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
        return "ConfigureViewModel{" +
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
                ", apiKey=" + apiKey.getValue() +
                ", publicKey=" + publicKey.getValue() +
                '}';
    }
}