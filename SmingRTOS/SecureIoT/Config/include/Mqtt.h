/*
 * Mqtt.h
 *
 *  Created on: 26.05.2016
 *      Author: sa005
 */

#ifndef INCLUDE_MQTT_H_
#define INCLUDE_MQTT_H_

#define MQTT_HOST "m21.cloudmqtt.com"
#define MQTT_PORT 18188
#define MQTT_USERNAME "secureIoT"
#define MQTT_PWD "admin"

void mqttPublish(String message);
void startMqttClient();
void onMessageReceived(String topic, String message);


#endif /* INCLUDE_MQTT_H_ */
