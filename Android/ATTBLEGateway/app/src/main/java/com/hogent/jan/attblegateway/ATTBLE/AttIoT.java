package com.hogent.jan.attblegateway.ATTBLE;

/****
 * AllThingsTalk Java client
 *
 * @author Sander
 *         <p/>
 *         v0.1
 *         March 14th 2016
 ****/

public class AttIoT {

    // Enter your device credentials here
    private static final String clientId = "JanVanBraeckel";
    private static final String clientKey = "5kcjpk1vcev";
    private static final String deviceId = "LuytgMoeqp6NZ7QN4EG5Qu8";

    private AttIoTStateChangedListener listener;
    private HttpHandler http;
    private MqttHandler mqtt;

    public AttIoT() {
        this.http = new HttpHandler(this);
        this.mqtt = new MqttHandler(this);
    }

    // Getters
    public String getClientId() {
        return clientId;
    }

    public String getClientKey() {
        return clientKey;
    }

    public String getDeviceId() {
        return deviceId;
    }

    /****
     * Mqtt receive message from device assets
     *
     * @param topic
     * @param message
     ****/
    public void callback(String topic, String message) {
        // Extend this callback for personal use
        System.out.println(topic + ":" + message);
        listener.callback(topic, message);
    }

    /****
     * Mqtt publish a message
     *
     * @param message
     * @param assetId
     ****/
    public void publish(String message, String assetId) {
        mqtt.publishAMessage(message, assetId);
    }

    /****
     * Add asset
     *
     * @param id
     * @param name
     * @param description
     * @param isActuator
     * @param assetType
     * @return
     ****/
    public String addAsset(String id, String name, String description, boolean isActuator, String assetType) {
        return http.addAsset(id, name, description, isActuator, assetType);
    }

    public void setAttIoTStateChangedListener(AttIoTStateChangedListener listener) {
        this.listener = listener;
    }

    public interface AttIoTStateChangedListener {
        void callback(String topic, String message);
    }
}
