// https://www.npmjs.com/package/ecdh
// https://www.npmjs.com/package/mqtt

var express     = require('express');
var bodyParser  = require('body-parser');
var mongoose    = require('mongoose');
var config      = require('./config'); 

var app  = express();

app.set('token-secret', config.tokenSecret);
app.set('root-private-key', config.rootPrivateKey);
app.set('root-public-key', config.rootPublicKey);
app.set('mqtt-broker', config.mqttBroker);
app.set('mqtt-options', config.mqttOptions);

app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

require('./router/auth')(app);
require('./router/service')(app);

mongoose.connect(config.database); 

 var server = app.listen(process.env.PORT || 3000, process.env.IP || "0.0.0.0", function() {

    var addr = server.address();
    console.log("server listening at", addr.address + ":" + addr.port);

});


