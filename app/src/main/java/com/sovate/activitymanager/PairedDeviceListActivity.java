package com.sovate.activitymanager;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by harks on 2016-02-26.
 */
public class PairedDeviceListActivity extends Activity {

    private BluetoothAdapter mBluetoothAdapter;

    private TextView mEmptyList;
    public static final String TAG = "UartService";

    List<BluetoothDevice> deviceList;
    private DeviceAdapter deviceAdapter;

    private static final long SCAN_PERIOD = 10000; // Bluetooth search time
    private Handler mHandler;
    private boolean mScanning;

    private final String deviceName = "InBodyBand";

    private BluetoothLeScanner leScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
        setContentView(R.layout.device_list);
        android.view.WindowManager.LayoutParams layoutParams = this.getWindow().getAttributes();
        layoutParams.gravity = Gravity.TOP;
        layoutParams.y = 200;
        mHandler = new Handler();

        // Check if BLE available
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //mBluetoothAdapter = bluetoothManager.getAdapter();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.enable();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        leScanner = mBluetoothAdapter.getBluetoothLeScanner();


        // scan 처리
        //populateList();


        mEmptyList = (TextView) findViewById(R.id.empty);
        Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mScanning == false)
                    scanLeDevice(true);
                else
                    finish();
            }
        });

        GetPairedBLEDevices(MainActivity.deviceName);

    }

    /*
     * listup
     */
    private void GetPairedBLEDevices(String DeviceName) {
        // list 정보 구성
        deviceList = new ArrayList<BluetoothDevice>();
        //deviceAdapter = new DeviceAdapter(this, deviceList);

        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(deviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() != 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName() == null)
                    continue;
                if (DeviceName.equals(device.getName()))
                {
                    // add list
                    deviceList.add(device);
                    deviceAdapter.notifyDataSetChanged();
                }
            }

//            if (deviceList.size() > 0) {
//                mEmptyList.setVisibility(View.GONE);
//                deviceAdapter.notifyDataSetChanged();
//            }
        }
    }

    /*
     * Setup list
     */
    private void populateList() {
        Log.d(TAG, "populateList");
        deviceList = new ArrayList<BluetoothDevice>();
        //deviceAdapter = new DeviceAdapter(this, deviceList);

        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(deviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        //scan();
        //scanLeDevice(true);

    }

    /*
     * Stop searching 10 seconds after starting bluetooth search
     */
    private void scanLeDevice(final boolean enable) {
        final Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    cancelButton.setText(R.string.scan);

                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            cancelButton.setText(R.string.cancel);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            cancelButton.setText(R.string.scan);
        }

    }

    /*
     * Event handler when bluetooth device is found
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {

            // Search InBodyBAND with certain signal
            if (rssi < -70 || !deviceName.equals(device.getName()))
                return;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addDevice(device, rssi);
                        }
                    });
                }
            });
        }
    };

    private void scan()
    {
        leScanner.startScan(scanCallback);
    }

    private ScanCallback scanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            super.onScanResult(callbackType, result);
            String log =  "|" + "111111111111111" + "|" + result.getDevice().getName() + "|" + result.getDevice().getAddress();
            Log.i("onScanResult", log);
        }


        @Override
        public void onScanFailed(int errorCode)
        {
            super.onScanFailed(errorCode);
            String log = "|" + "2222222222222" + "|" + errorCode;
            Log.i("onScanFailed", log);
        }


        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            super.onBatchScanResults(results);
            for (ScanResult result : results)
            {
                String log = "|" + "33333333333333|" + result.getDevice().getName() + "|" + result.getDevice().getAddress() + "|";
                Log.i("onBatchScanResults", log);
            }
        }
    };

    /*
     * Add found devices in the list
     */
    private void addDevice(BluetoothDevice device, int rssi) {
        boolean deviceFound = false;

        for (BluetoothDevice listDev : deviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }

        if (!deviceFound) {
            deviceList.add(device);
            mEmptyList.setVisibility(View.GONE);
            deviceAdapter.notifyDataSetChanged();
        }
    }

    /*
     * Setting bluetooth event filter
     *
     * @see android.app.Activity#onStart()
     */
    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    /*
     * Stop scanning when app is terminated
     *
     * @see android.app.Activity#onStop()
     */
    @Override
    public void onStop() {
        super.onStop();
    }

    /*
     * Stop scanning when app is terminated
     *
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /*
     * Event return to main screen after terminating the current screen when a
     * found device is selected
     */
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            Bundle b = new Bundle();
            b.putString(BluetoothDevice.EXTRA_DEVICE, deviceList.get(position).getAddress());

            Intent result = new Intent();
            result.putExtras(b);
            setResult(Activity.RESULT_OK, result);
            finish();
        }
    };

    /*
     * Stop scanning if app stops
     *
     * @see android.app.Activity#onPause()
     */
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

}
