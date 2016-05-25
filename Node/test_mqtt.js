
// https://www.npmjs.com/package/mqtt

var mqtt    = require('mqtt');


var id = 'mqttjs_' + Math.random().toString(16).substr(2, 8);
var options = {username: "secureIoT", password:"admin", port: 18188, clientId: id }

var client  = mqtt.connect('mqtt://m21.cloudmqtt.com', options);


client.on('connect', function () {
    console.log("Connected")
    client.subscribe('secureIoT/#');
    client.publish('secureIoT', 'Hello mqtt');
});

client.on('message', function (topic, message) {
    // message is Buffer
    console.log(message.toString());
    //client.end();
});
