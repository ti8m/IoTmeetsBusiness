/**
 *  CONFIG-MODE >> rom1.bin
 */

#include <user_config.h>
#include <SmingCore.h>

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "espressif/smartconfig.h"
#include <espressif/esp_system.h>

#include "security/libraries/micro-ecc/uECC.h"
#include "sming/services/WebHelpers/aw-sha1.h"

#include <Libraries/DHT/DHT.h>
#include <Ecdsa.h>
#include <Mqtt.h>
#include <Led.h>

#define SWITCH_PIN 4 // D2 >> GPIO4

Timer configTimer;
bool configMode;

Timer debounceTimer;
bool debounceActive = false;

Timer disconnectTimer;

Timer connectionTimer;

// Forward declarations
void setConfigMode(bool on);


/**
 *  Callback for messages, arrived from MQTT broker
 */
void onMessageReceived(String topic, String message) {

	Serial.println("Message received: " + message);

	// Messages from mobile-app
	if (message.equals("AUTH_REQUEST")) {

		authStartRequest();
	}

	// Messages from server
	if (message.equals("START_AUTH")) {

		startAuth();
	}
}

/**
 *  WiFi station connected to AP
 */
void connectOk() {

	// Waiting for smart-config state LINK_OVER
	if (configMode)
		return;

	debugf("I'm CONNECTED");
	Serial.println(WifiStation.getIP().toString());
	blinkStop();

	// run MQTT client
	startMqttClient();

	mqttPublish("CONNECTED");

	digitalWrite(GREEN_LED_PIN, true);

}

/**
 *  Connection timeout reached
 */
void connectFail() {

	debugf("CAN NOT CONNECT");

	blinkStop();
	digitalWrite(RED_LED_PIN, true);
	//WifiStation.disconnect();

	if (configMode) {

		configMode = false;
		smartconfig_stop();
	}

}

/**
 *  smart-config callback
 */
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
		// So we already try to connect here (if successful, we skip connectOk() and wait for the state LINK_OVER)
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

		setConfigMode(false);
		break;
	}

}

/**
 *  smart-config task
 */
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

		// Reboot in operation mode
//		rboot_set_current_rom(0);
//		Serial.println("Restarting...");
//		System.restart();
	}
}

/**
 *  Called after 1s when switch is pressed
 */
void switchDelay() {
	if (digitalRead(SWITCH_PIN)) {

		setConfigMode(!configMode);
	}
}

/**
 *  Handle switch interrupts (Debounce)
 */
void switchPressed() {
	if (!debounceActive) {

		Serial.println("Switch pressed");
		Serial.println(digitalRead(SWITCH_PIN));

		// Set config-mode after 1s if switch is already pressed
		configTimer.startOnce();

		debounceActive = true;
		debounceTimer.startOnce();
	}

}

/**
 *  Reset the switch debounce
 */
void debounceReset() {
	debounceActive = false;
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

void test() {

	//xTaskCreate(ecc_prepare_task, (const signed char* ) "ecc_prepare_task", 512, NULL, 2, NULL);
	startAuth();

}

// for testing
void init() {

	Serial.begin(SERIAL_BAUD_RATE); // 115200 by default
	Serial.systemDebugOutput(true); // Allow debug output to serial

	configMode = false;

	WifiStation.config("MeBu-Mobil", "0987qWerty");
	WifiStation.enable(true);
	WifiAccessPoint.enable(false);

	//WifiStation.waitConnection(test);
	WifiStation.waitConnection(connectOk, 10, connectFail);
}

void init2() {

	Serial.begin(SERIAL_BAUD_RATE); // 115200 by default
	Serial.systemDebugOutput(true); // Debug output to serial
	//commandHandler.registerSystemCommands();

	// Enable rboot in makefile!
//	int slot = rboot_get_current_rom();
//	Serial.printf("\r\nCurrently running rom %d.\r\n", slot);
//
//	if (!digitalRead(SWITCH_PIN)) {
//
//			// Reboot in operation-mode
//			rboot_set_current_rom(0);
//			Serial.println("Restarting...");
//			System.restart();
//		}

	pinMode(GREEN_LED_PIN, OUTPUT);
	pinMode(RED_LED_PIN, OUTPUT);
	pinMode(SWITCH_PIN, INPUT);

	WifiStation.enable(true);
	WifiStation.enableDHCP(true);

	WifiAccessPoint.enable(false);

//	messageTimer.initializeMs(2000, publishMessage);

	configMode = false;

	debounceTimer.initializeMs(1200, debounceReset);
	configTimer.initializeMs(1000, switchDelay);
	attachInterrupt(SWITCH_PIN, switchPressed, RISING);

	setConfigMode(true);

}
