/*
 * Ecdsa.cpp
 *
 *  Created on: 25.05.2016
 *      Author: sa005
 */

#include <SmingCore.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "security/libraries/micro-ecc/uECC.h"
#include "sming/services/WebHelpers/aw-sha1.h"

#include <Ecdsa.h>
#include <RootKeys.h>
#include <Mqtt.h>
#include <LED.h>

HttpClient keyReq;
HttpClient startReq;
HttpClient challengeReq;

// Device key-pair
uint8_t devicePrivate[24]; // curve-size
uint8_t devicePublic[48]; // 2x curve-size

uint8_t dataHash[20]; // max. curve-size (sha1 >> 20 Bytes)
uint8_t sig[48]; // 2x curve-size

// curve secp192r1
const struct uECC_Curve_t * curve = uECC_secp192r1();


/**
 *  Random number generator for lib micro-ecc
 */
static int OS_RNG(uint8_t *dest, unsigned size) {
	os_get_random(dest, size);
	return 1;
}


/**
 *  Task creating keys and signature
 */
void ecc_prepare_task(void *pvParameters) {

	String mac = WifiStation.getMAC();

	int privateLength = uECC_curve_private_key_size(curve);
	printf("Private-Key Size: %d", privateLength);
	printf("\n");

	int publicLength = uECC_curve_public_key_size(curve);
	printf("Public-Key Size: %d", publicLength);
	printf("\n");

	// create device key-pair
	uECC_make_key(devicePublic, devicePrivate, curve);

	char macArr[13] = { 0 };
	mac.toCharArray(macArr, 13, 0);

	// data array for public-key & mac
	uint8_t data[60] = { 0 }; // 48 for public-key + 12 for mac (without \0)

	// copy public-key to data
	memcpy(data, devicePublic, sizeof(data));

	// add mac to data
	for (int i = 48; i < 60; i++) {

		data[i] = macArr[i - 48];
	}

	// hash public-key & mac (data)
	sha1(dataHash, data, 60);

	// sign the hashed data with the device private-key
	if (!uECC_sign(devicePrivate, dataHash, sizeof(dataHash), sig, curve)) {
		Serial.println("Signing failed");
	}

	Serial.print("Device Public-Key: ");
	for (int i = 0; i < publicLength; i++) {

		Serial.printf("%02x ", devicePublic[i]);
	}

	printf("\n");

	Serial.print("Signature: ");
	for (int i = 0; i < publicLength; i++) {

		Serial.printf("%02x ", sig[i]);
	}

	Serial.println("MAC: " + mac);

	authKeyRequest();

	vTaskDelete(NULL);
}


/**
 *  Save token to flash (defined in rboot.h)
 */
void saveAuthToken(String token){

	rboot_config bootconf;
	bootconf = rboot_get_config();

	char tokenArr[165] = { 0 };
	token.toCharArray(tokenArr, 165, 0);
	memcpy(&bootconf.token, tokenArr, sizeof(bootconf.token));

	rboot_set_config(&bootconf);

}


/**
 *  Start authentication process
 */
void startAuth(){

	blinkGreenStart(100, -1);

	uECC_set_rng(&OS_RNG);
	xTaskCreate(ecc_prepare_task, (const signed char* ) "ecc_prepare_task", 512, NULL, 2, NULL);
}



/**
 *  HTTP response from challenge request
 */
void authChallengeResponse(HttpClient& client, bool successful) {

	int statusCode = client.getResponseCode();

	Serial.println("Server response: " + statusCode);

	if(statusCode == 503){
		Serial.println("Server not running");
		return;
	}

	String json = client.getResponseString();

	DynamicJsonBuffer jsonBuffer;
	JsonObject& root = jsonBuffer.parseObject(json);

	bool success = root["success"];
	String message = root["message"];
	String token = root["token"];

	Serial.println("Server response:");
	Serial.println("status: " + statusCode);
	Serial.println("message: " + message);
	Serial.println("token: " + token);

	if(success){

		mqttPublish("AUTH_SUCCESS");
		saveAuthToken(token);

		blinkStop();
		digitalWrite(GREEN_LED_PIN, true);

	} else {

		mqttPublish("AUTH_FAILED");
		Serial.println("Auth-Challenge-Request failed");
	}

}


/**
 *  Send the signed challenge to the server
 */
