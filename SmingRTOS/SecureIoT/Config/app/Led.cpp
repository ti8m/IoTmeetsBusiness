/*
 * Led.cpp
 *
 *  Created on: 24.02.2016
 *      Author: sa005
 */

#include <Led.h>
#include <SmingCore.h>

Timer blinkGreenTimer;
Timer blinkRedTimer;
int blinkCounter = 0;
bool ledState = false;



void blinkGreen()
{

	digitalWrite(GREEN_LED_PIN, ledState);
	ledState = !ledState;

	if (blinkCounter == 0)
	{
		blinkGreenTimer.stop();
		digitalWrite(GREEN_LED_PIN, false);
	}

	if (blinkCounter > 0)
	{
		--blinkCounter;
	}

}

void blinkRed()
{

	digitalWrite(RED_LED_PIN, ledState);
	ledState = !ledState;

	if (blinkCounter == 0)
	{
		blinkRedTimer.stop();
		digitalWrite(RED_LED_PIN, false);
	}

	if (blinkCounter > 0)
	{
		--blinkCounter;
	}

}

void blinkStop()
{

	ledState = false;
	blinkRedTimer.stop();
	blinkGreenTimer.stop();
	digitalWrite(RED_LED_PIN, false);
	digitalWrite(GREEN_LED_PIN, false);
}

void blinkGreenStart(int interval, int number)
{
	blinkStop();
	blinkCounter = number * 2; // set -1 for endless
	blinkGreenTimer.initializeMs(interval, blinkGreen).start();
}

void blinkRedStart(int interval, int number)
{

	blinkStop();
	blinkCounter = number * 2; // set -1 for endless
	blinkRedTimer.initializeMs(interval, blinkRed).start();

}
