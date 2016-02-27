package com.sovate.activitymanager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;



public class MainActivity extends AppCompatActivity {

    // Constants
    private static final String TAG = "UartService";
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;


    // permission constants
    private final int MY_PERMISSION_GRANTED = 100;

    // Bluetooth variables
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    // UI variables
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect;
    private Button btnRemove;
    private final Handler handler = new Handler();

    // Paired Device List
    private List<BluetoothDevice> deviceList;
    private DeviceAdapter deviceAdapter;
    private Button btnGetPairedDevice;



    // Connection variables
    public final static String deviceName = "InBodyBand";
    private Boolean isBonded = false;
    private byte[] resultString = new byte[2048]; // Receiving buffer
    private byte[] lastBuf = null; // Re-sending Frame in case transfer fails
    private int offset = 0; // Received Data offset
    private int waitCnt = 0; // Connection status monitoring variables

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        checkPermission();

    }

    // 권한확인 코드
    private void checkPermission() {

        Log.i("", "!!!!! CheckPermission : " + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //사용권한이 없을경우

            //최초권한 요청인지 , 사용자에 의한 재요청인지 확인
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.e("", "@@@@@@@@ permission  재요청");
            }

            //최초로 권한을 요청하는경우(처음실행)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, MY_PERMISSION_GRANTED);


        } else {
            //사용 권한이 있는경우
            Log.e("", "@@@@@@@@@@@@@ ermission deny");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_GRANTED:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "권한 획득", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "권한 허용을 선택하지않은경우 정상동작을 보장할수없습니다.", Toast.LENGTH_LONG).show();
                }
                break;
        }


    }

    private void init() {

        // Check Bluetooth available to use
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // UI
        messageListView = (ListView) findViewById(R.id.listMessage);
        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);
        btnConnectDisconnect = (Button) findViewById(R.id.btn_select);
        btnRemove = (Button) findViewById(R.id.btn_remove);

        btnGetPairedDevice = (Button) findViewById(R.id.btn_getPairedDevice);


        service_init();

        // Handler Remove button
        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // unpair는 사용하지 않도록 구성
                // unpairDevice(GetPairedBLEDevice(deviceName));
                //Toast.makeText(v, "not support", Toast.LENGTH_SHORT).show();
                //Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "not support", Toast.LENGTH_SHORT).show();
            }
        });

        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {
                        listAdapter.clear();
                        //mDevice = GetPairedBLEDevice(deviceName);

                        // Start searching if no InBodyBAND connected
                        if (mDevice == null) {
                            isBonded = false;
                            Intent newIntent = new Intent(MainActivity.this, PairedDeviceListActivity.class);
                            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                        } else {
                            // Start connection if InBodyBAND connected
                            isBonded = true;
                            Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                            ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - connecting");
                            mService.connect(mDevice.getAddress());
                        }
                    } else {
                        // Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();
                        }
                    }
                }
            }
        });

        btnGetPairedDevice.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                // get device list
                GetPairedBLEDevices(MainActivity.deviceName);
            }
        });

        // Set initial UI state
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            // select text view에 해당 내용을 전달 하도록 구성

