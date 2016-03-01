package com.sovate.activitymanager;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by harks on 2016-02-27.
 */
public class DeviceAdapter extends BaseAdapter {
    public static final String TAG = "UartService";
    Context context;
    List<DeviceInfo> devices;
    LayoutInflater inflater;

    public DeviceAdapter(Context context, List<DeviceInfo> devices) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.devices = devices;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewGroup vg;

        if (convertView != null) {
            vg = (ViewGroup) convertView;
        } else {
            vg = (ViewGroup) inflater.inflate(R.layout.device_element, null);
        }

        BluetoothDevice device = devices.get(position).getDevice();

        final TextView tvalias = ((TextView) vg.findViewById(R.id.alias));
        final TextView tvadd = ((TextView) vg.findViewById(R.id.address));
        final TextView tvname = ((TextView) vg.findViewById(R.id.name));
        final TextView tvpaired = (TextView) vg.findViewById(R.id.paired);
        final TextView tvrssi = (TextView) vg.findViewById(R.id.rssi);

        tvrssi.setVisibility(View.VISIBLE);

        String deviceAddress = device.getAddress();

        //byte rssival = (byte) devRssiValues.get(device.getAddress()).intValue();
        byte rssival = 1;
        if (rssival != 0) {
            tvrssi.setText("Rssi = " + String.valueOf(rssival));
        }

        tvalias.setText(devices.get(position).getName());
        //tvalias.setText("Student_01");

        tvname.setText(device.getName());
        tvadd.setText(device.getAddress());
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            Log.i(TAG, "device::" + device.getName());
//            tvname.setTextColor(Color.WHITE);
//            tvadd.setTextColor(Color.WHITE);
//            tvpaired.setTextColor(Color.GRAY);
//            tvpaired.setVisibility(View.VISIBLE);
//            tvpaired.setText(R.string.paired);
//            tvrssi.setVisibility(View.VISIBLE);
//            tvrssi.setTextColor(Color.WHITE);

            tvalias.setTextColor(Color.BLACK);
            tvname.setTextColor(Color.BLACK);
            tvadd.setTextColor(Color.BLACK);
            tvpaired.setTextColor(Color.BLACK);
            tvpaired.setVisibility(View.VISIBLE);
            tvpaired.setText(R.string.paired);
            tvrssi.setVisibility(View.VISIBLE);
            tvrssi.setTextColor(Color.BLACK);

        } else {
            tvalias.setTextColor(Color.BLACK);
            tvname.setTextColor(Color.BLACK);
            tvadd.setTextColor(Color.BLACK);
            tvpaired.setVisibility(View.GONE);
            tvrssi.setVisibility(View.VISIBLE);
            tvrssi.setTextColor(Color.BLACK);
        }
        return vg;
    }
}