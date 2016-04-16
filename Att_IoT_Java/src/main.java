import att.iot.client.DeviceUICallbacks;
import att.iot.client.IoTGateway;
import att.iot.client.Model.ActuatorData;
import att.iot.client.Model.AssetManagementCommandData;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jan on 14/03/2016.
 */
public class main implements DeviceUICallbacks {
    private List<String> devices =  new ArrayList<>();
    private IoTGateway gateway;

    public static void main(String[] args) {
        new main().run();
    }

    public void run() {
        gateway = new IoTGateway("JanVanBraeckel", "5kcjpk1vcev", null, null);
        gateway.setUICallbacks(this);

        connect();

        String deviceId = "DF9208D3C695";
        while (true) {
            if(!devices.contains(deviceId)){
                devices.add(deviceId);
                if(!this.gateway.deviceExists(deviceId)){
                    this.gateway.addDevice(deviceId, "name of the device", "description of the device", true);
                    break;
                }else{
                    break;
                }
            }
        }

        gateway.addAsset(deviceId, "mlksjdflmkqsdjf_mlkqsdjflmkqsdjf","test asset", "test", true, "boolean");

        gateway.send(deviceId, "mlksjdflmkqsdjf_mlkqsdjflmkqsdjf", "true");

        while(true){

        }
    }

    private void connect(){
        if(authenticate()){
            gateway.subscribe();
        }
    }

    private boolean authenticate(){
        if(!tryLoadConfig()){
            String uid = getUid();
            gateway.createGateway("generic gateway", uid);
            while(true){
                if(gateway.finishClaim("generic gateway", uid)){
                    //store config
                    return true;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }else{
            return gateway.authenticate();
        }
    }

    private String getUid(){
        String address = "";
        while(address.equals("")){
            try {
                InetAddress ip = InetAddress.getLocalHost();
                NetworkInterface network = NetworkInterface.getByInetAddress(ip);
                byte[] mac  = network.getHardwareAddress();

                StringBuilder builder = new StringBuilder();
                for(int i = 0; i < mac.length; i++){
                    builder.append(String.format("%02X", mac[i]));
                }

                address = builder.toString();
                System.out.println("Mac address: " + address);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        return address;
    }

    private boolean tryLoadConfig(){
        // load config
        return false;
    }

    @Override
    public void onActuatorValue(IoTGateway caller, ActuatorData data) {
        System.out.println("main.onActuatorValue");
        System.out.println("caller = [" + caller + "], data = [" + data + "], asset = [" + data.getAsset() + "]");
    }

    @Override
    public void onAssetManagementCommand(IoTGateway caller, AssetManagementCommandData data) {
        System.out.println("main.onAssetManagementCommand");
        System.out.println("caller = [" + caller + "], data = [" + data + "], asset = [" + data.getAsset() + "]");
    }

    @Override
    public void onDeviceManagementCommand(IoTGateway caller, String command) {
        System.out.println("main.onDeviceManagementCommand");
        System.out.println("caller = [" + caller + "], command = [" + command + "]");
    }

    @Override
    public void onConnectionReset(IoTGateway caller) {
        System.out.println("main.onConnectionReset");
        System.out.println("caller = [" + caller + "]");
    }
}
