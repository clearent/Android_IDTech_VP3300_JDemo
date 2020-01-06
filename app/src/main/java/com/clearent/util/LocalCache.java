package com.clearent.util;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

public class LocalCache {

    public static String SHARED_PREFERENCES_SELECTED_BLUETOOTH_DEVICE_LAST5 = "SelectedBluetoothDeviceLast5";
    public static String SHARED_PREFERENCES_NAME = "ClearentJdemo";

    public static void setSelectedBluetoothDeviceLast5(Context context, String selectedBluetoothDeviceLast5) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SHARED_PREFERENCES_SELECTED_BLUETOOTH_DEVICE_LAST5, selectedBluetoothDeviceLast5);
        editor.commit();
    }

    public static String getSelectedBluetoothDeviceLast5(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        return settings.getString(SHARED_PREFERENCES_SELECTED_BLUETOOTH_DEVICE_LAST5, "");
    }


    public static void setProdValue(Context context, Integer value) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("SettingsProdValue", value);
        editor.commit();
    }

    public static Integer getProdValue(Application application) {
        SharedPreferences settings = application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        return settings.getInt("SettingsProdValue", 0);
    }


    public static void setSandboxValue(Context context, Integer value) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("SettingsSandboxValue", value);
        editor.commit();
    }

    public static Integer getSandboxValue(Application application) {
        SharedPreferences settings = application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        return settings.getInt("SettingsSandboxValue", 1);
    }

    public static void setAudioJackValue(Context context, Integer value) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("SettingsAudioJackValue", value);
        editor.commit();
    }

    public static Integer getAudioJackValue(Application application) {
        SharedPreferences settings = application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        return settings.getInt("SettingsAudioJackValue", 0);
    }

    public static void setBluetoothReaderValue(Context context, Integer value) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("SettingsBluetoothValue", value);
        editor.commit();
    }

    public static Integer getBluetoothReaderValue(Application application) {
        SharedPreferences settings = application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        return settings.getInt("SettingsBluetoothValue", 1);
    }


    public static void setEnableContactlessValue(Context context, Boolean value) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("SettingsEnableContactlessValue", value);
        editor.commit();
    }

    public static Boolean getEnableContactlessValue(Application application) {
        SharedPreferences settings = application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        return settings.getBoolean("SettingsEnableContactlessValue", true);
    }

    public static void setEnable2InModeValue(Context context, Boolean value) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("SettingsEnable2In1ModeValue", value);
        editor.commit();
    }


    public static Boolean getEnable2InModeValue(Application application) {
        SharedPreferences settings = application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        return settings.getBoolean("SettingsEnable2In1ModeValue", false);
    }


    public static void setClearContactConfigValue(Context context, Boolean value) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("SettingsClearContactConfigValue", value);
        editor.commit();
    }

    public static Boolean getClearContactConfigValue(Application application) {
        SharedPreferences settings = application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        return settings.getBoolean("SettingsClearContactConfigValue", false);
    }


    public static void setClearContactlessConfigValue(Context context, Boolean value) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("SettingsClearContactlessConfigValue", value);
        editor.commit();
    }


    public static Boolean getClearContactlessConfigValue(Application application) {
        SharedPreferences settings = application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        return settings.getBoolean("SettingsClearContactlessConfigValue", false);
    }


    public static void setConfigureContactValue(Context context, Boolean value) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("SettingsConfigureContactValue", value);
        editor.commit();
    }

    public static Boolean getConfigureContactValue(Application application) {
        SharedPreferences settings = application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        return settings.getBoolean("SettingsConfigureContactValue", false);
    }


    public static void setConfigureContactlessValue(Context context, Boolean value) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("SettingsConfigureContactlessValue", value);
        editor.commit();
    }

    public static Boolean getConfigureContactlessValue(Application application) {
        SharedPreferences settings = application.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        return settings.getBoolean("SettingsConfigureContactlessValue", false);
    }
}
