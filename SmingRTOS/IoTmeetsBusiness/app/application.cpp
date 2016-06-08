#include <user_config.h>
#include <SmingCore.h>
#include <Libraries/DHT/DHT.h>
#include <AppSettings.h>
#include <Led.h>

#define SWITCH_PIN 4 // D2 >> GPIO4
#define SENSOR_PIN 14 // D5 >> GPIO14

DHT sensor(SENSOR_PIN, DHT22); // DHT22

Timer configTimer;
bool configMode;

Timer debounceTimer;
bool debounceActive = false;

Timer sleepTimer;
Timer disconnectTimer;

HttpServer server;

BssList networks;
String network, password;
Timer connectionTimer;

HttpClient thingSpeak;
Timer sendDataTimer;

bool successSent;

void sleeping()
{
	//System.deepSleep(30000, eDSO_RF_CAL_ALWAYS);
	System.deepSleep(30000);
}


// Will be called when WiFi station was connected to AP
void connectOk()
{
	debugf("I'm CONNECTED");
	Serial.println(WifiStation.getIP().toString());

	blinkStop();

	// start sensor reading
	sensor.begin();

	// start sending data
	sendDataTimer.startOnce();
}


// Will be called when WiFi station timeout was reached
void connectFail()
{
	debugf("CAN NOT CONNECT");
	Serial.println("Config-Mode is: " + configMode);

	blinkStop();
	digitalWrite(RED_LED_PIN, true);

	if(configMode)
	{
		delay(1000);
		blinkGreenStart(500, -1);
	}
	else
	{
		WifiStation.disconnect();
		sleepTimer.startOnce();
	}

}


void disableAP()
{
	WifiAccessPoint.enable(false);
}


// ThingSpeak stuff *****************************************************************************************

void onDataSent(HttpClient& client, bool successful)
{
	if (successSent)
		return;

	Serial.println("ON DATA SENT: ");

	if (successful)
	{
		successSent = true;
		blinkGreenStart(200, 2);
		Serial.println("Success sent");
	}
	else if (!successSent)
	{
		blinkRedStart(200, 4);
		Serial.println("Failed");

	}

	String response = client.getResponseString();
	Serial.println("Server response: '" + response + "'");
	if (response.length() > 0)
	{
		int intVal = response.toInt();

		if (intVal == 0)
			Serial.println("Sensor value wasn't accepted");
	}

	WifiStation.disconnect();
	sleepTimer.startOnce();
}

void sendData()
{
	if (thingSpeak.isProcessing())
		return; // We need to wait while request processing was completed

	TempAndHumidity th;
	if (sensor.readTempAndHumidity(th))
	{
		Serial.print("\tHumidity: ");
		Serial.println(th.humid);
		Serial.print("% Temperature: ");
		Serial.println(th.temp);

		thingSpeak.downloadString("http://api.thingspeak.com/update?key=" + AppSettings.channelKey + "&field1=" + String(th.temp) + "&field2=" + String(th.humid), onDataSent);
	}
	else
	{
		Serial.print("Failed to read from DHT: ");
		Serial.print(sensor.getLastError());
	}

}

//**********************************************************************************************************

void setConfigMode()
{
	if (!configMode)
	{
		configMode = true;

		// Start config-mode
		blinkGreenStart(500, -1);
		WifiAccessPoint.enable(true);
	}
	else
	{
		configMode = false;

		// Stop config-mode
		blinkStop();
		WifiAccessPoint.enable(false);
		WifiStation.waitConnection(connectOk, 5, connectFail);
	}
}

void switchDelay()
{
	if (digitalRead(SWITCH_PIN))
	{
		setConfigMode();
	}
}

void switchPressed()
{
	if (!debounceActive)
	{

		Serial.println("Switch pressed");
		Serial.println(digitalRead(SWITCH_PIN));

		// set config-mode after 1s if switch is already pressed
		configTimer.startOnce();

		debounceActive = true;
		debounceTimer.startOnce();
	}

}

void debounceReset()
{
	debounceActive = false;
}



void onAjaxNetworkList(HttpRequest &request, HttpResponse &response)
{
	JsonObjectStream* stream = new JsonObjectStream();
	JsonObject& json = stream->getRoot();

	json["status"] = (bool)true;

	bool connected = WifiStation.isConnected();
	json["connected"] = connected;
	if (connected)
	{
		// Copy full string to JSON buffer memory
		json["network"]= WifiStation.getSSID();
	}

	JsonArray& netlist = json.createNestedArray("available");
	for (int i = 0; i < networks.count(); i++)
	{
		if (networks[i].hidden) continue;
		JsonObject &item = netlist.createNestedObject();
		item["id"] = (int)networks[i].getHashId();
		// Copy full string to JSON buffer memory
		item["title"] = networks[i].ssid;
		item["signal"] = networks[i].rssi;
		item["encryption"] = networks[i].getAuthorizationMethodName();
	}

	response.setAllowCrossDomainOrigin("*");
	response.sendJsonObject(stream);
}