//            Bundle b = new Bundle();
//            b.putString(BluetoothDevice.EXTRA_DEVICE, deviceList.get(position).getAddress());
//
//            Intent result = new Intent();
//            result.putExtras(b);
//            setResult(Activity.RESULT_OK, result);
//            finish();
        }
    };

    private void GetPairedBLEDevices(String DeviceName) {
        // list 정보 구성
        deviceList = new ArrayList<BluetoothDevice>();
        deviceAdapter = new DeviceAdapter(this, deviceList);

        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(deviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

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

    // UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    /*
     * Event handler function received from UART service
     */
    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");

                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - ready");
                        listAdapter.add("[" + currentDateTimeString + "] Connected to: " + mDevice.getName());
                        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                        mState = UART_PROFILE_CONNECTED;

                        String _deviceAddress = "";
                        if (null != mDevice)
                            _deviceAddress = mDevice.getAddress(); // null 가능성
                        // null
                        // possibility

                        Log.d(TAG, "UART_CONNECT_MSG[UART_PROFILE_CONNECTED==" + mState + ", DeviceAddress:"
                                + _deviceAddress + "]");
                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");

                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        listAdapter.add("[" + currentDateTimeString + "] Disconnected to: " + mDevice.getName());
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();

                // Device bonding when service connected
                if (!isBonded)
                    pairDevice(mDevice);

                // set AK frame
                lastBuf = new byte[] { 0x02, 0x48, 0x0C, 0x0A, 0x41, 0x4B, 0x34, 0x03 };
                checkComm();
            }

            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                // Start reading data after connected
                if (isBonded) {
                    ReadData(intent);
                }
            }

            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }

        }
    };

    /*
     * Connect to UART service
     */
    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver,
                makeGattUpdateIntentFilter());
    }

    /*
     * UART service event filter
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /*
     * Kill the service when app terminated
     *
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;

    }

    /*
     * Check if Bluetooth is enabled when app starts
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /*
     * Receive from device list & Bluetooth Setting
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            // Case where found InBodyBAND is selected
            case REQUEST_SELECT_DEVICE:
                // When the DeviceListActivity return, with the selected device
                // address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - connecting");
                    mService.connect(deviceAddress);

                }
                break;

            // Case where Bluetooth turned on/off in the smartphone setting
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    /*
     * Toast messege
     */
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    /*
     * Handler function for back button
     *
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("InBodyBAND's running in background.\n             Disconnect to exit");
        } else {
            new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).setNegativeButton(R.string.popup_no, null).show();
        }
    }

    /*
     * garbage data trim.
     */
    private void Trim() {
        if (resultString[0] == 0x02)
            return;
        int cnt = 0;
        for (int i = 0; i < offset; i++)
            if (resultString[i] == 0x02) {
                cnt = i;
                break;
            }
        for (int j = 0; j < resultString.length; j++) {
            if (j < offset)
                resultString[j] = resultString[j + cnt];
            else
                resultString[j] = 0x00;
        }

        offset = offset - cnt;
    }

    /*
     * Bonding event receiver.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                switch (state) {
                    case BluetoothDevice.BOND_BONDING:
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        isBonded = true;
                        unregisterReceiver(mReceiver);
                        fncSendCommand((byte) 'A', (byte) 'K', null);
                        break;

                    case BluetoothDevice.BOND_NONE:
                        break;
                }
            }
        }
    };

    /*
     * Check Bonded device
     */
    private BluetoothDevice GetPairedBLEDevice(String DeviceName) {
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        BluetoothDevice returnBluetoothDevice = null;
        if (pairedDevices.size() != 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName() == null)
                    continue;
                if (DeviceName.equals(device.getName()))
                    returnBluetoothDevice = device;
            }
        }
        return returnBluetoothDevice;
    }

    /*
     * Bonding Bluetooth device
     */
    private void pairDevice(final BluetoothDevice device) {
        showMessage("InBodyBAND Bonding...");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unpairDevice(final BluetoothDevice device) {

        if (device == null) {
            showMessage("No InBodyBAND Bonded.");
            return;
        }
        showMessage("InBodyBAND removing...");
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
        }
    }

    /*
     * Make data frame
     */
    private void fncSendCommand(byte cmd1, byte cmd2, byte[] data) {
        if (data == null) {
            data = new byte[] {};
        }
        byte[] Buf = new byte[8 + data.length];
        Buf[0] = 0x02;
        Buf[1] = 'W';
        Buf[2] = (byte) (((Buf.length - 6) & 0x3f) + 0x0a);
        Buf[3] = (byte) ((((Buf.length - 6) >> 6) & 0x3f) + 0x0a);
        Buf[4] = cmd1;
        Buf[5] = cmd2;

        for (int i = 0; i < data.length; i++)
            Buf[6 + i] = data[i];

        // checksum
        Buf[Buf.length - 2] = 0x00;
        for (int i = 1; i < Buf.length - 2; i++) {
            Buf[Buf.length - 2] += Buf[i];
        }
        Buf[Buf.length - 2] = (byte) ((Buf[Buf.length - 2] & 0x3F) + 0X0A);

        Buf[Buf.length - 1] = 0x03;

        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
        }

        String log = "";
        for (int i = 0; i < Buf.length; i++)
            log = log += String.format("%02X ", Buf[i]);

        listAdapter.add("Snd : " + log);
        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
        lastBuf = Buf;
        SendData(Buf);
    }

    /*
     * No response check
     */
    private void checkComm() {
        handler.postDelayed(new Runnable() {
            public void run() {
                if (lastBuf != null && mService != null && mService.mConnectionState == UartService.STATE_CONNECTED
                        && mState == UART_PROFILE_CONNECTED) {
                    waitCnt++;
                    if (waitCnt > 4) {
                        if (mDevice != null) {
                            Log.e(TAG, "TIMEOUT");
                            // 요청사항이 느릴수도 있음.... 확인 요망.
                            //mService.disconnect();
                        }
                    } else if (waitCnt == 3) {
                        listAdapter.add("Resend buffer");
                        SendData(lastBuf);
                        checkComm();
                    } else {
                        checkComm();
                    }
                }
            }
        }, 1000);
    }

    /*
     * Send Data
     */
    private void SendData(byte[] value) {
        //
        if (value.length > 20) {
            byte[] temp = new byte[20];
            for (int i = 0; i < value.length / 20 + 1; i++) {
                int j = 0;
                for (j = 0; j < 20 && value.length > j + 20 * i; j++) {
                    temp[j] = value[j + 20 * i];
                }

                if (j != 19) {
                    temp = new byte[j];
                    for (int k = 0; k < j; k++)
                        temp[k] = value[k + 20 * i];
                }

                if (mService != null)
                    mService.writeRXCharacteristic(temp);
            }
        } else {
            if (mService != null)
                mService.writeRXCharacteristic(value);
        }
    }

    /*
     * AI command maker
     */
    private byte[] MakeAI() {
        Calendar c = Calendar.getInstance();
        int yyyy = c.get(Calendar.YEAR);
        int mm = c.get(Calendar.MONTH) + 1;
        int dd = c.get(Calendar.DAY_OF_MONTH);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);
        int sec = c.get(Calendar.SECOND);
        int weekDay = c.get(Calendar.DAY_OF_WEEK) - 1;

        double weight = 60;
        double height = 175;

        byte[] Buf = new byte[13];

        Buf[0] = (byte) (yyyy - 2000);
        Buf[1] = (byte) (mm);
        Buf[2] = (byte) (dd);
        Buf[3] = (byte) (weekDay);

        Buf[4] = (byte) (hour);
        Buf[5] = (byte) (min);
        Buf[6] = (byte) (sec);

        Buf[7] = (byte) ((int) ((weight * 10.0) / 256));
        Buf[8] = (byte) ((int) (weight * 10.0) % 256);
        Buf[9] = (byte) ((int) ((height * 10.0) / 256));
        Buf[10] = (byte) ((int) (height * 10.0) % 256);
        Buf[11] = 0x01;
        Buf[12] = (byte) (35); // Age
        return Buf;
    }

    /*
     * Read Data
     */
    private void ReadData(final Intent intent) {
        runOnUiThread(new Runnable() {
            public void run() {
                byte[] readBuf = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                int byteCnt = readBuf.length;

                try {

                    for (int i = 0; i < byteCnt; i++) {
                        resultString[offset + i] = readBuf[i];
                    }
                    offset += byteCnt;
                    Trim();

                    int nbyte = 0;
                    byte[] frame = null;
                    while (true) {
                        frame = new byte[offset];
                        for (byteCnt = 0; byteCnt < offset; byteCnt++) {
                            frame[byteCnt] = resultString[byteCnt];
                            if (frame[0] == 0x02 && byteCnt > 2) {
                                if (nbyte == 0) {
                                    nbyte = ((frame[2] - 0x0a) & 0x3f) + (((frame[3] - 0x0a) & 0x3f) << 6) + 4;
                                }
                                if (byteCnt > nbyte && resultString[byteCnt] == 0x03)
                                    break;
                            }
                        }
                        if (offset == byteCnt) {
                            break;
                        }

                        for (int j = 0; j < resultString.length; j++) {
                            if (j < byteCnt + 1) // Frame Shift
                                resultString[j] = resultString[j + byteCnt + 1];
                            else
                                resultString[j] = 0x0;
                        }
                        offset -= (byteCnt + 1);

                        String text = "";
                        for (int i = 0; i < frame.length; i++)
                            text = text += String.format("%02X ", frame[i]);
                        listAdapter.add("Rcv : " + text);
                        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);

                        waitCnt = 0;
                        lastBuf = null;
                        if (frame[4] == 'A' && frame[5] == 'K') {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                            fncSendCommand((byte) 'A', (byte) 'W', null);
                        } else if (frame[4] == 'A' && frame[5] == 'W') {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                            fncSendCommand((byte) 'A', (byte) 'I', MakeAI());
                        } else if (frame[4] == 'A' && frame[5] == 'I') {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                            fncSendCommand((byte) 'A', (byte) 'W', null);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

}