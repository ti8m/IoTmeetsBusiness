#include <user_config.h>
#include <SmingCore.h>

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "espressif/smartconfig.h"
#include <espressif/esp_system.h>

#include <Libraries/DHT/DHT.h>
#include <Led.h>
#include <Mqtt.h>

#define SWITCH_PIN 4 // D2 >> GPIO4
#define SENSOR_PIN 14 // D5 >> GPIO14


#define server_ip "192.168.101.142"
#define server_port 9669

DHT sensor(SENSOR_PIN, DHT22); // DHT22

Timer configTimer;
bool configMode;

Timer debounceTimer;
bool debounceActive = false;

Timer sleepTimer;
Timer disconnectTimer;
Timer messageTimer;

HttpServer server;

BssList networks;
Timer connectionTimer;

HttpClient thingSpeak;
Timer sendDataTimer;

bool successSent;

String mac;
String mqttMessage;

// Forward declarations
void setConfigMode(bool on);


void sleeping() {
	//System.deepSleep(30000, eDSO_RF_CAL_ALWAYS);
	System.deepSleep(30000);
}

void sendMessage(){

	if (isMqttClientConnected())
			messageTimer.stop();

	publishMessage(mac, mqttMessage);
}



// Will be called when WiFi station was connected to AP
void connectOk() {

	// Waiting for smart-config state LINK_OVER
	if(configMode)
		return;

	debugf("I'm CONNECTED");
	Serial.println(WifiStation.getIP().toString());
	blinkStop();

	// set mac-address
	mac = WifiStation.getMAC();

	// run MQTT client
	startMqttClient();

	// publish message CONNECTED
	mqttMessage = "CONNECTED";
	messageTimer.start();

	// start sensor reading
	//sensor.begin();

	// start sending data
	//sendDataTimer.startOnce();
}


// Will be called when WiFi station timeout was reached
void connectFail() {

	debugf("CAN NOT CONNECT");

	blinkStop();
	digitalWrite(RED_LED_PIN, true);
	WifiStation.disconnect();

	if (configMode) {

		configMode = false;
		smartconfig_stop();

	} else {
		sleepTimer.startOnce();
	}

}


// smart-config callback
void ICACHE_FLASH_ATTR
smartconfig_callback(sc_status status, void *pdata) {
	switch (status) {
	case SC_STATUS_WAIT:

		printf("SC_STATUS_WAIT\n");
		break;

	case SC_STATUS_FIND_CHANNEL:

		printf("SC_STATUS_FIND_CHANNEL\n");
		blinkGreenStart(100, -1);
		break;

	case SC_STATUS_GETTING_SSID_PSWD: {
		printf("SC_STATUS_GETTING_SSID_PSWD\n");
		sc_type *type = (sc_type *) pdata;
		if (*type == SC_TYPE_ESPTOUCH) {
			printf("SC_TYPE:SC_TYPE_ESPTOUCH\n");
		} else {
			printf("SC_TYPE:SC_TYPE_AIRKISS\n");
		}
		break;
	}
	case SC_STATUS_LINK: {

		debugf("SC_STATUS_LINK\n");
		struct station_config *sta_conf = (station_config *) pdata;
		char *ssid = (char*) sta_conf->ssid;
		char *password = (char*) sta_conf->password;
		WifiStation.config(ssid, password);

		// If connecting fails, the callback with state LINK_OVER is never called.
		// So we already try to connect here (if successful, we skip connectOk() and wati fot the state LINK_OVER)
		WifiStation.waitConnection(connectOk, 10, connectFail);

		break;
	}
	case SC_STATUS_LINK_OVER:
		printf("SC_STATUS_LINK_OVER\n");

		if (pdata != NULL) {
			uint8 phone_ip[4] = { 0 };

			memcpy(phone_ip, (uint8*) pdata, 4);
			printf("Phone ip: %d.%d.%d.%d\n", phone_ip[0], phone_ip[1], phone_ip[2], phone_ip[3]);
		}

//		configMode = false;
//		WifiStation.waitConnection(connectOk, 5, connectFail);
//		smartconfig_stop();
		setConfigMode(false);
		break;
	}

}

// smart-config task
void ICACHE_FLASH_ATTR
smartconfig_task(void *pvParameters) {
	smartconfig_start(smartconfig_callback);

	vTaskDelete(NULL);
}


void setConfigMode(bool on) {
	if (on) {

		configMode = true;
		blinkGreenStart(500, -1);

		// start smart-config
		xTaskCreate(smartconfig_task, (const signed char* ) "smartconfig_task", 256, NULL, 2, NULL);

	} else {

		configMode = false;
		blinkStop();
		smartconfig_stop();
		WifiStation.waitConnection(connectOk, 10, connectFail);
	}
}

void restartConfigMode(){

	setConfigMode(false);
	delay(1000);
	setConfigMode(true);
}








// ThingSpeak stuff *****************************************************************************************

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

	WifiStation.disconnect();
	sleepTimer.startOnce();
}

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

//**********************************************************************************************************

void switchDelay() {
	if (digitalRead(SWITCH_PIN)) {

		setConfigMode(!configMode);
	}
}

void switchPressed() {
	if (!debounceActive) {

		Serial.println("Switch pressed");
		Serial.println(digitalRead(SWITCH_PIN));

		// set config-mode after 1s if switch is already pressed
		configTimer.startOnce();

		debounceActive = true;
		debounceTimer.startOnce();
	}

}

void debounceReset() {
	debounceActive = false;
}

void networkScanCompleted(bool succeeded, BssList list) {
	if (succeeded) {
		for (int i = 0; i < list.count(); i++)
			if (!list[i].hidden && list[i].ssid.length() > 0)
				networks.add(list[i]);
	}
	networks.sort([](const BssInfo& a, const BssInfo& b)
	{	return b.rssi - a.rssi;});
}


// Callback for messages, arrived from MQTT server
void onMessageReceived(String topic, String message)
{
	Serial.println(topic);
	Serial.println(message);

	if(message.equals("DONE")){

		// start sensor reading
		sensor.begin();

		// start sending data
		sendDataTimer.startOnce();
	}
}



void init() {

	Serial.begin(SERIAL_BAUD_RATE); // 115200 by default
	Serial.systemDebugOutput(true); // Debug output to serial
	commandHandler.registerSystemCommands();

	int slot = rboot_get_current_rom();
	Serial.printf("\r\nCurrently running rom %d.\r\n", slot);

	pinMode(GREEN_LED_PIN, OUTPUT);
	pinMode(RED_LED_PIN, OUTPUT);
	pinMode(SWITCH_PIN, INPUT);

	WifiStation.enable(true);
	WifiStation.enableDHCP(true);

	WifiAccessPoint.enable(false);

	sendDataTimer.initializeMs(1000, sendData);
	sleepTimer.initializeMs(1000, sleeping);
	messageTimer.initializeMs(2000, sendMessage);

	configMode = false;
	successSent = false;

	if (digitalRead(SWITCH_PIN)) {
		debounceTimer.initializeMs(1200, debounceReset);
		configTimer.initializeMs(1000, switchDelay);
		attachInterrupt(SWITCH_PIN, switchPressed, RISING);

		setConfigMode(true);

	} else {
		WifiStation.waitConnection(connectOk, 5, connectFail);
	}

}
