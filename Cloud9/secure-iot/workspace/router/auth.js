
var express = require('express');
var Device  = require('../models/device');
var jwt     = require('jsonwebtoken'); 
var crypto  = require('crypto');
var ecdh    = require('../node_modules/ecdh/index');
var random  = require("randomstring");
var mqtt    = require('mqtt');

// Set curve-type
var curve = ecdh.getCurve('secp192r1');

// Set hash algorithm
var algorithm = 'sha1';

var challenge;

module.exports = function(app) {
    

// get an instance of the router for auth routes
var authRoutes = express.Router();

// Test Var
var INVALID_KEY_SIG = false;
var INVALID_CHALLENGE_SIG = false;
var INVALID_DEVICE_ID = false;


// ==================================
// Auth Key-Route ===================
// ==================================
authRoutes.post('/key', function(req, res) {
  
  // Get request data
  var deviceId = req.body.deviceId; // String
  var devicePublicKeyArray = req.body.publicKey; // Array
  var signature = req.body.signature; // Array
  
  // Create signature-buffer (split array in r and s)
  var length = signature.length;
  
  var r = signature.slice(0, length/2);
  var s = signature.slice(length/2);
  
  var signatureBuffer = serializeSig(r, s);
  
  // Create buffer from device-id
  var devicIdBuffer = new Buffer(deviceId);
  
  // Create device public-key
  var devicePublicBuffer = new Buffer(devicePublicKeyArray);
  var devicePublicKey = ecdh.PublicKey.fromBuffer(curve, devicePublicBuffer);
  
  // Create data-buffer to hash
  var bufLength = devicePublicBuffer.length + devicIdBuffer.length;
  var dataBuffer = Buffer.concat([devicePublicBuffer, devicIdBuffer], bufLength);
  
  // Hash device public-key & device-id (data)
  var dataHash = crypto.createHash(algorithm).update(dataBuffer).digest();
  
  // validate signature with the data-hash and device public-key
  var isValid = devicePublicKey.verifySignature(dataHash, signatureBuffer);
  console.log('Key-Signature is', isValid ? 'valid' : 'invalid');
  
  if (isValid && !INVALID_KEY_SIG) {
        
        // Save device
        saveDevice(deviceId, devicePublicKeyArray );
        
        // Start MQTT-Client for validation by user
        
        // Setup mqtt-connection
        var id = 'mqttjs_' + Math.random().toString(16).substr(2, 8);
        var broker = app.get('mqtt-broker');
        var options = app.get('mqtt-options');
        options.clientId = id;
  
        // Connect to mqtt-broker
        var client = mqtt.connect(broker, options);
  
        client.on('connect', function () {
        console.log("MQTT connected");
    
        // Subscribe to auth-messages from moblie-app
        client.subscribe('secureIoT/mobile/'+ deviceId);
    
        // Publish validation-message to mobile-app
        client.publish('secureIoT/server/' + deviceId, 'AUTH_VALIDATE');

        });

        client.on('message', function (topic, message) {
          
          var msg = message.toString();
          console.log("mqtt-message received: " + msg);
    
          if(msg === "AUTH_CONFIRM"){
            
            // Publish message to device
            client.publish('secureIoT/server/' + deviceId, 'AUTH_ACCEPTED');
    
            client.end();
          }
    
          if(msg === "AUTH_REJECT"){
            
            client.end();
          }
    
      });
      
      return res.status(202).send({
        success: true,
        message: 'Signature verified. Waiting for validation by user...'});
        
    } else {
        
        return res.status(401).send({
          success: false,
          message: 'Signature not valid'});
        
      }

});


// ==================================
// Auth-Signing Route ===============
// ==================================
authRoutes.post('/sign', function(req, res) {
  
  console.log("POST on auth/sign");
  
  // Get request data
  var deviceId = req.body.deviceId; // String
  
  // Check if device exist
  Device.findOne({deviceId: deviceId}, function(err, device) {
    if (err) throw err;
          
    if(device && !INVALID_DEVICE_ID) {
      
      // Get root private-key
      var keyBuffer = new Buffer(app.get('root-private-key'), "hex");
      var rootPrivateKey = ecdh.PrivateKey.fromBuffer(curve, keyBuffer);
      
      // Create buffer from device-id
      var devicIdBuffer = new Buffer(deviceId);
      
      // Create buffer from device public-key
      var devicePublicBuffer = new Buffer(device.publicKey);
      
      // Create data-buffer to hash
      var bufLength = devicePublicBuffer.length + devicIdBuffer.length;
      var dataBuffer = Buffer.concat([devicePublicBuffer, devicIdBuffer], bufLength);
      
      // Hash device public-key & device-id (data)
      var dataHash = crypto.createHash(algorithm).update(dataBuffer).digest();
      
      // Sign hashed public-key & device-id with the root private-key
      var signature = rootPrivateKey.sign(dataHash, algorithm);
      
      // Create array from signature
      var signatureBuffer = deserializeSig(signature);
      var uint8Array = new Uint8Array(signatureBuffer);
      var signatureArray = [].slice.call(uint8Array);
      console.log("Signature: " + signatureArray);
      
      // Create challenge
      challenge = random.generate(20);
      
      // response including signature and challenge
      return res.status(201).send({
        success: true,
        message: 'verify the signature with the root public-key and sign the challange with the device private-key',
        signature: signatureArray,
        challenge: challenge
      });
      
            
    } else {
      
      console.log("Device not found");
            
      return res.status(404).send({
        success: false,
        message: 'Device not found'
      });
            
    }
          
  });
  
});


// ==================================
// Auth Challenge-Route =============
// ==================================
authRoutes.post('/challenge', function(req, res) {
  
  // Get request data
  var deviceId = req.body.deviceId; // String
  var signature = req.body.signature; // Array
  
  Device.findOne({deviceId: deviceId}, function(err, device) {
    if (err) throw err;
          
    if(device && !INVALID_DEVICE_ID) {
      
      // Create signature-buffer (split array in r and s)
      var length = signature.length;
      
      var r = signature.slice(0, length/2);
      var s = signature.slice(length/2);
      
      var signatureBuffer = serializeSig(r, s);
      
      // Get device public-key
      var keyBuffer = new Buffer(device.publicKey);
      var publicKey = ecdh.PublicKey.fromBuffer(curve, keyBuffer);
      
      // Create challange-buffer
      var challengeBuffer = new Buffer(challenge);
      
      // validate the signature with challenge and device public-key
      var isValid = publicKey.verifySignature(challengeBuffer, signatureBuffer);
      console.log('Challenge-Signature is', isValid ? 'valid' : 'invalid');
      
      if (isValid && ! INVALID_CHALLENGE_SIG) {
    
        // create token
        var token = jwt.sign({c: challenge}, app.get('token-secret'), {
          
          // expiresIn: 30 // 10s
          // expiresIn: "24h"   
          expiresIn: "30 days"
          
        });
        
        console.log("Token: " + token);

        // response including token
        return res.status(201).send({
          success: true,
          message: 'use this token for service requests',
          token: token
        });
        
        
      } else {
        
        return res.status(401).send({
          success: false,
          message: 'Authentication failed' });
      }
      
    
    } else {
      
        console.log("Device not found");
            
        return res.status(404).send({
          success: false,
          message: 'Device not found'
        });
            
      }
          
  });
    
});


// apply the routes to app with the prefix /service
app.use('/auth', authRoutes);

};


