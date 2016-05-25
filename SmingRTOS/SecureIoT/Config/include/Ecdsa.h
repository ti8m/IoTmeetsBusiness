/*
 * Ecdsa.h
 *
 *  Created on: 25.05.2016
 *      Author: sa005
 */

#ifndef INCLUDE_ECDSA_H_
#define INCLUDE_ECDSA_H_

void startEccPrepareTask();
void authKeyRequest();
void authKeyResponse(HttpClient& client, bool successful);
void authChallengeRequest(String challenge);
void authChallengeResponse(HttpClient& client, bool successful);


#endif /* INCLUDE_ECDSA_H_ */
