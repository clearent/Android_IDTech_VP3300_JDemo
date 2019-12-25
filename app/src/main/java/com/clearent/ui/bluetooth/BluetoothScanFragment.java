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

package com.clearent.ui.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.clearent.Constants;
import com.clearent.payment.R;
import com.clearent.reader.bluetooth.BluetoothLeService;
import com.clearent.reader.bluetooth.BluetoothScanListener;
import com.clearent.reader.bluetooth.BluetoothScanMessage;
import com.clearent.util.LocalCache;
import com.clearent.ui.settings.SettingsViewModel;

import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class BluetoothScanFragment extends ListFragment implements BluetoothScanListener {

    private SettingsViewModel settingsViewModel;

    private BluetoothDeviceListAdapter bluetoothDeviceListAdapter;

    private BluetoothLeService bluetoothLeService;

    private View root = null;
    private String last5OfBluetoothReader = null;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bluetoothDeviceListAdapter = new BluetoothDeviceListAdapter(this.getLayoutInflater());
        setListAdapter(bluetoothDeviceListAdapter);

        settingsViewModel =
                ViewModelProviders.of(getActivity()).get(SettingsViewModel.class);

        bluetoothLeService = new BluetoothLeService(this, Constants.BLUETOOTH_SCAN_PERIOD);
        observeConfigurationValues(root);

        bluetoothLeService.scan(Constants.IDTECH);
    }

    private void observeConfigurationValues(final View root) {
        settingsViewModel.getLast5OfBluetoothReader().observe(getActivity(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                last5OfBluetoothReader = s;
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        bluetoothDeviceListAdapter.clear();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        final BluetoothDevice bluetoothDevice = bluetoothDeviceListAdapter.getDevice(position);

        if (bluetoothDevice == null) {
            return;
        }

        if(bluetoothDevice.getName() != null && !"".equals(bluetoothDevice.getName()) && bluetoothDevice.getName().length() > 5) {
            String last5 = bluetoothDevice.getName().substring(bluetoothDevice.getName().length() - 5);
            settingsViewModel.setLast5OfBluetoothReader(last5OfBluetoothReader);
            LocalCache.setSelectedBluetoothDeviceLast5(getActivity().getApplicationContext(), last5);
        }

        if (bluetoothLeService.isBluetoothScanningInProcess()) {
            bluetoothLeService.stopScan();
        }

        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.nav_configure);
    }

    @Override
    public void handle(BluetoothDevice bluetoothDevice) {
        bluetoothDeviceListAdapter.addDevice(bluetoothDevice);
        bluetoothDeviceListAdapter.notifyDataSetChanged();
    }

    @Override
    public void handle(BluetoothScanMessage bluetoothScanMessage) {
        Toast.makeText(getActivity(), bluetoothScanMessage.getDisplayMessage(), Toast.LENGTH_LONG).show();
    }

}
