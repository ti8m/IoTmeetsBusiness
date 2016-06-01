
var express = require('express');
var Device  = require('../models/device');

var id;

module.exports = function(app) {
    
    app.get('/', function(req, res) {
    res.send('Secure IoT');
});

 app.get('/id', function(req, res) {
    res.send(id);
});

app.get('/setup', function(req, res) {

  // create a sample device
  var testDevice = new Device({ 
    deviceId: '12345',
  });

  // save the sample device
  testDevice.save(function(err) {
    if (err) throw err;

    console.log('Device saved successfully');
    res.json({ success: true });
  });
});

// this route need a token now!
app.post('/', function(req, res) {
  
  console.log(req.body.deviceId);
  console.log(req.body.publicKey);
  console.log(req.body.signature);
  
  id = req.body.deviceId;
  
  res.json({ message: 'Secure IoT  url' });
});

}
