package com.hogent.jan.attblegateway;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.hogent.jan.attblegateway.ATTBLE.AttIoT;
import com.hogent.jan.attblegateway.ExpandableListView.ExpandableListAdapter;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleCharacteristic;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleNamesResolver;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleService;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleWrapper;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleWrapperUiCallbacks;
import com.hogent.jan.attblegateway.recyclerview.DeviceListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DeviceDetailFragment extends Fragment implements BleWrapperUiCallbacks, ExpandableListAdapter.ExpandableListAdapterListener, AttIoT.AttIoTStateChangedListener {
    private final String TAG = getClass().getSimpleName();

    private AttIoT att;

    private static final String BLE_NAME = "deviceName";
    private static final String BLE_ADDRESS = "deviceAddress";
    private static final String BLE_RSSI = "deviceRssi";

    private BleWrapper mBleWrapper = null;
    private ExpandableListAdapter mListAdapter;

    private String mDeviceName = "";
    private String mDeviceAddress = "";
    private int mDeviceRssi = 0;

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
        final List<BleService> bleServices = new ArrayList<>();
        for (BluetoothGattService s : services) {
            bleServices.add(new BleService(s));
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListAdapter.addServices(bleServices);
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
        final List<BleCharacteristic> characteristics = new ArrayList<>();
        for (BluetoothGattCharacteristic ch : chars) {
            characteristics.add(new BleCharacteristic(ch));
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListAdapter.addCharacteristicsForService(service, characteristics);
            }
        });

        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : chars) {
            int properties = bluetoothGattCharacteristic.getProperties();
            if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                att.addAsset(service.getUuid().toString().replace("-", "") + "_" + bluetoothGattCharacteristic.getUuid().toString().replace("-", ""),
                        BleNamesResolver.resolveCharacteristicName(bluetoothGattCharacteristic.getUuid().toString()),
                        "",
                        true,
                        BleNamesResolver.resolveCharacteristicType(bluetoothGattCharacteristic.getUuid().toString()));
            } else if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0
                    || (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                    || (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                att.addAsset(service.getUuid().toString().replace("-", "") + "_" + bluetoothGattCharacteristic.getUuid().toString().replace("-", ""),
                        BleNamesResolver.resolveCharacteristicName(bluetoothGattCharacteristic.getUuid().toString()),
                        "",
                        false,
                        BleNamesResolver.resolveCharacteristicType(bluetoothGattCharacteristic.getUuid().toString()));
            }
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

        String type = BleNamesResolver.resolveCharacteristicType(ch.getUuid().toString());

        if (type.equals("integer")) {
            att.publish(String.valueOf(intValue),
                    service.getUuid().toString().replace("-", "") + "_" + ch.getUuid().toString().replace("-", ""));
        } else if (type.equals("boolean")) {
            if (intValue != 0) {
                att.publish("true",
                        service.getUuid().toString().replace("-", "") + "_" + ch.getUuid().toString().replace("-", ""));
            } else {
                att.publish("false",
                        service.getUuid().toString().replace("-", "") + "_" + ch.getUuid().toString().replace("-", ""));
            }
        } else {
            att.publish(strValue,
                    service.getUuid().toString().replace("-", "") + "_" + ch.getUuid().toString().replace("-", ""));

        }

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
    public void requestCharacteristicValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic) {
        mBleWrapper.requestCharacteristicValue(service, characteristic);
    }

    @Override
    public void writeDataToCharacteristic(BluetoothGattService service, BluetoothGattCharacteristic characteristic, byte[] data, String originalValue) {
        mBleWrapper.writeDataToCharacteristic(characteristic, data);

        String type = BleNamesResolver.resolveCharacteristicType(characteristic.getUuid().toString());

        switch (type) {
            case "integer":
                att.publish(String.valueOf(originalValue),
                        service.getUuid().toString().replace("-", "") + "_" + characteristic.getUuid().toString().replace("-", ""));
                break;
            case "boolean":
                try {
                    if (Integer.parseInt(originalValue) != 0) {
                        att.publish("true",
                                service.getUuid().toString().replace("-", "") + "_" + characteristic.getUuid().toString().replace("-", ""));
                    } else {
                        att.publish("false",
                                service.getUuid().toString().replace("-", "") + "_" + characteristic.getUuid().toString().replace("-", ""));
                    }
                } catch (Exception ignored) {
                }
                try {
                    if (Boolean.parseBoolean(originalValue)) {
                        att.publish("true",
                                service.getUuid().toString().replace("-", "") + "_" + characteristic.getUuid().toString().replace("-", ""));
                    } else {
                        att.publish("false",
                                service.getUuid().toString().replace("-", "") + "_" + characteristic.getUuid().toString().replace("-", ""));
                    }
                } catch (Exception ignored) {
                }
                break;
            default:
                att.publish(originalValue,
                        service.getUuid().toString().replace("-", "") + "_" + characteristic.getUuid().toString().replace("-", ""));
                break;
        }
    }

    @Override
    public int getValueFormat(BluetoothGattCharacteristic characteristic) {
        return mBleWrapper.getValueFormat(characteristic);
    }

    @Override
    public void setNotificationForCharacteristic(BluetoothGattService service, BluetoothGattCharacteristic characteristic, boolean enabled) {
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

        att = new AttIoT();
        att.setAttIoTStateChangedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mBleWrapper == null) {
            mBleWrapper = new BleWrapper(getActivity(), this);
        }

        if (!mBleWrapper.initialize()) {
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

    @Override
    public void callback(String topic, String message) {
        String[] split = topic.split("/");
        String serviceChar = split[split.length - 2];

        String service = serviceChar.split("_")[0];
        String characteristic = serviceChar.split("_")[1];

        mListAdapter.callback(service, characteristic, message);
    }
}