void makeConnection()
{
	WifiStation.config(network, password);

	network = "";
	password = "";

	blinkGreenStart(100, -1);

	WifiStation.waitConnection(connectOk, 20, connectFail);
}

void onAjaxConnect(HttpRequest &request, HttpResponse &response)
{
	response.setHeader("Access-Control-Allow-Origin", "*");
	response.setHeader("Access-Control-Allow-Methods", "POST");
	response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");

    // parse the json-string from request body
	DynamicJsonBuffer jsonBuffer;
	String bodyString = request.getBody();
	int stringLength = request.getContentLength();
	char* requestJson = new char[stringLength + 1];
	strcpy(requestJson, bodyString.c_str());
	JsonObject& body = jsonBuffer.parseObject(requestJson);

    // get parameter form json
	String newNetwork = body["network"].asString();
	String newPassword = body["password"].asString();

	AppSettings.channelKey = body["channelKey"].asString();
	AppSettings.save();

	// json for response
	JsonObjectStream* stream = new JsonObjectStream();
	JsonObject& responseJson = stream->getRoot();

    // try connecting
	network = newNetwork;
	password = newPassword;
	debugf("CONNECT TO: %s %s", network.c_str(), password.c_str());
	connectionTimer.initializeMs(1200, makeConnection).startOnce();

    // response always connected because we don't know yet if connecting was successful
	responseJson["status"] = (bool) true;
	responseJson["connected"] = true;

	response.sendJsonObject(stream);
}

void onAjaxGetIP(HttpRequest &request, HttpResponse &response)
{
	response.setHeader("Access-Control-Allow-Origin", "*");
	response.setHeader("Access-Control-Allow-Methods", "GET");
	response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");

	JsonObjectStream* stream = new JsonObjectStream();
	JsonObject& json = stream->getRoot();

	json["ip"]= WifiStation.getIP().toString();

	response.sendJsonObject(stream);
}

void onAjaxDisableAP(HttpRequest &request, HttpResponse &response)
{
	response.setHeader("Access-Control-Allow-Origin", "*");
	response.setHeader("Access-Control-Allow-Methods", "GET");
	response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");

	//connectionTimer.initializeMs(2000, disableAP).startOnce();
}

// Will be called when system initialization was completed
void startWebServer()
{
	Serial.println("starting web-server...");
	server.listen(80);
	server.addPath("/ajax/get-networks", onAjaxNetworkList);
	server.addPath("/ajax/connect", onAjaxConnect);
	server.addPath("/ajax/get-ip", onAjaxGetIP);
	server.addPath("/ajax/disable-ap", onAjaxDisableAP);

}

void networkScanCompleted(bool succeeded, BssList list)
{
	if (succeeded)
	{
		for (int i = 0; i < list.count(); i++)
			if (!list[i].hidden && list[i].ssid.length() > 0)
				networks.add(list[i]);
	}
	networks.sort([](const BssInfo& a, const BssInfo& b)
	{	return b.rssi - a.rssi;});
}

void init()
{
	spiffs_mount(); // Mount file system

	Serial.begin(SERIAL_BAUD_RATE); // 115200 by default
	Serial.systemDebugOutput(true); // Enable debug output to serial

	AppSettings.load();

	pinMode(GREEN_LED_PIN, OUTPUT);
	pinMode(RED_LED_PIN, OUTPUT);
	pinMode(SWITCH_PIN, INPUT);

	WifiStation.enable(true);
	WifiStation.enableDHCP(true);

	sendDataTimer.initializeMs(1000, sendData);
	sleepTimer.initializeMs(1000, sleeping);

	configMode = false;
	successSent = false;

	String currentNetwork = WifiStation.getSSID();
	String currentPassword = WifiStation.getPassword();


	if (digitalRead(SWITCH_PIN) || currentNetwork.length() == 0 || currentPassword.length() == 0)
	{
		debounceTimer.initializeMs(1200, debounceReset);
		configTimer.initializeMs(1000, switchDelay);

		//attachInterrupt(SWITCH_PIN, switchPressed, RISING);

		startWebServer();
		WifiStation.startScan(networkScanCompleted);
		WifiAccessPoint.config("iot-meets-business", "", AUTH_OPEN); // AUTH_WPA_WPA2_PSK
		setConfigMode();
	}
	else
	{
		WifiStation.waitConnection(connectOk, 5, connectFail);
	}

}
