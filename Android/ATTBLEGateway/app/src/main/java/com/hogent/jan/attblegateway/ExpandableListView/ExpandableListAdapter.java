package com.hogent.jan.attblegateway.ExpandableListView;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.hogent.jan.attblegateway.R;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleNamesResolver;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;

/**
 * Created by Jan on 28/02/2016.
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {
    private Context mContext;
    private List<BluetoothGattService> mBluetoothServices;
    private HashMap<BluetoothGattService, List<BluetoothGattCharacteristic>> mBluetoothCharacteristics;
    private BleWrapper mBleWrapper = null;

    public ExpandableListAdapter(Context context, BleWrapper wrapper) {
        mContext = context;
        mBluetoothCharacteristics = new HashMap<>();
        mBluetoothServices = new ArrayList<>();
        mBleWrapper = wrapper;
    }

    public void addServices(List<BluetoothGattService> services){
        for(BluetoothGattService service : services){
            if(!mBluetoothServices.contains(service)){
                mBluetoothServices.add(service);
            }
        }

        notifyDataSetChanged();
    }

    public void addCharacteristicsForService(BluetoothGattService service, List<BluetoothGattCharacteristic> characteristics){
        if(!mBluetoothCharacteristics.containsKey(service)){
            mBluetoothCharacteristics.put(service, characteristics);
        }
        for(BluetoothGattCharacteristic characteristic : characteristics){
            if(!mBluetoothCharacteristics.get(service).contains(characteristic)){
                mBluetoothCharacteristics.get(service).add(characteristic);
            }
        }

        notifyDataSetChanged();
    }

    public void newValueForCharacteristic(BluetoothGattService service, BluetoothGattCharacteristic characteristic, String strValue, int intValue, byte[] rawValue, String timestamp){
        List<BluetoothGattCharacteristic> characteristics = mBluetoothCharacteristics.get(service);
        if(characteristics != null && characteristics.contains(characteristic)){
            int serviceIndex = mBluetoothServices.indexOf(service);
            int characteristicIndex = characteristics.indexOf(characteristic);

            View detailView = getChildView(serviceIndex, characteristicIndex,false, null, null);

            EditText txtValue = ButterKnife.findById(detailView, R.id.characteristicDetailValue);
            TextView lblStringValue = ButterKnife.findById(detailView, R.id.characteristicDetailStringValue);
            TextView lblDecimalValue = ButterKnife.findById(detailView, R.id.characteristicDetailDecimalValue);
            TextView lblUpdated = ButterKnife.findById(detailView, R.id.characteristicDetailLastUpdated);

            lblStringValue.setText(strValue);
            if(rawValue != null && rawValue.length > 0){
                final StringBuilder builder = new StringBuilder(rawValue.length);
                for(byte byteChar: rawValue){
                    builder.append(String.format("%02X", byteChar));
                }
                txtValue.setText(builder.toString());
            }else{
                txtValue.setText("");
            }
            lblDecimalValue.setText(String.format("%d", intValue));
            lblUpdated.setText(timestamp);
        }
    }

    public void clearLists(){
        mBluetoothCharacteristics.clear();
        mBluetoothServices.clear();
        notifyDataSetChanged();
    }

    public byte[] parseHexStringToBytes(final String hex) {
        String tmp = hex.substring(2).replaceAll("[^[0-9][a-f]]", "");
        byte[] bytes = new byte[tmp.length() / 2]; // every two letters in the string are one byte finally

        String part = "";

        for(int i = 0; i < bytes.length; ++i) {
            part = "0x" + tmp.substring(i*2, i*2+2);
            bytes[i] = Long.decode(part).byteValue();
        }

        return bytes;
    }

    @Override
    public BluetoothGattCharacteristic getChild(int groupPosition, int childPosition){
        return mBluetoothCharacteristics.get(mBluetoothServices.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final BluetoothGattCharacteristic child = getChild(groupPosition, childPosition);
        final String characteristicName = BleNamesResolver.resolveUuid(child.getUuid().toString());
        final String characteristicUUID = child.getUuid().toString().toUpperCase();

        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_characteristic, null);
        }

        TextView lblCharacteristicName = (TextView) convertView.findViewById(R.id.deviceServiceCharacteristic);
        TextView lblCharacteristicUUID = (TextView) convertView.findViewById(R.id.deviceServiceCharacteristicUUID);
        lblCharacteristicName.setText(characteristicName);
        lblCharacteristicUUID.setText(characteristicUUID);

        final LinearLayout detailView = ButterKnife.findById(convertView, R.id.characteristicDetail);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detailView.getVisibility() == View.GONE) {
                    detailView.setVisibility(View.VISIBLE);

                    TextView lblDataType = ButterKnife.findById(detailView, R.id.characteristicDetailDataType);
                    TextView lblProperties = ButterKnife.findById(detailView, R.id.characteristicDetailProperties);
                    ToggleButton btnNotification = ButterKnife.findById(detailView, R.id.characteristicDetailNotificationToggle);
                    Button btnRead  = ButterKnife.findById(detailView, R.id.characteristicDetailRead);
                    Button btnWrite = ButterKnife.findById(detailView, R.id.characteristicDetailWrite);
                    final EditText txtValue = ButterKnife.findById(detailView, R.id.characteristicDetailValue);
                    TextView lblStringValue = ButterKnife.findById(detailView, R.id.characteristicDetailStringValue);
                    TextView lblDecimalValue = ButterKnife.findById(detailView, R.id.characteristicDetailDecimalValue);
                    TextView lblUpdated = ButterKnife.findById(detailView, R.id.characteristicDetailLastUpdated);

                    btnNotification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if(isChecked){
                                mBleWrapper.setNotificationForCharacteristic(child, true);
                            }else{
                                mBleWrapper.setNotificationForCharacteristic(child, false);
                            }
                        }
                    });

                    btnWrite.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String value = txtValue.getText().toString().toLowerCase(Locale.getDefault());
                            byte[] dataToWrite = parseHexStringToBytes(value);

                            mBleWrapper.writeDataToCharacteristic(child, dataToWrite);
                        }
                    });

                    btnRead.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mBleWrapper.requestCharacteristicValue(child);
                        }
                    });

                    int format = mBleWrapper.getValueFormat(child);
                    lblDataType.setText(BleNamesResolver.resolveValueTypeDescription(format));
                    int properties = child.getProperties();
                    String propertiesString = String.format("0x%04X [", properties);
                    if((properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0){
                        propertiesString += "read ";
                    }
                    if((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0){
                        propertiesString += "write ";
                    }
                    if((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0){
                        propertiesString += "notify ";
                    }
                    if((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0){
                        propertiesString += "indicate ";
                    }
                    if((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0){
                        propertiesString += "write_no_response ";
                    }
                    if(propertiesString.endsWith(" ")){
                        propertiesString = propertiesString.substring(0, propertiesString.length()-1);
                    }
                    lblProperties.setText(propertiesString + "]");

                    btnNotification.setEnabled((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0);
                    btnRead.setEnabled((properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
                    btnWrite.setEnabled((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0);
                    txtValue.setEnabled(btnWrite.isEnabled());

                    lblDecimalValue.setText("Read value");
                    lblStringValue.setText("Read value");
                    lblUpdated.setText("Read value");
                } else {
                    detailView.setVisibility(View.GONE);
                }
            }
        });

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mBluetoothCharacteristics.get(mBluetoothServices.get(groupPosition)).size();
    }

    @Override
    public BluetoothGattService getGroup(int groupPosition) {
        return mBluetoothServices.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return mBluetoothServices.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        final String serviceName = BleNamesResolver.resolveUuid(getGroup(groupPosition).getUuid().toString());
        final String serviceUUID = getGroup(groupPosition).getUuid().toString().toUpperCase();
        final String type = getGroup(groupPosition).getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY ? "Primary service" : "Secondary service";
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_service, null);
        }

        TextView lblServiceName = ButterKnife.findById(convertView, R.id.deviceService);
        TextView lblServiceUUID = ButterKnife.findById(convertView, R.id.deviceServiceUUID);
        TextView lblServiceType = ButterKnife.findById(convertView, R.id.deviceServiceType);
        lblServiceName.setText(serviceName);
        lblServiceUUID.setText(serviceUUID);
        lblServiceType.setText(type);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}