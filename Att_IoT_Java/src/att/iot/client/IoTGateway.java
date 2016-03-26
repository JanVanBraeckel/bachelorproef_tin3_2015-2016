package att.iot.client;

import att.iot.client.Model.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Jan on 26/03/2016.
 */
public class IoTGateway implements MqttCallback {
    private MqttClient mqtt;
    boolean httpError = false;
    private String deviceId;
    private String clientId;
    private String clientKey;
    private String gatewayId;
    private String brokerUri;
    private String apiUri;
    private MqttConnectOptions conOpt;
    private DeviceUICallbacks callbacks;

    public IoTGateway(String clientId, String clientKey, String apiUri, String brokerUri) {
        this.clientId = clientId;
        this.clientKey = clientKey;
        this.apiUri = apiUri == null ? "https://api.smartliving.io/" : apiUri;
        this.brokerUri = brokerUri == null ? "tcp://broker.smartliving.io:1883" : brokerUri;
    }

    private void init() {
        initMqtt();
        subscribeToTopics();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        if (this.deviceId == null) {
            this.deviceId = deviceId;
            subscribeToTopics();
        } else if (!this.deviceId.equals(deviceId)) {
            if (!(this.deviceId.isEmpty())) {
                unSubscribeToTopics();
            }
            this.deviceId = deviceId;
            if (!(this.deviceId != null && this.deviceId.isEmpty())) {
                subscribeToTopics();
            }
        }
    }

    public void setUICallbacks(DeviceUICallbacks callbacks) {
        this.callbacks = callbacks;
    }

