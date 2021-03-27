package com.healthcarelab.wearable.mibandapp2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.CheckedOutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    Boolean isListeningHeartRate = false;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    BluetoothAdapter bluetoothAdapter;
    BluetoothGatt bluetoothGatt;
    BluetoothDevice bluetoothDevice;

    Button btnStartConnecting, btnGetBatteryInfo, btnWalkingInfo, btnStartVibrate, btnStopVibrate, btnDeviceScan;
    ToggleButton btnGetHeartRate;
    EditText txtPhysicalAddress;
    TextView txtState, txtByte;
    private String mDeviceName;
    private String mDeviceAddress;

    // 파이어 베이스
    private long Time;
    private String day_s;
    private String time_s;
    SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat timetime = new SimpleDateFormat("HH:mm:ss");
    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference("미밴드데이터/심박수/");

    Timer timer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeObjects();
        initilaizeComponents();
        initializeEvents();

        getBoundedDevice();
    }

    void getBoundedDevice() {

        mDeviceName = getIntent().getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = getIntent().getStringExtra(EXTRAS_DEVICE_ADDRESS);
        txtPhysicalAddress.setText(mDeviceAddress);

        Set<BluetoothDevice> boundedDevice = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bd : boundedDevice) {
            if (bd.getName().contains("MI Band 2")) {
                txtPhysicalAddress.setText(bd.getAddress());
            }
        }
    }

    void initializeObjects() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    void initilaizeComponents() {
        btnStartConnecting = (Button) findViewById(R.id.btnStartConnecting);
        btnGetBatteryInfo = (Button) findViewById(R.id.btnGetBatteryInfo);
        btnWalkingInfo = (Button) findViewById(R.id.btnWalkingInfo);
        btnStartVibrate = (Button) findViewById(R.id.btnStartVibrate);
        btnStopVibrate = (Button) findViewById(R.id.btnStopVibrate);
        btnDeviceScan = (Button) findViewById(R.id.btnDeviceScan);
        btnGetHeartRate = (ToggleButton) findViewById(R.id.btnGetHeartRate);
        txtPhysicalAddress = (EditText) findViewById(R.id.txtPhysicalAddress);
        txtState = (TextView) findViewById(R.id.txtState);
        txtByte = (TextView) findViewById(R.id.txtByte);
    }

    void initializeEvents() {
        btnStartConnecting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConnecting();
            }
        });
        btnGetBatteryInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBatteryStatus();
            }
        });
        btnStartVibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVibrate();
            }
        });
        btnStopVibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVibrate();
            }
        });
        btnGetHeartRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btnGetHeartRate.isChecked()){
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            startScanHeartRate();
                        }
                    }, 0, 5000);
                } else {
                    timer.cancel();
                    timer = null;
                    Toast.makeText(MainActivity.this, "측정 종료", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnWalkingInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRealtimeStepStatus();
            }
        });
        btnDeviceScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DeviceScanActivity.class);
                startActivity(intent);
            }
        });
    }

    void startConnecting() {

        String address = txtPhysicalAddress.getText().toString();
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);

        Log.v(TAG, "Connecting to " + address);
        Log.v(TAG, "Device name " + bluetoothDevice.getName());

        bluetoothGatt = bluetoothDevice.connectGatt(this, true, bluetoothGattCallback);

    }

    void stateConnected() {
        // 블루투스 신호 연결 되었을 때
        bluetoothGatt.discoverServices();
        txtState.setText("Connected");
    }

    void stateDisconnected() {
        bluetoothGatt.disconnect();
        txtState.setText("Disconnected");
    }

    void startScanHeartRate() {
        if (bluetoothGatt != null) {
            BluetoothGattCharacteristic bchar = bluetoothGatt.getService(CustomBluetoothProfile.HeartRate.service)
                    .getCharacteristic(CustomBluetoothProfile.HeartRate.controlCharacteristic);
            bchar.setValue(new byte[]{21, 1, 1}); // {21, 2, 1} : 1번측정
            bluetoothGatt.writeCharacteristic(bchar);
        } else {
            startConnecting();
            Toast.makeText(this, "Re try.", Toast.LENGTH_SHORT).show();
        }
    }

    void listenHeartRate() {
        BluetoothGattCharacteristic bchar = bluetoothGatt.getService(CustomBluetoothProfile.HeartRate.service)
                .getCharacteristic(CustomBluetoothProfile.HeartRate.measurementCharacteristic);
        bluetoothGatt.setCharacteristicNotification(bchar, true);
        BluetoothGattDescriptor descriptor = bchar.getDescriptor(CustomBluetoothProfile.HeartRate.descriptor);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);
        isListeningHeartRate = true;
    }

    void getBatteryStatus() {
        if (bluetoothGatt != null) {
            txtByte.setText("...");
            BluetoothGattCharacteristic bchar = bluetoothGatt.getService(CustomBluetoothProfile.Basic.service)
                    .getCharacteristic(CustomBluetoothProfile.Basic.batteryCharacteristic);
            if (!bluetoothGatt.readCharacteristic(bchar)) {
                Toast.makeText(this, "Failed get battery info", Toast.LENGTH_SHORT).show();
            }
        } else {
            startConnecting();
            Toast.makeText(this, "Re try.", Toast.LENGTH_SHORT).show();
        }
    }

    void getRealtimeStepStatus() {
        if (bluetoothGatt != null) {
            txtByte.setText("...");
            BluetoothGattCharacteristic bchar = bluetoothGatt.getService(CustomBluetoothProfile.Basic.service)
                    .getCharacteristic(CustomBluetoothProfile.Basic.realtimeStepCharacteristic);
            if (!bluetoothGatt.readCharacteristic(bchar)) {
                Toast.makeText(this, "Failed get Step info", Toast.LENGTH_SHORT).show();
            }
        } else {
            startConnecting();
            Toast.makeText(this, "Re try.", Toast.LENGTH_SHORT).show();
        }
    }

    void startVibrate() {
        if (bluetoothGatt != null) {
            BluetoothGattCharacteristic bchar = bluetoothGatt.getService(CustomBluetoothProfile.AlertNotification.service)
                    .getCharacteristic(CustomBluetoothProfile.AlertNotification.alertCharacteristic);
            bchar.setValue(new byte[]{2});
            if (!bluetoothGatt.writeCharacteristic(bchar)) {
                Toast.makeText(this, "Failed start vibrate", Toast.LENGTH_SHORT).show();
            }
        } else {
            startConnecting();
            Toast.makeText(this, "Re try.", Toast.LENGTH_SHORT).show();
        }
    }

    void stopVibrate() {
        if (bluetoothGatt != null) {
            BluetoothGattCharacteristic bchar = bluetoothGatt.getService(CustomBluetoothProfile.AlertNotification.service)
                    .getCharacteristic(CustomBluetoothProfile.AlertNotification.alertCharacteristic);
            bchar.setValue(new byte[]{0});
            if (!bluetoothGatt.writeCharacteristic(bchar)) {
                Toast.makeText(this, "Failed stop vibrate", Toast.LENGTH_SHORT).show();
            }
        } else {
            startConnecting();
            Toast.makeText(this, "Re try.", Toast.LENGTH_SHORT).show();
        }
    }

    final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.v(TAG, "onConnectionStateChange");

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                stateConnected();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                stateDisconnected();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.v(TAG, "onServicesDiscovered");
            listenHeartRate();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            byte[] data = characteristic.getValue();
            Log.i(TAG, "onCharacteristicRead/setValue: " + Arrays.toString(data));
            txtByte.setText(Arrays.toString(data));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.v(TAG, "onCharacteristicWrite");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] data = characteristic.getValue();
            Log.i(TAG, "onCharacteristicChanged/setValue: " + Arrays.toString(data));
            Time = System.currentTimeMillis();
            day_s = dayTime.format(new Date(Time));
            time_s = timetime.format(new Date(Time));
            databaseReference.child(bluetoothDevice.getAddress()).child(day_s).child(time_s).setValue((int) data[1]);
            txtByte.setText(Arrays.toString(data));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.v(TAG, "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.v(TAG, "onDescriptorWrite");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.v(TAG, "onReliableWriteCompleted");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.v(TAG, "onReadRemoteRssi");
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.v(TAG, "onMtuChanged");
        }

    };

}
