package com.sovate.activitymanager;

import android.bluetooth.BluetoothDevice;

/**
 * Created by harks on 2016-03-01.
 */
public class DeviceInfo {

    BluetoothDevice device;
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

}
