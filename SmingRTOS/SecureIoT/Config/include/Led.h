/*
 * Led.h
 *
 *  Created on: 24.02.2016
 *      Author: sa005
 */

#ifndef INCLUDE_LED_H_
#define INCLUDE_LED_H_

#define GREEN_LED_PIN 13 // D7 >> GPIO13
#define RED_LED_PIN 5 // D1 >> GPIO5

void blinkGreen();
void blinkRed();
void blinkStop();
void blinkGreenStart(int interval, int number);
void blinkRedStart(int interval, int number);


#endif /* INCLUDE_LED_H_ */
