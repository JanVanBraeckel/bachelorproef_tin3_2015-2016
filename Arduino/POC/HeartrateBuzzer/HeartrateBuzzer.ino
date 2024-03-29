/*
 * Little project based on https://www.arduino.cc/en/Tutorial/Genuino101CurieBLEHeartRateMonitor
 * I've added a custom service to enable a buzzer when sending anything other than 0, and disabling it by sending 0
 */

#include <CurieBLE.h>

BLEPeripheral blePeripheral;       // BLE Peripheral Device (the board you're programming)
BLEService heartRateService("180D"); // BLE Heart Rate Service
BLEService buzzerService("19B10000-E8F2-537E-4F6C-D104768A1214"); // Custom service that has a characteristic to turn a buzzer on/off

// BLE Heart Rate Measurement Characteristic"
BLECharacteristic heartRateChar("2A37",  // standard 16-bit characteristic UUID
    BLERead | BLENotify, 2);  // remote clients will be able to get notifications if this characteristic changes
                              // the characteristic is 2 bytes long as the first field needs to be "Flags" as per BLE specifications
                              // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
BLEUnsignedCharCharacteristic buzzerChar("19B10001-E8F2-537E-4F6C-D104768A1214", BLERead | BLEWrite); // Custom characteristic to toggle buzzer. 0 for off, everything else for on

long previousMillis = 0;  // last time the heart rate was checked, in ms

void setup() {
  Serial.begin(9600);    // initialize serial communication
  pinMode(5, OUTPUT);    // initialize the buzzer on pin 5 as output
  
  /* Set a local name for the BLE device
     This name will appear in advertising packets
     and can be used by remote devices to identify this BLE device
     The name can be changed but maybe be truncated based on space left in advertisement packet */
  blePeripheral.setLocalName("HRBuzz");
  blePeripheral.setAdvertisedServiceUuid(heartRateService.uuid());  // add the service UUID
  blePeripheral.addAttribute(heartRateService);   // Add the BLE Heart Rate service
  blePeripheral.addAttribute(heartRateChar); // add the Heart Rate Measurement characteristic
  blePeripheral.setAdvertisedServiceUuid(buzzerService.uuid());
  blePeripheral.addAttribute(buzzerService);
  blePeripheral.addAttribute(buzzerChar);

  // Disable the buzzer by default
  buzzerChar.setValue(0);

  /* Now activate the BLE device.  It will start continuously transmitting BLE
     advertising packets and will be visible to remote BLE central devices
     until it receives a new connection */
  blePeripheral.begin();
  Serial.println("Bluetooth device active, waiting for connections...");
}

void loop() {
  // listen for BLE peripherals to connect:
  BLECentral central = blePeripheral.central();

  // if a central is connected to peripheral:
  if (central) {
    Serial.print("Connected to central: ");
    // print the central's MAC address:
    Serial.println(central.address());

    // check the heart rate measurement every 1 second
    // as long as the central is still connected:
    while (central.connected()) {
      long currentMillis = millis();
      // if 1 second has passed, check the heart rate measurement:
      if (currentMillis - previousMillis >= 1000) {
        previousMillis = currentMillis;
        updateHeartRate();
      }
      
      if(buzzerChar.written()){
        Serial.println("Written to " + buzzerChar.value());
        if(buzzerChar.value()){
          Serial.println("Buzzer on");
          digitalWrite(5, HIGH);
        } else {
          Serial.println("Buzzer off");
          digitalWrite(5, LOW);
        }
      }
    }
    
    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
  }
}

void updateHeartRate() {
  /* Read the current voltage level on the A0 analog input pin.
     This is used here to simulate the heart rate's measurement.
  */
  int heartRateMeasurement = analogRead(A0);
  int heartRate = map(heartRateMeasurement, 0, 1023, 0, 100);
  Serial.print("Heart Rate is now: "); // print it
  Serial.println(heartRate);
  const unsigned char heartRateCharArray[2] = { 0, (char)heartRate };
  heartRateChar.setValue(heartRateCharArray, 2);  // and update the heart rate measurement characteristic
}
