/**
 *  OPERATION-MODE >> rom0.bin
 */

#include <user_config.h>
#include <SmingCore.h>
#include "rboot/rboot.h"

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "espressif/smartconfig.h"
#include <espressif/esp_system.h>

#include <Libraries/DHT/DHT.h>
#include <Led.h>

#define SWITCH_PIN 4 // D2 >> GPIO4
#define SENSOR_PIN 14 // D5 >> GPIO14

DHT sensor(SENSOR_PIN, DHT22); // DHT22

Timer debounceTimer;
bool debounceActive = false;

Timer sleepTimer;
Timer disconnectTimer;
Timer connectionTimer;

HttpClient http;
Timer sendDataTimer;

bool successSent;

String authToken;


void sleeping() {
	System.deepSleep(30000);
}


/**
 *  WiFi station connected to AP
 */
void connectOk() {

	debugf("I'm CONNECTED");
	Serial.println(WifiStation.getIP().toString());
	blinkStop();

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
 *  Http-Client has sent data
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
	Serial.println("Server response: " + response);

	sleepTimer.startOnce();
}


/**
 *  Read Data from Sensor and sent them to the server
 */
void sendData() {
	if (http.isProcessing())
		return; // wait while request processing was completed

	TempAndHumidity th;
	if (sensor.readTempAndHumidity(th)) {
		Serial.print("\tHumidity: ");
		Serial.println(th.humid);
		Serial.print("% Temperature: ");
		Serial.println(th.temp);

		String mac = WifiStation.getMAC();

		// create body with temperature and humidity
		String body = "{ ";
		body.concat("\"deviceId\":");
		body.concat("\"" + mac + "\",");
		body.concat("\"temp\":");
		body.concat("\"" + String(th.temp) + "\",");
		body.concat("\"humid\":");
		body.concat("\"" + String(th.humid) + "\"");
		body.concat("}");

		Serial.println(body);

		http.setPostBody(body);

		// set header with token
		http.setRequestHeader("x-access-token", authToken);
		http.setRequestContentType("application/json");
		http.downloadString("http://secure-iot-samschaerer.c9users.io/service/data", onDataSent);


	} else {
		Serial.print("Failed to read from DHT: ");
		Serial.print(sensor.getLastError());
		sleepTimer.startOnce();
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


/**
 *  Get current auth-token from rboot-config (defined in rboot.h)
 */
String getAuthToken(){

	String authToken;

	rboot_config bootconf;
	bootconf = rboot_get_config();

	char tokenArr[165] = { 0 };
	memcpy(tokenArr, &bootconf.token, sizeof(tokenArr));

	authToken = tokenArr;
	Serial.println("Auth-Token: " + authToken);

	return authToken;
}


void init() {

	Serial.begin(SERIAL_BAUD_RATE); // 115200 by default
	Serial.systemDebugOutput(true); // Debug output to serial

	int slot = rboot_get_current_rom();
	Serial.printf("\r\nCurrently running rom %d.\r\n", slot);

	authToken = getAuthToken();

	pinMode(GREEN_LED_PIN, OUTPUT);
	pinMode(RED_LED_PIN, OUTPUT);
	pinMode(SWITCH_PIN, INPUT);

	WifiStation.enable(true);
	WifiStation.enableDHCP(true);

	WifiAccessPoint.enable(false);

	sendDataTimer.initializeMs(1000, sendData);
	sleepTimer.initializeMs(5000, sleeping);

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
