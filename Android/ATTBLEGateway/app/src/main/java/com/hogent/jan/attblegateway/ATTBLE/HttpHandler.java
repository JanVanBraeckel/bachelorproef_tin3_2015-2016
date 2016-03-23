package com.hogent.jan.attblegateway.ATTBLE;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class HttpHandler {

    private static final String baseUrl = "https://api.smartliving.io/";

    private String clientId;
    private String clientKey;
    private String deviceId;

    public HttpHandler(AttIoT att)
    {
        this.deviceId  = att.getDeviceId();
        this.clientId  = att.getClientId();
        this.clientKey = att.getClientKey();
    }

    /****
     * Add asset
     * @param id
     * @param name
     * @param description
     * @param isActuator
     * @param assetType
     * @return
     ****/
    public String addAsset(String id, String name, String description, boolean isActuator, String assetType)
    {
        System.out.println("\n* Add asset");
        StringBuffer response = new StringBuffer();

        try
        {
            String url = baseUrl + "device/" + deviceId + "/asset/"  + id;
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // Add request header
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Auth-ClientKey", clientKey);
            con.setRequestProperty("Auth-ClientId", clientId);

            // Create request body
            StringBuffer sb = new StringBuffer();
            sb.append("{\"name\":\"");
            sb.append(name);
            sb.append("\",\"description\":\"");
            sb.append(description);
            sb.append("\",\"is\":\"");
            sb.append(isActuator == true ? "actuator" : "sensor");

            if(assetType == null)
                sb.append("\"}");   // asset type undefined
            else if(assetType.charAt(0) == '{')
                sb.append("\",\"profile\":\"" + assetType + "\"}\"");   // primitive asset type
            else
                sb.append("\",\"profile\":{\"type\":\"" + assetType + "\"}}");   // complex asset type

            String urlParameters = sb.toString();

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Sending 'PUT' request to URL : " + url);
            System.out.println("Parameters : " + urlParameters);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if(response.length() > 0)
            return response.toString();

        return null;
    }
}
