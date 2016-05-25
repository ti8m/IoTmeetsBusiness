
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
var devicePublicKey;

module.exports = function(app) {
    

// get an instance of the router for auth routes
var authRoutes = express.Router();

authRoutes.get('/', function(req, res) {
  res.json({ message: 'Secure IoT auth url' });
});


// ==================================
// Auth-Key Route ===================
// ==================================
authRoutes.post('/key', function(req, res) {
  
  // Get request data
  var deviceId = req.body.deviceId; // String
  devicePublicKey = req.body.publicKey; // Array
  var signature = req.body.signature; // Array
  
  // Get root public-key
  var keyBuffer = new Buffer(app.get('root-public-key'), "hex");
  var rootPublicKey = ecdh.PublicKey.fromBuffer(curve, keyBuffer);
  
  // Create signature-buffer (split array in r and s)
  var length = signature.length;
  
  var r = signature.slice(0, length/2);
  var s = signature.slice(length/2);
  
  var signatureBuffer = serializeSig(r, s);
  
  // Create buffer from device-id
  var devicIdBuffer = new Buffer(deviceId);
  
  // Create buffer from device public-key
  var devicePublicBuffer = new Buffer(devicePublicKey);
  
  // Create data-buffer to hash
  var bufLength = devicePublicBuffer.length + devicIdBuffer.length;
  var dataBuffer = Buffer.concat([devicePublicBuffer, devicIdBuffer], bufLength);
  
  // Hash device public-key & device-id (data)
  var dataHash = crypto.createHash(algorithm).update(dataBuffer).digest();
  
  // validate signature with the data-hash and the root public-key
  var isValid = rootPublicKey.verifySignature(dataHash, signatureBuffer);
  console.log('Key-Signature is', isValid ? 'valid' : 'invalid');
  
  if (isValid) {
        
        challenge = random.generate(20);
        
        res.json({
          success: true,
          message: 'sign the challenge with your device private-key',
          challenge: challenge
        });
        
      } else {
        
        res.json({ success: false, message: 'Authentication failed' });
      }

  
});



// ==================================
// Auth-Challenge Route =============
// ==================================
authRoutes.post('/challenge', function(req, res) {
  
  // Get request data
  var deviceId = req.body.deviceId; // String
  var signature = req.body.signature; // Array
  
  // Create signature-buffer (split array in r and s)
  var length = signature.length;
  
  var r = signature.slice(0, length/2);
  var s = signature.slice(length/2);
  
  var signatureBuffer = serializeSig(r, s);
  
  // Get device public-key
  var keyBuffer = new Buffer(devicePublicKey);
  var publicKey = ecdh.PublicKey.fromBuffer(curve, keyBuffer);
  
  // Create challange-buffer
  var challengeBuffer = new Buffer(challenge);
  
  // validate the signature with challenge and device public-key
  var isValid = publicKey.verifySignature(challengeBuffer, signatureBuffer);
  console.log('Challenge-Signature is', isValid ? 'valid' : 'invalid');
  
  
  if (isValid) {
    
        // create token
        var token = jwt.sign({c: challenge}, app.get('token-secret'), {
          expiresIn: "24h"  // or  "2 days" 
        });

        // response including token
        res.json({
          success: true,
          message: 'use this token for service requests',
          token: token
        });
        
        // check if device already exist
        Device.findOne({deviceId: deviceId}, function(err, device) {
          if (err) throw err;
          
          if(device) {
            console.log("Update existing device");
            
            device.publicKey = devicePublicKey;
            
            device.save(function(err) {
              if (err) throw err;
              console.log('Device updated successfully');
            });
            
          } else {
            console.log("Create new device");
            
            var newDevice = new Device({
              deviceId: deviceId,
              publicKey: devicePublicKey
            });
            
            newDevice.save(function(err) {
              if (err) throw err;
              console.log('Device saved successfully');
            });
            
          }
          
        });
          
        
      } else {
        
        res.json({ success: false, message: 'Authentication failed' });
      }

  
});


// apply the routes to app with the prefix /service
app.use('/auth', authRoutes);

};


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
}