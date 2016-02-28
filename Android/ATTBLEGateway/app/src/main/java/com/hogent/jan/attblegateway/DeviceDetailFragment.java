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

public class DeviceDetailFragment extends Fragment {
    private final String TAG = getClass().getSimpleName();
    private static final String ARG_PARAM = "bleDevice";

    private BleWrapper mBleWrapper = null;
    private ExpandableListAdapter mListAdapter;
    private BluetoothDevice mBleDevice;

    @Bind(R.id.deviceServices)
    ExpandableListView mExpandableListView;

    public static DeviceDetailFragment newInstance(BluetoothDevice device) {
        DeviceDetailFragment fragment = new DeviceDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAM, device);
        fragment.setArguments(args);
        return fragment;
    }

    public DeviceDetailFragment() {
    }

    public BluetoothDevice getBleDevice() {
        return mBleDevice;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBleDevice = getArguments().getParcelable(ARG_PARAM);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mBleWrapper.initialize();
        mBleWrapper.connect(mBleDevice.getAddress());
    }

    @Override
    public void onPause() {
        super.onPause();
        mBleWrapper.disconnect();
        mBleWrapper.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_device_detail, container, false);

        ButterKnife.bind(this, v);

        mBleWrapper = new BleWrapper(getActivity(), new BleWrapperUiCallbacks.Null() {
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
        });

        mListAdapter = new ExpandableListAdapter(getContext());
        mExpandableListView.setAdapter(mListAdapter);

        return v;
    }
}
