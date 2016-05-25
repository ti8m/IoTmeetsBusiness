
var express = require('express');
var Device  = require('../models/device');
var jwt    = require('jsonwebtoken'); 

module.exports = function(app) {
    

// ===========================
// Service Routes ============
// ===========================

// get an instance of the router for api routes
var serviceRoutes = express.Router();


// route middleware to verify a token
serviceRoutes.use(function(req, res, next) {

  // check header or url parameters or post parameters for token
  var token = req.body.token || req.query.token || req.headers['x-access-token'];

  // decode token
  if (token) {

    // verifies secret and checks exp
    jwt.verify(token, app.get('token-secret'), function(err, decoded) {      
      if (err) {
        return res.json({ success: false, message: 'Failed to authenticate token.' });    
      } else {
        // if everything is good, save to request for use in other routes
        req.decoded = decoded;    
        next();
      }
    });

  } else {

    // if there is no token
    // return an error
    return res.status(403).send({ 
        success: false, 
        message: 'No token provided.' 
    });
    
  }
});

// this route need a token now!
serviceRoutes.post('/', function(req, res) {
  res.json({ message: 'Secure IoT service url' });
});



// apply the routes to app with the prefix /service
app.use('/service', serviceRoutes);

}

