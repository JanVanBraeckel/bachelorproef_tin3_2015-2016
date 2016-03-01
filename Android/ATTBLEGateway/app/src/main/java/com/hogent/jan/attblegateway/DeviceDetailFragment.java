package com.hogent.jan.attblegateway;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.hogent.jan.attblegateway.ExpandableListView.ExpandableListAdapter;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleNamesResolver;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleWrapper;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleWrapperUiCallbacks;
import com.hogent.jan.attblegateway.recyclerview.DeviceListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DeviceDetailFragment extends Fragment implements BleWrapperUiCallbacks, ExpandableListAdapter.ExpandableListAdapterListener{
    private final String TAG = getClass().getSimpleName();
    private static final String BLE_NAME = "deviceName";
    private static final String BLE_ADDRESS = "deviceAddress";
    private static final String BLE_RSSI = "deviceRssi";

    private BleWrapper mBleWrapper = null;
    private ExpandableListAdapter mListAdapter;

    private String mDeviceName = "";
    private String mDeviceAddress = "";
    private int mDeviceRssi =0;

    @Bind(R.id.deviceDetailAddress)
    TextView mDeviceAddressView;

    @Bind(R.id.deviceDetailRssi)
    TextView mDeviceRssiView;

    @Bind(R.id.deviceDetailName)
    TextView mDeviceNameView;

    @Bind(R.id.deviceServices)
    ExpandableListView mExpandableListView;

    public static DeviceDetailFragment newInstance(String name, String address, int rssi) {
        DeviceDetailFragment fragment = new DeviceDetailFragment();
        Bundle args = new Bundle();
        args.putString(BLE_NAME, name);
        args.putString(BLE_ADDRESS, address);
        args.putInt(BLE_RSSI, rssi);
        fragment.setArguments(args);
        return fragment;
    }

    public DeviceDetailFragment() {
    }

    @Override
    public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {
        //
    }

    @Override
    public void uiDeviceConnected(BluetoothGatt gatt, BluetoothDevice device) {
        Log.d(TAG, "uiDeviceConnected() called with: " + "state = [" + mBleWrapper.getAdapter().getState() + "]");
    }

    @Override
    public void uiDeviceDisconnected(BluetoothGatt gatt, BluetoothDevice device) {
        Log.d(TAG, "uiDeviceDisconnected() called with: " + "state = [" + mBleWrapper.getAdapter().getState() + "]");
    }

    @Override
    public void uiNewRssiAvailable(BluetoothGatt gatt, BluetoothDevice device, final int rssi) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceRssi = rssi;
                mDeviceRssiView.setText(rssi + " db");
            }
        });
    }

    @Override
    public void uiAvailableServices(BluetoothGatt gatt, BluetoothDevice device, final List<BluetoothGattService> services) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListAdapter.addServices(services);
            }
        });

        for (BluetoothGattService service : services) {
            final String serviceName = BleNamesResolver.resolveUuid(service.getUuid().toString());
            Log.d(TAG, "uiAvailableServices() called with: " + "Service found = [" + serviceName + "]");

            mBleWrapper.getCharacteristicsForService(service);
        }
    }

    @Override
    public void uiCharacteristicForService(BluetoothGatt gatt, BluetoothDevice device, final BluetoothGattService service, final List<BluetoothGattCharacteristic> chars) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListAdapter.addCharacteristicsForService(service, chars);
            }
        });

        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : chars) {
            String characteristicName = BleNamesResolver.resolveCharacteristicName(bluetoothGattCharacteristic.getUuid().toString());
            Log.d(TAG, "uiCharacteristicForService() called with: " + "Characteristic found = [" + characteristicName + "]");
        }
    }

    @Override
    public void uiCharacteristicsDetails(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "uiCharacteristicsDetails() called with: " + "gatt = [" + gatt + "], device = [" + device + "], service = [" + service + "], characteristic = [" + characteristic + "]");
    }

    @Override
    public void uiNewValueForCharacteristic(BluetoothGatt gatt, BluetoothDevice device, final BluetoothGattService service, final BluetoothGattCharacteristic ch, final String strValue, final int intValue, final byte[] rawValue, final String timestamp) {
        Log.d(TAG, "uiNewValueForCharacteristic() called with: " + "gatt = [" + gatt + "], device = [" + device + "], service = [" + service.getUuid() + "], ch = [" + ch.getUuid() + "], strValue = [" + strValue + "], intValue = [" + intValue + "], rawValue = [" + rawValue + "], timestamp = [" + timestamp + "]");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListAdapter.newValueForCharacteristic(service, ch, strValue, intValue, rawValue, timestamp);
            }
        });
    }

    @Override
    public void uiSuccessfulWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String description) {

    }

    @Override
    public void uiFailedWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String description) {

    }

    @Override
    public void uiGotNotification(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "uiGotNotification() called with: " + "gatt = [" + gatt + "], device = [" + device + "], service = [" + service + "], characteristic = [" + characteristic + "]");
    }

    @Override
    public void requestCharacteristicValue(BluetoothGattCharacteristic characteristic) {
        mBleWrapper.requestCharacteristicValue(characteristic);
    }

    @Override
    public void writeDataToCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        mBleWrapper.writeDataToCharacteristic(characteristic, data);
    }

    @Override
    public int getValueFormat(BluetoothGattCharacteristic characteristic) {
        return mBleWrapper.getValueFormat(characteristic);
    }

    @Override
    public void setNotificationForCharacteristic(BluetoothGattCharacteristic characteristic, boolean enabled) {
        mBleWrapper.setNotificationForCharacteristic(characteristic, enabled);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDeviceName = getArguments().getString(BLE_NAME);
            mDeviceAddress = getArguments().getString(BLE_ADDRESS);
            mDeviceRssi = getArguments().getInt(BLE_RSSI);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mBleWrapper == null){
            mBleWrapper = new BleWrapper(getActivity(), this);
        }

        if(!mBleWrapper.initialize()){
            getActivity().finish();
        }

        mListAdapter = new ExpandableListAdapter(getContext());
        mListAdapter.setExpandableListAdapterListener(this);
        mExpandableListView.setAdapter(mListAdapter);

        mBleWrapper.connect(mDeviceAddress);
    }

    @Override
    public void onPause() {
        super.onPause();

        mListAdapter.clearLists();

        mBleWrapper.stopMonitoringRssiValue();
        mBleWrapper.disconnect();
        mBleWrapper.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_device_detail, container, false);

        ButterKnife.bind(this, v);

        mDeviceAddressView.setText(mDeviceAddress);
        mDeviceRssiView.setText(mDeviceRssi + " db");
        mDeviceNameView.setText(mDeviceName);

        return v;
    }
}