    private void initMqtt() {
        try {
            // Construct the connection options object that contains connection parameters
            this.conOpt = new MqttConnectOptions();
            this.conOpt.setCleanSession(true);

            String tmpDir = System.getProperty("java.io.tmpdir");
            MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);

            String clientId = UUID.randomUUID().toString().substring(0, 22);
            String mqttUsername = this.clientId + ":" + this.clientId;

            conOpt.setPassword(clientKey.toCharArray());

            conOpt.setUserName(mqttUsername);

            // Construct an MQTT blocking mode client
            //client = new MqttClient(this.brokerUrl, att.getClientId(), dataStore);
            mqtt = new MqttClient(this.brokerUri, clientId, dataStore);

            /* for android, will require storage permission */
            //mqtt = new MqttClient(this.brokerUri, clientId, new MqttDefaultFilePersistence(Environment.getExternalStorageDirectory().getAbsolutePath()));

            // Set this wrapper as the callback handler
            mqtt.setCallback(this);

            connect();
        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    @Override
    public void connectionLost(Throwable throwable) {
        while (!mqtt.isConnected()) {
            try {
                connect();
            } catch (MqttException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        connectionReset();
    }

    private void connectionReset() {
        if (callbacks != null) {
            callbacks.onConnectionReset(this);
        }
        subscribeToTopics();
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        try {
            String[] parts = s.split("/");
            TopicPath path = new TopicPath(parts);

            System.out.println("Message arrived on topic " + s + " with message " + mqttMessage.toString());

            if (!path.isSetter() && path.getAssetId() != null) {
                onActuatorValue(path, mqttMessage);
            } else {
                onManagementCommand(path, mqttMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onManagementCommand(TopicPath path, MqttMessage message) {
        if (path.getAssetId() != null) {
            if (callbacks != null) {
                AssetManagementCommandData data = new AssetManagementCommandData();
                data.setAsset(path.getAssetId()[0]);
                data.setCommand(message.toString());
                //assetManagementCommand(this, data);
                callbacks.onAssetManagementCommand(this, data);
            }
        } else if (callbacks != null) {
            String command = message.toString();
            callbacks.onDeviceManagementCommand(this, command);
        }
    }

    private void onActuatorValue(TopicPath path, MqttMessage message) {
        if (callbacks != null) {
            String val = message.toString();
            ActuatorData data = null;
            if (val.charAt(0) == '{') {
                data = new JsonActuatorData();
            } else {
                data = new StringActuatorData();
            }
            data.load(val);
            data.setAsset(path.getAssetId()[0]);
            callbacks.onActuatorValue(this, data);
        }
    }

    private String getTopicPath(String assetId) {
        return String.format("client/%s/out/device/%s/asset/%s/state", clientId, deviceId, assetId);
    }

    public void send(String asset, Object value) {
        String toSend = prepareValueForSending(value);

        MqttTopic topic = mqtt.getTopic(getTopicPath(asset));
        try {
            topic.publish(new MqttMessage(toSend.getBytes()));
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private String prepareValueForSending(Object value) {
        String toSend = null;
        if (value instanceof String) {
            toSend = "0|" + value.toString();
        } else if (value instanceof JSONObject) {
            try {
                JSONObject result = new JSONObject();
                SimpleDateFormat sdf = new SimpleDateFormat();
                sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
                result.put("at", sdf.parse(new Date().toString()));
                result.put("value", value);
                toSend = result.toString();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            throw new UnsupportedOperationException("Value is of a none supported type");
        }

        return toSend;
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    private void connect() throws MqttException {
        mqtt.connect(conOpt);
        System.out.println("Connected to " + brokerUri + " with client ID " + clientId);
    }

    private void subscribeToTopics() {
        if (mqtt != null) {

            Thread t = new Thread() {
                public void run() {
                    try {
                        String[] toSubscribe = getTopics();
                        int[] qos = new int[toSubscribe.length];
                        for (int i = 0; i < toSubscribe.length; i++) {
                            qos[i] = 0; // at most once?
                        }
                        mqtt.subscribe(toSubscribe, qos);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            };

            t.start();
        }
    }

    private void unSubscribeToTopics() {
        if (mqtt != null) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        String[] toRemove = getTopics();
                        mqtt.unsubscribe(toRemove);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            };

            t.start();
        }
    }

    private String[] getTopics() {
        String[] topics = new String[1];
        String root = String.format("client/%s/in/gateway/%s/#/command", clientId, gatewayId);
        topics[0] = root;
        return topics;
    }

    private String getRemoteAssetId(String assetId) {
        return String.format("%s_%s", deviceId, assetId);
    }

    public void addAsset(String deviceId, String asset, JSONObject content, HashMap<String, String> extraHeaders) {
        this.deviceId = deviceId;
        try {
            String uri = apiUri + "/device/" + deviceId + "/asset/" + asset;

            URL obj = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // Add request header
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Auth-GatewayKey", clientKey);
            con.setRequestProperty("Auth-GatewayId", gatewayId);
            prepareRequestForAuth(con);

            if (extraHeaders != null) {
                for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                    con.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // Create request body
            StringBuffer sb = new StringBuffer();
            sb.append(content.toString());

            String urlParameters = sb.toString();

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'PUT' request to URL : " + uri);
            System.out.println("Parameters : " + urlParameters);
            System.out.println("Response Code : " + responseCode);

            httpError = false;
        } catch (IOException e) {
            e.printStackTrace();
            httpError = true;
        }
    }

    public boolean addAsset(String deviceId, String assetId, String name, String description, boolean isActuator, String type) {
        this.deviceId = deviceId;
        try {
            String content;

            if (type == null || type.isEmpty()) {
                content = String.format("{ \"is\" : \"%s\", \"name\" : \"%s\", \"description\" : \"%s\" }", isActuator ? "actuator" : "sensor", name, description);
            } else if (type.startsWith("{")) {
                content = String.format("{ \"is\" : \"%s\", \"name\" : \"%s\", \"description\" : \"%s\", \"profile\": %s }", isActuator ? "actuator" : "sensor", name, description, type);
            } else {
                content = String.format("{ \"is\" : \"%s\", \"name\" : \"%s\", \"description\" : \"%s\", \"profile\" : { \"type\" : \"%s\" }}", isActuator ? "actuator" : "sensor", name, description, type);
            }

            String uri = apiUri + "/device/" + deviceId + "/asset/" + assetId;
            URL obj = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // Add request header
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Auth-GatewayKey", clientKey);
            con.setRequestProperty("Auth-GatewayId", gatewayId);
            prepareRequestForAuth(con);

            String urlParameters = content;

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'PUT' request to URL : " + uri);
            System.out.println("Parameters : " + urlParameters);
            System.out.println("Response Code : " + responseCode);

            httpError = false;

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            httpError = true;
            return false;
        }
    }

    private void prepareRequestForAuth(HttpsURLConnection con) {
        con.setRequestProperty("Auth-ClientKey", clientKey);
        con.setRequestProperty("Auth-ClientId", clientId);
    }

    public JSONObject getPrimaryAsset() {
        try {
            StringBuilder response = new StringBuilder();

            String uri = apiUri + "/device/" + deviceId + "/assets?style=primary";

            URL obj = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // Add request header
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            prepareRequestForAuth(con);

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'PUT' request to URL : " + uri);
            System.out.println("Parameters : none");
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            httpError = false;

            return new JSONObject(response.toString());
        } catch (Exception e) {
            httpError = true;
            e.printStackTrace();
        }

        return null;
    }

    public void sendAssetValueHTTP(String asset, Object value) {
        String toSend = prepareValueForSendingHTTP(value);
        try {
            String uri = apiUri + "/device/" + deviceId + "/asset/" + asset + "/state";

            URL obj = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // Add request header
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            prepareRequestForAuth(con);

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(toSend);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'PUT' request to URL : " + uri);
            System.out.println("Parameters : " + toSend);
            System.out.println("Response Code : " + responseCode);

            httpError = false;

        } catch (Exception e) {
            e.printStackTrace();
            httpError = true;
        }
    }

    public void sendCommandto(String asset, Object value) {
        String toSend = value.toString();
        try {
            String uri = apiUri + "/asset/" + asset + "/command";

            URL obj = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // Add request header
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            prepareRequestForAuth(con);

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(toSend);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'PUT' request to URL : " + uri);
            System.out.println("Parameters : " + toSend);
            System.out.println("Response Code : " + responseCode);

            httpError = false;
        } catch (Exception e) {
            e.printStackTrace();
            httpError = true;
        }
    }

    private JSONObject getAssetState(String asset) {
        try {
            StringBuilder response = new StringBuilder();
            String uri = apiUri + "/device/" + deviceId + "/asset/" + asset + "/state";

            URL obj = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // Add request header
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            prepareRequestForAuth(con);

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'PUT' request to URL : " + uri);
            System.out.println("Parameters : none");
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            httpError = false;

            return new JSONObject(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            httpError = false;
        }

        return new JSONObject();
    }

    public JSONObject getAssets() {
        try {
            StringBuilder response = new StringBuilder();
            String uri = apiUri + "/device/" + deviceId + "/assets";

            URL obj = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // Add request header
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            prepareRequestForAuth(con);

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'PUT' request to URL : " + uri);
            System.out.println("Parameters : none");
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            httpError = false;

            return new JSONObject(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            httpError = true;
        }
        return new JSONObject();
    }

    private String prepareValueForSendingHTTP(Object value) {
        try {
            JSONObject result = new JSONObject();

            SimpleDateFormat sdf = new SimpleDateFormat();
            sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
            result.put("at", sdf.parse(new Date().toString()));

            if (value instanceof JSONObject)
                result.put("value", value);
            else {
                JSONObject conv;
                conv = new JSONObject(value);
                result.put("value", conv);
            }
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public void addDevice(String deviceId, String name, String description, boolean activateActivity) {
        try {
            String uri = apiUri + "/device/" + deviceId;

            URL obj = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // Add request header
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Auth-GatewayKey", clientKey);
            con.setRequestProperty("Auth-GatewayId", gatewayId);
            prepareRequestForAuth(con);

            JSONObject toSend = new JSONObject();
            toSend.put("title", name);
            toSend.put("description", description);
            toSend.put("type", "custom");
            toSend.put("activityEnabled", activateActivity);

            // Send put request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(toSend.toString());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'PUT' request to URL : " + uri);
            System.out.println("Parameters : " + toSend);
            System.out.println("Response Code : " + responseCode);

            httpError = false;
        } catch (Exception e) {
            e.printStackTrace();
            httpError = true;
        }
    }

    public boolean deviceExists(String deviceId) {
        try {
            String uri = apiUri + "/device/" + deviceId;

            URL obj = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // Add request header
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Auth-GatewayKey", clientKey);
            con.setRequestProperty("Auth-GatewayId", gatewayId);
            prepareRequestForAuth(con);

            // Send get request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'GET' request to URL : " + uri);
            System.out.println("Parameters : none");
            System.out.println("Response Code : " + responseCode);

            httpError = false;

            if(responseCode != 200){
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            httpError = true;
        }

        return false;
    }

    public void subscribe() {
        init();
    }

    public void createGateway(String name, String uid) {
        try {
            String uri = apiUri + "/gateway";

            URL obj = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // Add request header
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            prepareRequestForAuth(con);

            JSONObject toSend = new JSONObject();
            toSend.put("uid", uid);
            toSend.put("name", name);
            toSend.put("assets", new JSONArray());

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(toSend.toString());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'POST' request to URL : " + uri);
            System.out.println("Parameters : " + toSend);
            System.out.println("Response Code : " + responseCode);

            httpError = false;
        } catch (Exception e) {
            e.printStackTrace();
            httpError = true;
        }
    }

    public boolean finishClaim(String name, String uid) {
        try {
            StringBuilder response = new StringBuilder();
            String uri = apiUri + "/gateway";

            URL obj = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // Add request header
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            prepareRequestForAuth(con);

            JSONObject toSend = new JSONObject();
            toSend.put("uid", uid);
            toSend.put("name", name);
            toSend.put("assets", new JSONArray());

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(toSend.toString());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'POST' request to URL : " + uri);
            System.out.println("Parameters : " + toSend);
            System.out.println("Response Code : " + responseCode);

            httpError = false;

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String responseString = response.toString();

            JSONObject res =  new JSONObject();

            if(!responseString.isEmpty()){
                res = new JSONObject(response.toString());
                gatewayId = res.getString("id");
            }
            System.out.println(res.toString());

            if (responseCode == 200) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            httpError = true;
        }

        return false;
    }

    public boolean authenticate() {
        return false;
    }
}
