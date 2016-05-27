/*
 * Mqtt.cpp
 *
 *  Created on: 26.05.2016
 *      Author: sa005
 */

#include <SmingCore.h>
#include <Mqtt.h>
#include <Ecdsa.h>

Timer procTimer;
Timer messageTimer;
String mqttMessage;

// MQTT client
MqttClient mqtt(MQTT_HOST, MQTT_PORT, onMessageReceived);


bool isMqttClientConnected() {
	return mqtt.getConnectionState() == eTCS_Connected;
}

/**
 *  Publish MQTT message
 */
void publishMessage() {

	String mac = WifiStation.getMAC();

	if (isMqttClientConnected()) {

		messageTimer.stop();

	} else {

		// reconnect
		startMqttClient();
	}

	Serial.println("MQTT Publish: " + mqttMessage);
	mqtt.publish("secureIoT/device/" + mac, mqttMessage); // or publishWithQoS
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

	// Restart connection attempt after 2 seconds
	procTimer.initializeMs(2000, startMqttClient).startOnce();
}

/**
 *  Run MQTT client
 */
void startMqttClient() {

	String deviceId = WifiStation.getMAC();

	// If connection is lost, the broker will send the message LOST_CONNECTION to topic "secureIoT/device/MAC"
	if (!mqtt.setWill("secureIoT/device" + deviceId, "LOST_CONNECTION", 1, true)) {
		debugf("Unable to set the last will and testament. Most probably there is not enough memory on the device.");
	}

	mqtt.connect(deviceId, MQTT_USERNAME, MQTT_PWD);
	// Assign a disconnect callback function
	mqtt.setCompleteDelegate(checkMQTTDisconnect);

	// Subscribe for messages from mobile-app and server
	mqtt.subscribe("secureIoT/mobile/" + deviceId);
	mqtt.subscribe("secureIoT/server/" + deviceId);

	mqtt.commandProcessing(true,"secureIoT/mobile/" + deviceId,"cmdreply");

}

///**
// *  Callback for messages, arrived from MQTT broker
// */
//void onMessageReceived(String topic, String message) {
//
//	Serial.println("Message received: " + message);
//
//	if (message.equals("DONE")) {
//
////		digitalWrite(GREEN_LED_PIN, false);
//
//		// Reboot in operation mode
////		rboot_set_current_rom(1);
////		Serial.println("Restarting...");
////		System.restart();
//	}
//
//	// Messages from mobile-app
//	if (message.equals("AUTH_REQUEST")) {
//
//		authStartRequest();
//	}
//
//	// Messages from server
//	if (message.equals("START_AUTH")) {
//
//		startAuth();
//	}
//}

void mqttPublish(String message){

	mqttMessage = message;
	messageTimer.initializeMs(2000, publishMessage).start();
}