void authChallengeRequest(String challenge) {

	String mac = WifiStation.getMAC();

	uint8_t signature[48];

	char challengeBuffer[21] = { 0 };
	challenge.toCharArray(challengeBuffer, 21, 0);

	// data array to sign
	uint8_t data[20] = { 0 };

	// add challenge to data (without /0)
	memcpy(data, challengeBuffer, sizeof(data));

	// sign the challenge with the device private-key
	if (!uECC_sign(devicePrivate, data, sizeof(data), signature, curve)) {

		Serial.println("Signing failed");
	}

	// create body with device-id and signature-array
	String body = "{ ";
	body.concat("\"deviceId\":");
	body.concat("\"" + mac + "\",");
	body.concat("\"signature\": [");

	for (int i = 0; i < sizeof(signature); i++) {
		body.concat(signature[i]);
		if (i < sizeof(signature) - 1)
			body.concat(", ");
	}

	body.concat("]}");

	Serial.println(body);

	challengeReq.setPostBody(body);

	challengeReq.setRequestContentType("application/json");
	challengeReq.downloadString("http://secure-iot-samschaerer.c9users.io/auth/challenge", authChallengeResponse);
}


/**
 *  HTTP response from key request
 */
void authKeyResponse(HttpClient& client, bool successful) {

	int statusCode = client.getResponseCode();

	Serial.println("Server response: " + statusCode);

	if(statusCode == 503){
		Serial.println("Server not running");
		return;
	}

	String json = client.getResponseString();

	DynamicJsonBuffer jsonBuffer;
	JsonObject& root = jsonBuffer.parseObject(json);

	bool success = root["success"];
	String message = root["message"];

	Serial.println("Server response:");
	Serial.println("status: " + statusCode);
	Serial.println("message: " + message);

	if(!success) {
		mqttPublish("AUTH_FAILED");
		Serial.println("Auth-Key-Request failed");
	}

}


/**
 *  Send signed public-key and device-id to server
 */
void authKeyRequest() {

	String mac = WifiStation.getMAC();

	// Create request-body
	String body = "{ ";
	body.concat("\"deviceId\":");
	body.concat("\"" + mac + "\",");
	body.concat("\"publicKey\": [");

	// create public-key as array
	for (int i = 0; i < sizeof(devicePublic); i++) {
		body.concat(devicePublic[i]);
		if (i < sizeof(devicePublic) - 1)
			body.concat(", ");
	}

	body.concat("],");

	body.concat("\"signature\": [");

	// create signature as array
	for (int i = 0; i < sizeof(sig); i++) {
		body.concat(sig[i]);
		if (i < sizeof(sig) - 1)
			body.concat(", ");
	}

	body.concat("]}");

	Serial.println(body);

	// set request-body
	keyReq.setPostBody(body);

	keyReq.setRequestContentType("application/json");
	keyReq.downloadString("http://secure-iot-samschaerer.c9users.io/auth/key", authKeyResponse);
}


/**
 *  HTTP response from signing request
 */
void authSigningResponse(HttpClient& client, bool successful) {

	int statusCode = client.getResponseCode();

	Serial.println("Server response: " + statusCode);

	if(statusCode == 503){
		Serial.println("Server not running");
		return;
	}

	String json = client.getResponseString();

	DynamicJsonBuffer jsonBuffer;
	JsonObject& root = jsonBuffer.parseObject(json);

	bool success = root["success"];
	String message = root["message"];
	String challenge = root["challenge"];

	Serial.println("Server response:");
	Serial.println("status: " + statusCode);
	Serial.println("message: " + message);
	Serial.println("challenge: " + challenge);
	Serial.print("Signature: ");

	if(!success){
		mqttPublish("AUTH_FAILED");
		Serial.println("Auth-Signing-Request failed");
		return;
	}

	// create signature array
	uint8_t signature[48] = {0};
	for (int i = 0; i < sizeof(signature); i++) {

		signature[i] = root["signature"][i];
		Serial.printf("%d ", signature[i]);
	}

	Serial.println("");


	// Verify signature with root public-key and data-hash of device-id and device public-key
	if (uECC_verify(rootPublic, dataHash, sizeof(dataHash), signature, curve)) {

		Serial.println("Signature is valid");

		// Sign the challenge and send it back to the server
		authChallengeRequest(challenge);

	} else {
		Serial.println("Invalid signature");
		mqttPublish("AUTH_FAILED");
	}

}


/**
 *  Request the signed device public-key by the server
 */
void authSigningRequest() {

	String mac = WifiStation.getMAC();

	// Create request-body
	String body = "{ ";
	body.concat("\"deviceId\":");
	body.concat("\"" + mac + "\"}");

	// set request-body
	startReq.setPostBody(body);

	startReq.setRequestContentType("application/json");
	startReq.downloadString("http://secure-iot-samschaerer.c9users.io/auth/sign", authSigningResponse);
}

