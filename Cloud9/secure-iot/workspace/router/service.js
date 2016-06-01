var express = require('express');
var Device = require('../models/device');
var jwt = require('jsonwebtoken');

module.exports = function(app) {

  // get an instance of the router for api routes
  var serviceRoutes = express.Router();


  // ========================================
  // Route middleware to verify a token =====
  // ========================================

  serviceRoutes.use(function(req, res, next) {

    // check header or url parameters or post parameters for token
    var token = req.body.token || req.query.token || req.headers['x-access-token'];

    // decode token
    if (token) {

      // verifies secret and checks exp
      jwt.verify(token, app.get('token-secret'), function(err, decoded) {
        if (err) {
          return res.status(403).send({
            success: false,
            message: 'Invalid token'
          });
          
        }
        else {
          // if everything is good, save to request for use in other routes
          req.decoded = decoded;
          next();
        }
      });

    }
    else {
      
      return res.status(403).send({
        success: false,
        message: 'No token provided.'
      });

    }
  });



  // ==========================================
  // Service Auth-Route (need a token) ========
  // ==========================================

  serviceRoutes.post('/data', function(req, res) {
    
    // var deviceId = "1234";
    var deviceId = req.body.deviceId;
    var timestamp = new Date(); // UTC time

    // find device by id
    Device.findOne({deviceId: deviceId}, function(err, device) {
          if (err) throw err;
          
          if(device) {
            
            console.log("Save data from device " + deviceId);
            console.log(req.body.temp + "°C");
            console.log(req.body.humid + "%");
            console.log(timestamp);
            
            var data = {
              temp: req.body.temp,
              humid: req.body.humid,
              timestamp: timestamp
            };
            
            device.data.push(data);
            
            device.save(function(err) {
              if (err) throw err;
              console.log('Data saved successfully');
              
              return res.status(201).send({
              success: true,
              message: 'New data saved'
              });
            
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


  // apply the routes to app with the prefix /service
  app.use('/service', serviceRoutes);

};