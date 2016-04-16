package att.iot.client;

import att.iot.client.Model.ActuatorData;
import att.iot.client.Model.AssetManagementCommandData;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Manages a single IOT device
 * @author Jan Van Braeckel
 * @since 26/03/2016
 * @version 0.2
 */
public interface DeviceUICallbacks {
    void onActuatorValue(IoTGateway caller, ActuatorData data);
    void onAssetManagementCommand(IoTGateway caller, AssetManagementCommandData data);
    void onDeviceManagementCommand(IoTGateway caller, String command);
    void onConnectionReset(IoTGateway caller);
}
