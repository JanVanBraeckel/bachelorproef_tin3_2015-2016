package com.hogent.jan.attblegateway.ExpandableListView;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.hogent.jan.attblegateway.R;
import com.hogent.jan.attblegateway.bluetoothWrapper.BleNamesResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;

/**
 * Created by Jan on 28/02/2016.
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {
    private Context mContext;
    private List<BluetoothGattService> mBluetoothServices;
    private HashMap<BluetoothGattService, List<BluetoothGattCharacteristic>> mBluetoothCharacteristics;

    public ExpandableListAdapter(Context context) {
        mContext = context;
        mBluetoothCharacteristics = new HashMap<>();
        mBluetoothServices = new ArrayList<>();
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

    @Override
    public BluetoothGattCharacteristic getChild(int groupPosition, int childPosition){
        return mBluetoothCharacteristics.get(mBluetoothServices.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final String characteristicName = BleNamesResolver.resolveUuid(getChild(groupPosition, childPosition).getUuid().toString());
        final String characteristicUUID = getChild(groupPosition, childPosition).getUuid().toString().toUpperCase();

        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_characteristic, null);
        }

        TextView lblCharacteristicName = (TextView) convertView.findViewById(R.id.deviceServiceCharacteristic);
        TextView lblCharacteristicUUID = (TextView) convertView.findViewById(R.id.deviceServiceCharacteristicUUID);
        lblCharacteristicName.setText(characteristicName);
        lblCharacteristicUUID.setText(characteristicUUID);

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
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_service, null);
        }

        TextView lblServiceName = ButterKnife.findById(convertView, R.id.deviceService);
        TextView lblServiceUUID = ButterKnife.findById(convertView, R.id.deviceServiceUUID);
        lblServiceName.setTypeface(null, Typeface.BOLD);
        lblServiceName.setText(serviceName);
        lblServiceUUID.setText(serviceUUID);

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