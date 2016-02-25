package com.hogent.jan.attblegateway;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hogent.jan.attblegateway.bluetoothWrapper.BleNamesResolver;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleWrapper;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleWrapperUiCallbacks;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.fromString;

public class MainActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();
    private final String TARGET = "HeartRateSketch";

    private static final int ENABLE_BT_REQUEST_ID = 1;
    private BleWrapper mBleWrapper = null;
    private mSensorState mState;
    private String gattList = "";

    private enum mSensorState {IDLE, ACC_ENABLE, ACC_READ}

    public final static UUID
            UUID_IRT_SERV = fromString("f000aa00-0451-4000-b000-000000000000"),
            UUID_IRT_DATA = fromString("f000aa01-0451-4000-b000-000000000000"),
            UUID_IRT_CONF = fromString("f000aa02-0451-4000-b000-000000000000"), // 0: disable, 1: enable

            UUID_ACC_SERV = fromString("f000aa10-0451-4000-b000-000000000000"),
            UUID_ACC_DATA = fromString("f000aa11-0451-4000-b000-000000000000"),
            UUID_ACC_CONF = fromString("f000aa12-0451-4000-b000-000000000000"), // 0: disable, 1: enable
            UUID_ACC_PERI = fromString("f000aa13-0451-4000-b000-000000000000"), // Period in tens of milliseconds

            UUID_HUM_SERV = fromString("f000aa20-0451-4000-b000-000000000000"),
            UUID_HUM_DATA = fromString("f000aa21-0451-4000-b000-000000000000"),
            UUID_HUM_CONF = fromString("f000aa22-0451-4000-b000-000000000000"), // 0: disable, 1: enable

            UUID_MAG_SERV = fromString("f000aa30-0451-4000-b000-000000000000"),
            UUID_MAG_DATA = fromString("f000aa31-0451-4000-b000-000000000000"),
            UUID_MAG_CONF = fromString("f000aa32-0451-4000-b000-000000000000"), // 0: disable, 1: enable
            UUID_MAG_PERI = fromString("f000aa33-0451-4000-b000-000000000000"), // Period in tens of milliseconds

            UUID_BAR_SERV = fromString("f000aa40-0451-4000-b000-000000000000"),
            UUID_BAR_DATA = fromString("f000aa41-0451-4000-b000-000000000000"),
            UUID_BAR_CONF = fromString("f000aa42-0451-4000-b000-000000000000"), // 0: disable, 1: enable
            UUID_BAR_CALI = fromString("f000aa43-0451-4000-b000-000000000000"), // Calibration characteristic

            UUID_GYR_SERV = fromString("f000aa50-0451-4000-b000-000000000000"),
            UUID_GYR_DATA = fromString("f000aa51-0451-4000-b000-000000000000"),
            UUID_GYR_CONF = fromString("f000aa52-0451-4000-b000-000000000000"), // 0: disable, bit 0: enable x, bit 1: enable y, bit 2: enable z

            UUID_KEY_SERV = fromString("0000ffe0-0000-1000-8000-00805f9b34fb"),
            UUID_KEY_DATA = fromString("0000ffe1-0000-1000-8000-00805f9b34fb"),
            UUID_CCC_DESC = fromString("00002902-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBleWrapper = new BleWrapper(this, new BleWrapperUiCallbacks.Null() {
            @Override
            public void uiDeviceFound(final BluetoothDevice device, final int rssi, final byte[] record) {
                Log.d(TAG, "uiDeviceFound() called with: " + "device = [" + device.getName() + "], rssi = [" + rssi + "], record = [" + record + "]");
                if(device.getName().equals(TARGET)){
                    if(!mBleWrapper.connect(device.getAddress())){
                        Log.d(TAG, "uiDeviceFound() failed to connect to remote device");
                    }
                }
            }

            @Override
            public void uiDeviceConnected(BluetoothGatt gatt, BluetoothDevice device) {
                Log.d(TAG, "uiDeviceConnected() called with: " + "state = [" + mBleWrapper.getAdapter().getState() + "]");
            }

            @Override
            public void uiDeviceDisconnected(BluetoothGatt gatt, BluetoothDevice device) {
                Log.d(TAG, "uiDeviceDisconnected() called with: " + "state = [" + mBleWrapper.getAdapter().getState() + "]");
                if (gatt != null) {
                    gatt.disconnect();
                }
            }

            @Override
            public void uiAvailableServices(BluetoothGatt gatt, BluetoothDevice device, List<BluetoothGattService> services) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic;

                for (BluetoothGattService service : services) {
                    String serviceName = BleNamesResolver.resolveUuid(service.getUuid().toString());
                    Log.d(TAG, "uiAvailableServices() called with: " + "Service found = [" + serviceName + "]");
                    gattList += serviceName + "\n";

                    mBleWrapper.getCharacteristicsForService(service);
                }
            }

            @Override
            public void uiCharacteristicForService(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, List<BluetoothGattCharacteristic> chars) {
                for (BluetoothGattCharacteristic bluetoothGattCharacteristic : chars) {
                    String characteristicName = BleNamesResolver.resolveCharacteristicName(bluetoothGattCharacteristic.getUuid().toString());
                    Log.d(TAG, "uiCharacteristicForService() called with: " + "Characteristic found = [" + characteristicName + "]");
                    gattList += "Characteristic: " + characteristicName + "\n";
                }
            }

            @Override
            public void uiSuccessfulWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String description) {
                Log.d(TAG, "uiSuccessfulWrite() called");

//                BluetoothGattCharacteristic bluetoothGattCharacteristic;
//
//                switch (mState) {
//                    case ACC_ENABLE:
//                        Log.d(TAG, "uiSuccessfulWrite(): Reading acc");
//                        bluetoothGattCharacteristic = gatt.getService(UUID_ACC_SERV).getCharacteristic(UUID_ACC_DATA);
//                        mBleWrapper.requestCharacteristicValue(bluetoothGattCharacteristic);
//                        mState = mSensorState.ACC_READ;
//                        break;
//
//                    case ACC_READ:
//                        Log.d(TAG, "uiSuccessfulWrite(): state = ACC_READ");
//                        break;
//
//                    default:
//                        break;
//                }
            }

            @Override
            public void uiFailedWrite(BluetoothGatt gatt,
                                      BluetoothDevice device,
                                      BluetoothGattService service,
                                      BluetoothGattCharacteristic ch,
                                      String description) {
                Log.d(TAG, "uiFailedWrite() called with: " + "description = [" + description + "]");
            }

            @Override
            public void uiNewValueForCharacteristic(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String strValue, int intValue, byte[] rawValue, String timestamp) {
                Log.d(TAG, "uiNewValueForCharacteristic() called");
                for (byte b : rawValue) {
                    Log.d(TAG, "Val: " + b);
                }
            }

            @Override
            public void uiGotNotification(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch) {
                String characteristic = BleNamesResolver.resolveCharacteristicName(ch.getUuid().toString());

                Log.d(TAG, "uiGotNotification() called with: " + "characteristic = [" + characteristic + "]");
            }
        });

        if (!mBleWrapper.checkBleHardwareAvailable()) {
            Toast.makeText(this, "Device doesn't support BLE", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBleWrapper.isBtEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_ID);
        }

        mBleWrapper.initialize();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ENABLE_BT_REQUEST_ID){
            if(resultCode == MainActivity.RESULT_CANCELED){
                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBleWrapper.diconnect();
        mBleWrapper.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.action_scan):
                startScan();
                break;

            case (R.id.action_stop):
                stopScan();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startScan() {
        Log.d(TAG, "startScan() called");
        mBleWrapper.startScanning();
    }

    private void stopScan() {
        Log.d(TAG, "stopScan() called");
        mBleWrapper.stopScanning();
    }
}
