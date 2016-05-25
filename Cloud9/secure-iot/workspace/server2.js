// https://www.npmjs.com/package/ecdh
// https://www.npmjs.com/package/mqtt

var http = require('http');
var path = require('path');

var express = require('express');
var bodyParser = require("body-parser");
var app = express();

app.use(bodyParser.urlencoded({extended: false}));
app.use(bodyParser.json());

require('./router/main')(app);



var crypto = require('crypto');
var ecdh = require('./node_modules/ecdh/index');

var mqtt = require('mqtt');



var server = app.listen(process.env.PORT || 3000, process.env.IP || "0.0.0.0", function() {

    var addr = server.address();
    console.log("server listening at", addr.address + ":" + addr.port);




})
