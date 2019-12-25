package com.clearent.util;

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
}
