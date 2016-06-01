module.exports = {

    'tokenSecret': '8JNN1VnhSHMl0CbjBgiK3LgZJyJD4VNnnQ6ri58DWoH73pssTA',
    'rootPublicKey': '005a142baee297105658b624c7a4a89937f60d9ed6a64196cec69a7ea147d893b90358532c4db2e26b1318f8cecf4eed',
    'database': "mongodb://"+process.env.IP+":27017/secureIoT",
    'mqttBroker': 'mqtt://m21.cloudmqtt.com',
    'mqttOptions': {username: "secureIoT", password:"admin", port: 18188, clientId: "" }

};