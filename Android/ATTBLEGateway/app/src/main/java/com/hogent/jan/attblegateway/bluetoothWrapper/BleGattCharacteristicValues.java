package com.hogent.jan.attblegateway.bluetoothWrapper;

/**
 * Created by Jan on 1/03/2016.
 */
public class BleGattCharacteristicValues {
    private String strValue = "", timestamp = "", asciiValue = "";
    private int intValue = 0;

    public BleGattCharacteristicValues(){}

    public BleGattCharacteristicValues(String strValue, String timestamp, int intValue, String asciiValue) {
        this.strValue = strValue;
        this.timestamp = timestamp;
        this.intValue = intValue;
        this.asciiValue = asciiValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public void setAsciiValue(String asciiValue) {
        this.asciiValue = asciiValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAsciiValue() {
        return asciiValue;
    }

    public int getIntValue() {
        return intValue;
    }

    public String getStrValue() {
        return strValue;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setValues(String strValue, int intValue, String asciiValue, String timestamp) {
        this.strValue = strValue;
        this.intValue = intValue;
        this.asciiValue = asciiValue;
        this.timestamp = timestamp;
    }
}
