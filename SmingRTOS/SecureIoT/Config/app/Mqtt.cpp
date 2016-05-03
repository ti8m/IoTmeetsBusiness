/*
 * Mqtt.cpp
 *
 *  Created on: 28.04.2016
 *      Author: sa005
 */

#include <Mqtt.h>
#include <SmingCore.h>

Timer procTimer;

void onMessageReceived(String topic, String message);

// MQTT client
MqttClient mqtt(MQTT_HOST, MQTT_PORT, onMessageReceived);

// Check for MQTT Disconnection
void checkMQTTDisconnect(TcpClient& client, bool flag){

	// Called whenever MQTT connection is failed.
	if (flag == true)
		Serial.println("MQTT Broker Disconnected!!");
	else
		Serial.println("MQTT Broker Unreachable!!");

	// Restart connection attempt after few seconds
	procTimer.initializeMs(2 * 1000, startMqttClient).start(); // every 2 seconds
}


// Publish our message
void publishMessage(String mac, String message)
{
	if (mqtt.getConnectionState() != eTCS_Connected)
		startMqttClient(); // Auto reconnect

	Serial.println("----------------------- Let's publish message now! -----------------------------");
	mqtt.publish("secureIoT/" + mac + "/state", message); // or publishWithQoS
}


// Run MQTT client
void startMqttClient()
{
	procTimer.stop();
	String device = WifiStation.getMAC();

	// If connection is lost, the broker will send the message LOST_CONNECTION to topic "secureIoT/MAC/state"
	if(!mqtt.setWill("secureIoT" + device + "/state","LOST_CONNECTION", 1, true)) {
		debugf("Unable to set the last will and testament. Most probably there is not enough memory on the device.");
	}
	mqtt.connect(device, MQTT_USERNAME, MQTT_PWD);
	// Assign a disconnect callback function
	mqtt.setCompleteDelegate(checkMQTTDisconnect);
	mqtt.subscribe("secureIoT/" + device + "/command");

	//mqtt.commandProcessing(true,"command","cmdreply");

}

bool isMqttClientConnected(){
	return mqtt.getConnectionState() == eTCS_Connected;
}

