package com.socketbytes.hexiscale.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.socketbytes.hexiscale.R;
import com.socketbytes.hexiscale.model.Characteristic;
import com.socketbytes.hexiscale.model.HexiwearDevice;
import com.socketbytes.hexiscale.services.BluetoothLeService;
import com.socketbytes.hexiscale.services.HexiwearService;
import com.socketbytes.hexiscale.util.ApplicationSession;
import com.socketbytes.hexiscale.util.DataConverter;
import com.socketbytes.hexiscale.util.HexiwearDevices;
import com.wolkabout.wolk.Device;
import com.wolkabout.wolk.ReadingType;
import com.wolkabout.wolk.Wolk;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private static final float CONSTANT_OFFSET = 0f;
    private static final float SCALE = 1000f;
    private static final float WEIGHT_RESOLUTION = 0.0000407059f;
//    private static final float WEIGHT_RESOLUTION = 0.0000386837f;
    private HexiwearService hexiwearService;
    private final ArrayList<String> uuidArray = new ArrayList<>();
    TextView txtView_temperature;
    TextView txtView_weight;
    Button btnSyncData;
    Wolk wolk;
    String uuid;
    byte[] data;
    float weight;
    String weightString;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_activty);

        uuidArray.add(HexiwearService.UUID_CHAR_TEMPERATURE);
        uuidArray.add(HexiwearService.UUID_CHAR_HUMIDITY);
        uuidArray.add(HexiwearService.UUID_CHAR_PRESSURE);

        Typeface thin= Typeface.createFromAsset(this.getAssets(), "Roboto-Thin.ttf");
        Typeface light= Typeface.createFromAsset(this.getAssets(), "Roboto-Light.ttf");

        //txtView_temperature = (TextView) findViewById(R.id.temperature_label);
        txtView_weight = (TextView) findViewById(R.id.weight_label);
        btnSyncData = (Button) findViewById(R.id.sync_button);

        txtView_weight.setTypeface(thin);
        btnSyncData.setTypeface(light);

        btnSyncData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(weightString!=null && !weightString.isEmpty() && !uuid.isEmpty())
                SynchronizeData(uuid,weightString);
            }
        });
    }

//    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//                invalidateOptionsMenu();
//                Intent intentAct = new Intent(HomeActivity.this, DeviceScanActivity.class);
//                startActivity(intentAct);
//            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
//                uuid = intent.getStringExtra(BluetoothLeService.EXTRA_CHAR);
//                displayCharData(uuid, data);
//            }
//        }
//    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                invalidateOptionsMenu();
                Intent intentAct = new Intent(HomeActivity.this, DeviceScanActivity.class);
                startActivity(intentAct);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                weight = intent.getIntExtra(BluetoothLeService.EXTRA_DATA, 0);
                uuid = intent.getStringExtra(BluetoothLeService.EXTRA_CHAR);
                displayWeight(uuid, weight);
            }
        }
    };

    @Override

    protected void onResume() {
        super.onResume();
        hexiwearService = new HexiwearService(uuidArray);
        hexiwearService.readCharStart(10);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        hexiwearService.readCharStop();
        unregisterReceiver(mGattUpdateReceiver);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void displayData(TextView txtView, String data) {
        if (data != null) {
            txtView.setText(data);
        }
    }

    private void displayCharData(String uuid, byte[] data) {
        String tmpString;
        int tmpLong;
        float tmpFloat;

        if (uuid.equals(HexiwearService.UUID_CHAR_TEMPERATURE)) {
            tmpLong = (((int) data[1]) << 8) | (data[0] & 0xff);
            tmpFloat = (float) tmpLong / 100;
            tmpString = tmpFloat + (" \u2103");
            displayData(txtView_temperature, tmpString);
        }
        else if (uuid.equals(HexiwearService.UUID_CHAR_PRESSURE)) {
            tmpLong = (data[1] << 8) & 0xff00 | (data[0] & 0xff);
            tmpFloat = ((float)tmpLong / 100)- CONSTANT_OFFSET;
            tmpString = tmpFloat + (" Kg");
            displayData(txtView_weight, tmpString);
        }
    }

    private void displayWeight(String uuid, float data) {
        String tmpString;
        if (uuid.equals(HexiwearService.UUID_CHAR_PRESSURE)) {
            float kgWeight = data * SCALE * WEIGHT_RESOLUTION;
            DecimalFormat df = new DecimalFormat("#.##");
            weightString = df.format(kgWeight);
            tmpString = df.format(kgWeight) + (" Kg");
            displayData(txtView_weight, tmpString);
        }
    }

    private void SynchronizeData(final String uuid, final String data){

        Thread thread = new Thread(){
            public void run(){
                try {
                    HexiwearDevice hexiwearDevice = HexiwearDevices.getInstance(HomeActivity.this).getDevice(DeviceScanActivity.KWARP_ADDRESS);
                    wolk = new Wolk(hexiwearDevice);
                    final Characteristic characteristic = Characteristic.byUuid(uuid);
                    //if (wolk != null && HexiwearDevices.getInstance(HomeActivity.this).shouldTransmit(hexiwearDevice) && characteristic != Characteristic.BATTERY) {
                    final ReadingType readingType = ReadingType.valueOf(characteristic.name());
                    wolk.addReading(readingType, data);
                    wolk.publish();
                    //}
                    stopDialog();
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        };
        thread.start();
        startDialog("Publishing....");

//        if (HexiwearDevices.getInstance(this).shouldTransmit(ApplicationSession.getInstance().getBluetoothDevice())) {
//            final int publishInterval = HexiwearDevices.getInstance(this).getPublishInterval(hexiwearDevice);
//            wolk.startAutoPublishing(publishInterval);
//        }



    }

    private void startDialog(String message){
        dialog = new ProgressDialog(this);
        dialog.setMessage(message);
        dialog.show();
    }

    private void stopDialog(){
        if(dialog!=null){
            dialog.dismiss();
        }
    }
}