/**
 *  Creates a signature-buffer in DER-format
 *  @param {Array} r (r-values from signature)
 *  @param {Array} s (s-values from signature)
 *  @return {Buffer} buf (signature-buffer in DER-format)
 */
var serializeSig = function(r, s) {
    
    var DER_SEQUENCE = 0x30;
    var DER_INTEGER = 0x02;
    
    var rBa = new Buffer(r);
    var sBa = new Buffer(s);

    var buf = new Buffer(6 + rBa.length + sBa.length),
        end = buf.length - sBa.length;

    buf[0] = DER_SEQUENCE;
    buf[1] = buf.length - 2;

    buf[2] = DER_INTEGER;
    buf[3] = rBa.length;
    rBa.copy(buf, 4);

    buf[end-2] = DER_INTEGER;
    buf[end-1] = sBa.length;
    sBa.copy(buf, end);

    return buf;
};


/**
 *  Create buffer with raw signature
 *  @param {Buffer} buf (signature-buffer in DER-format)
 *  @return {Buffer} buf (buffer with raw signature)
 */
function deserializeSig(buf) {
  
  var DER_SEQUENCE = 0x30;
    var DER_INTEGER = 0x02;
    
    if(buf[0] !== DER_SEQUENCE)
        throw new Error("Signature is not a valid DERSequence");

    if(buf[1] > buf.length-2)
        throw new Error("Signature length is too short");

    if(buf[2] !== DER_INTEGER)
        throw new Error("First element in signature must be a DERInteger");

    var pos = 4,
        rBa = buf.slice(pos, pos+buf[3]);

    pos += rBa.length;
    if(buf[pos++] !== DER_INTEGER)
        throw new Error("Second element in signature must be a DERInteger");

    var sBa = buf.slice(pos+1, pos+1+buf[pos]);

    var length = rBa.length + sBa.length;
    return Buffer.concat([rBa, sBa], length);

}


/**
 *  Save a new device instance (or update if already exist)
 *  @param {String} deviceId
 *  @return {Array} publicKey
 */
var saveDevice = function(deviceId, publicKey){
  
  // check if device already exist
  Device.findOne({deviceId: deviceId}, function(err, device) {
    
    if (err) throw err;
          
    if(device) {
      console.log("Update existing device");
            
      device.publicKey = publicKey;
            
      device.save(function(err) {
        if (err) throw err;
            console.log('Device updated successfully');
        });
            
    } else {
        console.log("Create new device");
            
        var newDevice = new Device({
          deviceId: deviceId,
          publicKey: publicKey
        });
            
        newDevice.save(function(err) {
          if (err) throw err;
          console.log('Device saved successfully');
        });
            
     }
          
  });      
  
};