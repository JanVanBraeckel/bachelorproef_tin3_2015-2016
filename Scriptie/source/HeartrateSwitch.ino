/*
   This library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2.1 of the License, or (at your option) any later version.

   This library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public
   License along with this library; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

/*
 * Little project based on https://www.arduino.cc/en/Tutorial/Genuino101CurieBLEHeartRateMonitor
 * I've added a custom service to enable a switch when sending anything other than 0, and disabling it by sending 0
 */

#include <CurieBLE.h>

// BLE Peripheral Device (the board you're programming)
BLEPeripheral blePeripheral;

// BLE Heart Rate Service
BLEService heartRateService("180D");

// Custom service that has a characteristic to turn a switch on/off
BLEService switchService("19B10000-E8F2-537E-4F6C-D104768A1214");

// BLE Heart Rate Measurement Characteristic"
// "2A37" - standard 16-bit characteristic UUID
// remote clients will be able to get notifications if this characteristic changes
// the characteristic is 2 bytes long as the first field needs to be "Flags" as per BLE specifications
// https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
BLECharacteristic heartRateChar("2A37", BLERead | BLENotify, 2);

// Custom characteristic to toggle a generic device with 2 inputs. 0 for off, everything else for on
BLEUnsignedCharCharacteristic switchChar("19B10001-E8F2-537E-4F6C-D104768A1214", BLERead | BLEWrite);

// last time the heart rate was checked, in ms
long previousMillis = 0;

void setup() {
  // initialize serial communication
  Serial.begin(9600);

  // initialize the switch on pin 5 as output
  pinMode(5, OUTPUT);
  
  /* Set a local name for the BLE device
     This name will appear in advertising packets
     and can be used by remote devices to identify this BLE device
     The name can be changed but maybe be truncated based on space left in advertisement packet */
  blePeripheral.setLocalName("HRSwitch");
  blePeripheral.setAdvertisedServiceUuid(heartRateService.uuid());  // add the service UUID
  blePeripheral.addAttribute(heartRateService);   // Add the BLE Heart Rate service
  blePeripheral.addAttribute(heartRateChar); // add the Heart Rate Measurement characteristic
  blePeripheral.setAdvertisedServiceUuid(switchService.uuid());
  blePeripheral.addAttribute(switchService);
  blePeripheral.addAttribute(switchChar);

  // Disable the switch by default
  switchChar.setValue(0);

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
      
      if(switchChar.written()){
        Serial.println("Written to " + switchChar.value());
        if(switchChar.value()){
          Serial.println("Switch on");
          digitalWrite(5, HIGH);
        } else {
          Serial.println("Switch off");
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
     This is used here to simulate the heart rate's measurement. */
  int heartRateMeasurement = analogRead(A0);
  int heartRate = map(heartRateMeasurement, 0, 1023, 0, 100);
  Serial.print("Heart Rate is now: "); // print it
  Serial.println(heartRate);
  const unsigned char heartRateCharArray[2] = { 0, (char)heartRate };
  heartRateChar.setValue(heartRateCharArray, 2);  // and update the heart rate measurement characteristic
}
