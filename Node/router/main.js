
// https://www.npmjs.com/package/ecdh
var crypto = require('crypto');
ecdh = require('../node_modules/ecdh/index');

module.exports = function (app) {
    app.get('/', function (req, res) {
        console.log("Got a GET request for the homepage");


        var curve = ecdh.getCurve('secp192r1');

// Choose algorithm to the hash function
        var algorithm = 'sha256';

// Generate random keys for Alice
        var aliceKeys = ecdh.generateKeys(curve);

        console.log("Private Key: " + aliceKeys.privateKey.buffer.toString('hex'));
        console.log("Public Key: " + aliceKeys.publicKey.buffer.toString('hex'));


// Hash something so we can have a digest to sign
        var message = new Buffer('Hello World');
        var  hash = crypto.createHash(algorithm).update(message).digest();

// Sign it with Alice's key
        var signature = aliceKeys.privateKey.sign(hash, algorithm);
        console.log('Signature:', signature.toString('hex'));


        res.send('Signature:' + signature.toString('hex'));

    })


// This responds a POST request for the homepage
    app.post('/', function (req, res) {
        console.log("Got a POST request for the homepage");
        res.send('Hello POST');
    })

// This responds a DELETE request for the /del_user page.
    app.delete('/del_user', function (req, res) {
        console.log("Got a DELETE request for /del_user");
        res.send('Hello DELETE');
    })

// This responds a GET request for the /list_user page.
    app.get('/list_user', function (req, res) {
        console.log("Got a GET request for /list_user");
        res.send('Page Listing');
    })

// This responds a GET request for abcd, abxcd, ab123cd, and so on
    app.get('/ab*cd', function (req, res) {
        console.log("Got a GET request for /ab*cd");
        res.send('Page Pattern Match');
    })
}
