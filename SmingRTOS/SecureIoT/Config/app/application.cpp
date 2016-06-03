/**
 *  CONFIG-MODE >> rom1.bin
 */

#include <user_config.h>
#include <SmingCore.h>
#include "rboot/rboot.h"

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
bool smartConfigActive;

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

		startAuth();
	}

	if (message.equals("AUTH_CANCEL")) {

		WifiStation.disconnect();
		smartconfig_stop();
		setConfigMode(true);
	}


	if (message.equals("AUTH_END")) {

		setConfigMode(false);
	}

	// Messages from server
	if (message.equals("AUTH_ACCEPTED")) {

		authSigningRequest();
	}
}

/**
 *  WiFi station connected to AP
 */
void connectOk() {

	// Waiting for smart-config state LINK_OVER
	if (smartConfigActive)
		return;

	debugf("I'm CONNECTED");
	Serial.println(WifiStation.getIP().toString());
	blinkStop();

	// run MQTT client
	startMqttClient();

	mqttPublish("CONNECTED");

	blinkGreenStart(500, -1);

}

/**
 *  Connection timeout reached
 */
void connectFail() {

	debugf("CAN NOT CONNECT");

	blinkStop();
	digitalWrite(RED_LED_PIN, true);

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
		case SC_STATUS_WAIT: {

			Serial.println("SC_STATUS_WAIT");
			break;
		}
		case SC_STATUS_FIND_CHANNEL: {

			Serial.println("SC_STATUS_FIND_CHANNEL");
			blinkGreenStart(100, -1);
			break;
		}
		case SC_STATUS_GETTING_SSID_PSWD: {

			Serial.println("SC_STATUS_GETTING_SSID_PSWD");
			sc_type *type = (sc_type *) pdata;

			if (*type == SC_TYPE_ESPTOUCH) {

				Serial.println("SC_TYPE: SC_TYPE_ESPTOUCH");

			} else {

				Serial.println("SC_TYPE: SC_TYPE_AIRKISS");
			}

			break;
		}
		case SC_STATUS_LINK: {

			Serial.println("SC_STATUS_LINK");
			struct station_config *sta_conf = (station_config *) pdata;
			char *ssid = (char*) sta_conf->ssid;
			char *password = (char*) sta_conf->password;
			WifiStation.config(ssid, password);

			// If connecting fails, the callback with state LINK_OVER is never called.
			// So we already try to connect here (if successful, we skip connectOk() and wait for the state LINK_OVER)
			WifiStation.waitConnection(connectOk, 10, connectFail);

			break;
		}
		case SC_STATUS_LINK_OVER: {
			Serial.println("SC_STATUS_LINK_OVER");

			if (pdata != NULL) {
				uint8 phone_ip[4] = { 0 };

				memcpy(phone_ip, (uint8*) pdata, 4);
				Serial.printf("Phone ip: %d.%d.%d.%d\n", phone_ip[0], phone_ip[1], phone_ip[2], phone_ip[3]);
			}

			//setConfigMode(false);
			smartConfigActive = false;
			WifiStation.waitConnection(connectOk, 10, connectFail);
			break;
		}
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
		smartConfigActive = true;

	} else {

		configMode = false;
		blinkStop();
		smartconfig_stop();
		smartConfigActive = false;
		WifiStation.waitConnection(connectOk, 10, connectFail);

		// Reboot in operation mode
		rboot_set_current_rom(0);
		Serial.println("Restarting...");
		System.restart();
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



void init() {

	Serial.begin(SERIAL_BAUD_RATE); // 115200 by default
	Serial.systemDebugOutput(true); // Allow debug output to serial

	int slot = rboot_get_current_rom();
	Serial.printf("\r\nCurrently running rom %d.\r\n", slot);


	if (!digitalRead(SWITCH_PIN)) {

			// Reboot in operation-mode
			rboot_set_current_rom(0);
			Serial.println("Restarting...");
			System.restart();
		}

	pinMode(GREEN_LED_PIN, OUTPUT);
	pinMode(RED_LED_PIN, OUTPUT);
	pinMode(SWITCH_PIN, INPUT);



	WifiStation.enable(true);
	WifiStation.enableDHCP(true);

	WifiAccessPoint.enable(false);

	configMode = false;
	smartConfigActive = false;

	debounceTimer.initializeMs(1200, debounceReset);
	configTimer.initializeMs(1000, switchDelay);
	attachInterrupt(SWITCH_PIN, switchPressed, RISING);

	setConfigMode(true);

}
