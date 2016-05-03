/*
 * Mqtt.h
 *
 *  Created on: 28.04.2016
 *      Author: sa005
 */

#include <SmingCore.h>

#ifndef INCLUDE_MQTT_H_
#define INCLUDE_MQTT_H_

#define MQTT_HOST "m21.cloudmqtt.com"
#define MQTT_PORT 18188
#define MQTT_USERNAME "secureIoT"
#define MQTT_PWD "admin"


void startMqttClient();
void onMessageReceived();
void checkMQTTDisconnect(TcpClient& client, bool flag);
void publishMessage(String mac, String message);
void onMessageReceived();
bool isMqttClientConnected();



#endif /* INCLUDE_MQTT_H_ */
