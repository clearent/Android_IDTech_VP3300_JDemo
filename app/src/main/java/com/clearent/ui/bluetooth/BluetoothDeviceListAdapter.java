package com.clearent.ui.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.clearent.payment.R;

import java.util.ArrayList;

public class BluetoothDeviceListAdapter extends BaseAdapter {

    private ArrayList<BluetoothDevice> bluetoothDevices;
    private LayoutInflater layoutInflater;

    public BluetoothDeviceListAdapter(LayoutInflater layoutInflater) {
        super();
        bluetoothDevices = new ArrayList<BluetoothDevice>();
        this.layoutInflater = layoutInflater;
    }

    public void addDevice(BluetoothDevice device) {
        if (!bluetoothDevices.contains(device)) {
            bluetoothDevices.add(device);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return bluetoothDevices.get(position);
    }

    public void clear() {
        bluetoothDevices.clear();
    }

    @Override
    public int getCount() {
        return bluetoothDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return bluetoothDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        BluetoothDeviceListAdapter.ViewHolder viewHolder;
        if (view == null) {
            view = layoutInflater.inflate(R.layout.listitem_device, null);
            viewHolder = new BluetoothDeviceListAdapter.ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            view.setTag(viewHolder);
        } else {
            viewHolder = (BluetoothDeviceListAdapter.ViewHolder) view.getTag();
        }

        BluetoothDevice device = bluetoothDevices.get(i);
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0) {
            viewHolder.deviceName.setText(deviceName);
        } else {
            viewHolder.deviceName.setText(R.string.unknown_device);
        }
        viewHolder.deviceAddress.setText(device.getAddress());
        return view;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
