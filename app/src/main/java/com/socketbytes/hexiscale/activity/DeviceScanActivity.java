package com.socketbytes.hexiscale.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.socketbytes.hexiscale.R;
import com.socketbytes.hexiscale.model.HexiwearDevice;
import com.socketbytes.hexiscale.services.BluetoothLeService;
import com.socketbytes.hexiscale.services.NotificationService;
import com.socketbytes.hexiscale.util.ApplicationSession;
import com.socketbytes.hexiscale.util.HexiwearDevices;
import com.wolkabout.wolkrestandroid.dto.CreatePointBodyDTO;
import com.wolkabout.wolkrestandroid.dto.CreatedPointDto;
import com.wolkabout.wolkrestandroid.dto.PointWithFeedsResponse;
import com.wolkabout.wolkrestandroid.dto.SerialDto;
import com.wolkabout.wolkrestandroid.enumeration.SensorType;
import com.wolkabout.wolkrestandroid.service.DeviceService;
import com.wolkabout.wolkrestandroid.service.DeviceService_;
import com.wolkabout.wolkrestandroid.service.PointService;
import com.wolkabout.wolkrestandroid.service.PointService_;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class DeviceScanActivity extends AppCompatActivity {

    private static final String TAG = "DeviceScanActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    public static final String UUID_CHAR_ALERTIN = "00002031-0000-1000-8000-00805f9b34fb";
    private BluetoothGattCharacteristic alertInCharacteristic;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    public static final String KWARP_ADDRESS = "00:41:40:0A:00:09";
    private TextView mScanTitle;
    private String mDeviceAddress;
    private BluetoothDevice device;
    private final String LIST_UUID = "UUID";
    private static BluetoothLeService mBluetoothLeService;
    private static ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    public static ArrayList<ArrayList<BluetoothGattCharacteristic>> getGattCharacteristics() {return mGattCharacteristics;}
    public static BluetoothLeService getBluetoothLeService() {
        return mBluetoothLeService;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        mHandler = new Handler();
        mScanTitle = (TextView) findViewById(R.id.scanTitle);
        mDeviceAddress = KWARP_ADDRESS;

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This App needs your location access");
                builder.setMessage("please grant location access so this app can detect beacons");
                builder.setPositiveButton("Ok",null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                Log.e("PERMISSION", "No permission granted yet");
                builder.show();
                return;
            }
        }

        checkNotificationEnabled();
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));
        ComponentName name = startService(new Intent(DeviceScanActivity.this, NotificationService.class));
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        scanLeDevice(true);
        Intent gattServiceIntent = new Intent(DeviceScanActivity.this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics.clear();
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();
                if(uuid.equals(UUID_CHAR_ALERTIN)) {
                    alertInCharacteristic = gattCharacteristic;
                    byte[] value = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
                    alertInCharacteristic.setValue(value);
                }
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(device!=null) {
                    ApplicationSession.getInstance().setBluetoothDevice(device);
                    registerHexiwearDevice(device);
                }
                Intent intent = new Intent(DeviceScanActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        }, 1500);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_WRITE_RESPONSE_OK);
        intentFilter.addAction(BluetoothLeService.ACTION_WRITE_RESPONSE_ERROR);
        return intentFilter;
    }

    private BroadcastReceiver onNotice = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("text");
            if ((alertInCharacteristic != null) && (mBluetoothLeService != null)) {
                int charaProp = alertInCharacteristic.getProperties();
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    byte[] bytes = Arrays.copyOf(text.getBytes(), 20);
                    alertInCharacteristic.setValue(bytes);
                    while(!mBluetoothLeService.writeNoResponseCharacteristic(alertInCharacteristic)) {
                        try {
                            Thread.sleep(50);
                        }
                        catch (InterruptedException e) {
                            Log.e(TAG, "InterruptedException");
                        }
                    }
                }
            }
        }
    };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mScanTitle.setText(R.string.device_connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

            } else if ((BluetoothLeService.ACTION_WRITE_RESPONSE_OK.equals(action)) || (BluetoothLeService.ACTION_WRITE_RESPONSE_ERROR.equals(action))) {

            }

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    public boolean checkNotificationEnabled() {
        try {
            if (Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners").contains(getApplicationContext().getPackageName())) {
                return true;
            } else {
                //service is not enabled try to enabled by calling...
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            try {
                if (device.getAddress().equals(KWARP_ADDRESS)) {
                    mBluetoothLeService.connect(mDeviceAddress);
                    DeviceScanActivity.this.device = device;
                    scanLeDevice(false);
                }
                Log.e("BLE", KWARP_ADDRESS + " " + device.getAddress() + " " + device.getName());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
        mBluetoothLeService = null;
        stopService(new Intent(DeviceScanActivity.this, NotificationService.class));
        mGattCharacteristics.clear();
        unbindService(mServiceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "coarse location permission granted");
                    checkNotificationEnabled();
                    LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));
                    ComponentName name = startService(new Intent(DeviceScanActivity.this, NotificationService.class));
                    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                    scanLeDevice(true);
                    Intent gattServiceIntent = new Intent(DeviceScanActivity.this, BluetoothLeService.class);
                    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
            }
        }
    }

    private void registerHexiwearDevice(final BluetoothDevice device){

        Thread thread = new Thread(){
            @Override
            public void run (){
                DeviceService_ deviceService = new DeviceService_(DeviceScanActivity.this);
                final SerialDto serialDto = deviceService.getRandomSerial(SensorType.HEXIWEAR);
                final String serial = serialDto.getSerial();

                PointService pointService = new PointService_(DeviceScanActivity.this);
                List<PointWithFeedsResponse> points = pointService.getPoints();
                final List<HexiwearDevice> hexiwearDevices = HexiwearDevices.getDevices(points);
                if(hexiwearDevices!=null && hexiwearDevices.isEmpty()) {
                    registerHexiwearDevice(device, serial, "Hexiscale");
                }else if(hexiwearDevices!=null){
                    deactivateDeviceAndRegister(device, serial, "Hexiscale",hexiwearDevices.get(0).getSerialId());
                }
            }
        };

        thread.start();

    }

    void registerHexiwearDevice(final BluetoothDevice device, final String serial, final String wolkName) {
        Log.d(TAG, "Registering hexiwear device.");
        final CreatePointBodyDTO bodyDto = new CreatePointBodyDTO(wolkName, "", "T:ON,P:ON,H:ON");
        final ArrayList<CreatePointBodyDTO> bodyDtos = new ArrayList<>();
        bodyDtos.add(bodyDto);
        final List<CreatedPointDto> response = new DeviceService_(DeviceScanActivity.this).createPointWithThings(serial, bodyDtos);
        if (!response.isEmpty()) {
            final String name = device.getName();
            final String address = device.getAddress();
            final String password = response.get(0).getPassword();
            final HexiwearDevice hexiwearDevice = new HexiwearDevice(name, serial, address, password, wolkName);
            HexiwearDevices.getInstance(DeviceScanActivity.this).storeDevice(hexiwearDevice);
            Log.d(TAG, "Hexiwear registered." + hexiwearDevice);
        } else {

        }
    }

    void deactivateDeviceAndRegister(final BluetoothDevice device, final String serial, final String wolkName, String existingDeviceSerial) {
        try {
            Log.d(TAG, "Deactivating existing device...");
            new DeviceService_(DeviceScanActivity.this).deactivateDevice(existingDeviceSerial);
            Log.d(TAG, "Deactivated.");
            registerHexiwearDevice(device, serial, wolkName);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

}
