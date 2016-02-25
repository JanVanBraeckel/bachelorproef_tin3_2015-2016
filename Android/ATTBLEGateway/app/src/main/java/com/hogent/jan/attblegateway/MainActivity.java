package com.hogent.jan.attblegateway;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.hogent.jan.attblegateway.bluetoothWrapper.BleNamesResolver;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleWrapper;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleWrapperUiCallbacks;
import com.hogent.jan.attblegateway.recyclerview.DeviceListAdapter;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, DeviceListAdapter.DeviceClickedListener {
    private final String TAG = getClass().getSimpleName();
    private static final long SCANNING_TIMEOUT = 5 * 1000;
    private static final int ENABLE_BT_REQUEST_ID = 1;

    private boolean mScanning = false;
    private BleWrapper mBleWrapper = null;
    private Handler mHandler = new Handler();

    @Bind(R.id.device_list)
    RecyclerView mDeviceList;

    @Bind(R.id.swipeRefresh)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        mDeviceList.setLayoutManager(layoutManager);
        DeviceListAdapter adapter = new DeviceListAdapter(getApplicationContext());
        adapter.setDeviceClickedListener(this);
        mDeviceList.setAdapter(adapter);

        mSwipeRefreshLayout.setOnRefreshListener(this);

        mBleWrapper = new BleWrapper(this, new BleWrapperUiCallbacks.Null() {
            @Override
            public void uiDeviceFound(final BluetoothDevice device, final int rssi, final byte[] record) {
                Log.d(TAG, "uiDeviceFound() called with: " + "device = [" + device.getName() + "], rssi = [" + rssi + "], record = [" + record + "]");
                ((DeviceListAdapter) mDeviceList.getAdapter()).addDevice(device, rssi);
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

                    mBleWrapper.getCharacteristicsForService(service);
                }
            }

            @Override
            public void uiCharacteristicForService(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, List<BluetoothGattCharacteristic> chars) {
                for (BluetoothGattCharacteristic bluetoothGattCharacteristic : chars) {
                    String characteristicName = BleNamesResolver.resolveCharacteristicName(bluetoothGattCharacteristic.getUuid().toString());
                    Log.d(TAG, "uiCharacteristicForService() called with: " + "Characteristic found = [" + characteristicName + "]");
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
    public void onRefresh() {
        if (!mScanning) {
            mBleWrapper.disconnect();
            ((DeviceListAdapter)mDeviceList.getAdapter()).clearList();
            addScanningTimeout();
            mBleWrapper.startScanning();
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

        mScanning = true;
        addScanningTimeout();
        mBleWrapper.startScanning();
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                ((DeviceListAdapter)mDeviceList.getAdapter()).clearList();
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ENABLE_BT_REQUEST_ID) {
            if (resultCode == MainActivity.RESULT_CANCELED) {
                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScanning = false;
        mBleWrapper.stopScanning();
        mBleWrapper.disconnect();
        mBleWrapper.close();
    }

    private void addScanningTimeout() {
        Runnable timeout = new Runnable() {
            @Override
            public void run() {
                if(mBleWrapper == null) {
                    return;
                }
                mScanning = false;
                mBleWrapper.stopScanning();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        };
        mHandler.postDelayed(timeout, SCANNING_TIMEOUT);
    }

    @Override
    public void deviceClicked(BluetoothDevice device) {
        Log.d(TAG, "deviceClicked() called with: " + "device = [" + device.getName() + "]");
        mBleWrapper.connect(device.getAddress());
    }
}
