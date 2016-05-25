
var express = require('express');
var Device  = require('../models/device');

module.exports = function(app) {
    

// =======================
// API Routes ============
// =======================

// get an instance of the router for api routes
var apiRoutes = express.Router();

// route to show a random message (GET http://localhost:8080/api/)
apiRoutes.get('/', function(req, res) {
  res.json({ message: 'Secure IoT api url' });
});

// route to return all devices (GET http://localhost:8080/api/devices)
apiRoutes.get('/devices', function(req, res) {
  Device.find({}, function(err, devices) {
    res.json(devices);
  });
});   

// apply the routes to app with the prefix /api
app.use('/api', apiRoutes);
}