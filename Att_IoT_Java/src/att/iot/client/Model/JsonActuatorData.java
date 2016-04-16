package att.iot.client.Model;

import org.json.JSONObject;

/**
 * Contains the data that we found when an actuator value was sent from the cloud to a device.
 */
public class JsonActuatorData extends ActuatorData{
    private JSONObject value;

    /**
     * Loads the data.
     * @param value the raw value
     */
    @Override
    public void load(String value) {
        this.value = new JSONObject(value);
    }

    public JSONObject getValue() {
        return value;
    }

    /**
     * Returns @{@link String} that represents this instance
     * @return @{@link String} that represents this instance
     */
    @Override
    public String toString() {
        return value.toString();
    }
}
