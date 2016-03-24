package com.hogent.jan.attblegateway.ATTBLE;

import android.os.Environment;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.IOException;

@SuppressWarnings("unused")
public class MqttHandler implements MqttCallback {

    private static int qos = 0; // 0, 1 or 2 quality of service

    private MqttClient client;
    private String brokerUrl;
    private MqttConnectOptions conOpt;

    private AttIoT att;

    private String username, password;

    /****
     * MQTT constructor
     ****/
    public MqttHandler(AttIoT att) {
        this.att = att;
        this.username = att.getClientId() + ":" + att.getClientId();
        this.password = att.getClientKey();
        this.brokerUrl = Broker.getBrokerUrl();

        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
        //MemoryPersistence dataStore = new MemoryPersistence();

        try {
            // Construct the connection options object that contains connection parameters
            this.conOpt = new MqttConnectOptions();
            this.conOpt.setCleanSession(true);

            if (password != null) {
                conOpt.setPassword(password.toCharArray());
            }
            if (username != null) {
                conOpt.setUserName(username);
            }

            // Construct an MQTT blocking mode client
            //client = new MqttClient(this.brokerUrl, att.getClientId(), dataStore);
            client = new MqttClient(this.brokerUrl, att.getClientId(), new MqttDefaultFilePersistence(Environment.getExternalStorageDirectory().getAbsolutePath()));

            // Set this wrapper as the callback handler
            client.setCallback(this);

            //
            connect();

            // Subscribe to all device assets
            final String topic = "client/" + att.getClientId() + "/in/device/" + att.getDeviceId() + "/asset/+/command";

            Thread t = new Thread() {
                public void run() {
                    try {
                        subscribe(topic);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            };

            t.start();

        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /****
     * Publish
     *
     * @param assetId   to publish to
     * @param pubMsg to publish
     ****/
    public void publishAMessage(String pubMsg, String assetId) {
        String topic = "client/" + att.getClientId() + "/out/device/" + att.getDeviceId() + "/asset/" + assetId + "/state";
        System.out.println(topic);

        MqttDeliveryToken token;
        MqttTopic mqttTopic = client.getTopic(topic);

        pubMsg = "0|" + pubMsg;

        try {
            // Publish to the broker
            token = mqttTopic.publish(new MqttMessage(pubMsg.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /****
     * Subscribe to a single topic
     *
     * @param topicName to subscribe to
     ****/
    private void subscribe(String topicName) throws MqttException {
        // subscribing to topic
        System.out.println("Subscribing to topic \"" + topicName + "\"");
        client.subscribe(topicName, 0);

        // Continue waiting for messages until the Enter is pressed
        //logln("Press <Enter> to exit");
        try {
            System.in.read();
        } catch (IOException e) {
            //If we can't read we'll just exit
        }
    }

    /****
     * callback function
     *
     * @param topic    on which we received the message
     * @param message message
     ****/
    public void messageArrived(String topic, MqttMessage message) {
        att.callback(topic, message.toString());
    }


    /****
     * Connect / disconnect
     ****/
    public void connect() throws MqttException {
        client.connect(conOpt);
        System.out.println("Connected to " + brokerUrl + " with client ID " + client.getClientId());
    }

    public void disconnect() throws MqttException {
        client.disconnect();
        System.out.println("Disconnected");
    }

    /****
     * Generic methods to implement the MqttCallback interface
     ****/
    public void connectionLost(Throwable cause) {
        System.out.println("Connection to " + brokerUrl + " lost!" + cause);
        System.exit(1);
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}
