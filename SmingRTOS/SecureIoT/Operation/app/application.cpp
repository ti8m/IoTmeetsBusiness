/**
 *  OPERATION-MODE >> rom0.bin
 */

#include <user_config.h>
#include <SmingCore.h>

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "espressif/smartconfig.h"
#include <espressif/esp_system.h>

#include <Libraries/DHT/DHT.h>
#include <Led.h>

#define SWITCH_PIN 4 // D2 >> GPIO4
#define SENSOR_PIN 14 // D5 >> GPIO14

#define MQTT_HOST "m21.cloudmqtt.com"
#define MQTT_PORT 18188
#define MQTT_USERNAME "secureIoT"
#define MQTT_PWD "admin"

DHT sensor(SENSOR_PIN, DHT22); // DHT22

Timer debounceTimer;
bool debounceActive = false;

Timer sleepTimer;
Timer disconnectTimer;
Timer messageTimer;

BssList networks;
Timer connectionTimer;

HttpClient thingSpeak;
Timer sendDataTimer;

bool successSent;

String mac;
String mqttMessage;
Timer procTimer;

// Forward declarations
void onMessageReceived(String topic, String message);
void startMqttClient();


void sleeping() {
	//System.deepSleep(30000, eDSO_RF_CAL_ALWAYS);
	System.deepSleep(30000);
}


// MQTT client
MqttClient mqtt(MQTT_HOST, MQTT_PORT, onMessageReceived);

bool isMqttClientConnected() {
	return mqtt.getConnectionState() == eTCS_Connected;
}

/**
 *  Publish MQTT message
 */
void publishMessage() {

	if (isMqttClientConnected()) {

		messageTimer.stop();

	} else {

		// reconnect
		startMqttClient();
	}

	Serial.println("------ publish message ------");
	mqtt.publish("secureIoT/" + mac + "/state", mqttMessage); // or publishWithQoS
}

/**
 *  Check for MQTT Disconnection
 */
void checkMQTTDisconnect(TcpClient& client, bool flag) {

	// Called whenever MQTT connection is failed.
	if (flag == true)
		Serial.println("MQTT Broker Disconnected!!");
	else
		Serial.println("MQTT Broker Unreachable!!");

	// Restart connection attempt after few seconds
	procTimer.initializeMs(2 * 1000, startMqttClient).start(); // every 2 seconds
}

/**
 *  Run MQTT client
 */
void startMqttClient() {
	procTimer.stop();
	String device = WifiStation.getMAC();

	// If connection is lost, the broker will send the message LOST_CONNECTION to topic "secureIoT/MAC/state"
	if (!mqtt.setWill("secureIoT" + device + "/state", "LOST_CONNECTION", 1, true)) {
		debugf("Unable to set the last will and testament. Most probably there is not enough memory on the device.");
	}
	mqtt.connect(device, MQTT_USERNAME, MQTT_PWD);
	// Assign a disconnect callback function
	mqtt.setCompleteDelegate(checkMQTTDisconnect);
	mqtt.subscribe("secureIoT/" + device + "/command");

	//mqtt.commandProcessing(true,"command","cmdreply");

}

/**
 *  Callback for messages, arrived from MQTT broker
 */
void onMessageReceived(String topic, String message) {

	Serial.println("Message received: " + message);

	if (message.equals("DONE")) {



	}
}



/**
 *  WiFi station connected to AP
 */
void connectOk() {

	debugf("I'm CONNECTED");
	Serial.println(WifiStation.getIP().toString());
	blinkStop();

	// set mac-address
	mac = WifiStation.getMAC();

	// run MQTT client
	startMqttClient();

	// start sensor reading
	sensor.begin();

	// start sending data
	sendDataTimer.startOnce();
}


/**
 *  Connection timeout reached
 */
void connectFail() {

	debugf("CAN NOT CONNECT");

	blinkStop();
	digitalWrite(RED_LED_PIN, true);

	WifiStation.disconnect();
	sleepTimer.startOnce();

}

/**
 *  ThingSpeak has sent data
 */
void onDataSent(HttpClient& client, bool successful) {
	if (successSent)
		return;

	Serial.println("ON DATA SENT: ");

	if (successful) {
		successSent = true;
		blinkGreenStart(200, 2);
		Serial.println("Success sent");
	} else if (!successSent) {
		blinkRedStart(200, 4);
		Serial.println("Failed");

	}

	String response = client.getResponseString();
	Serial.println("Server response: '" + response + "'");
	if (response.length() > 0) {
		int intVal = response.toInt();

		if (intVal == 0)
			Serial.println("Sensor value wasn't accepted");
	}

//	WifiStation.disconnect();
	sleepTimer.startOnce();
}


/**
 *  Read Data from Sensor and sent them to ThingSpeak
 */
void sendData() {
	if (thingSpeak.isProcessing())
		return; // We need to wait while request processing was completed

	TempAndHumidity th;
	if (sensor.readTempAndHumidity(th)) {
		Serial.print("\tHumidity: ");
		Serial.println(th.humid);
		Serial.print("% Temperature: ");
		Serial.println(th.temp);

		thingSpeak.downloadString("http://54.164.214.198/update?key=SHWNHCPXP78N4NIR&field1=" + String(th.temp) + "&field2=" + String(th.humid), onDataSent);
		//thingSpeak.downloadString("http://54.164.214.198/update?key=" + AppSettings.channelKey + "&field1=" + String(th.temp) + "&field2=" + String(th.humid), onDataSent);

	} else {
		Serial.print("Failed to read from DHT: ");
		Serial.print(sensor.getLastError());
	}

}



// for test
void serialCallBack(Stream& stream, char arrivedChar, unsigned short availableCharsCount) {

	if (arrivedChar == 's') {

		// Reboot in operation mode
		rboot_set_current_rom(0);
		Serial.println("Restarting...");
		System.restart();

	}
}


void init() {

	Serial.begin(SERIAL_BAUD_RATE); // 115200 by default
	Serial.systemDebugOutput(true); // Debug output to serial

	int slot = rboot_get_current_rom();
	Serial.printf("\r\nCurrently running rom %d.\r\n", slot);

	pinMode(GREEN_LED_PIN, OUTPUT);
	pinMode(RED_LED_PIN, OUTPUT);
	pinMode(SWITCH_PIN, INPUT);

	WifiStation.enable(true);
	WifiStation.enableDHCP(true);

	WifiAccessPoint.enable(false);

	sendDataTimer.initializeMs(1000, sendData);
	sleepTimer.initializeMs(5000, sleeping);
	messageTimer.initializeMs(2000, publishMessage);

	successSent = false;

	if (digitalRead(SWITCH_PIN)) {

		WifiStation.disconnect();

		// Reboot in config-mode
		rboot_set_current_rom(1);
		Serial.println("Restarting...");
		System.restart();

	} else {

		WifiStation.waitConnection(connectOk, 5, connectFail);
	}

	// for test
		Serial.setCallback(serialCallBack);

}
