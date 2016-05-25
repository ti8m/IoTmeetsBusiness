var express = require('express');
var app = express();

require('./router/main')(app);

// https://www.npmjs.com/package/ecdh
var crypto = require('crypto');
ecdh = require('./node_modules/ecdh/index');


// use address 0.0.0.0 for access from inside the network (localhost is 127.0.0.1)
var server = app.listen(8080,'0.0.0.0', function () {

    var host = server.address().address
    var port = server.address().port



    console.log("Example app listening at http://%s:%s", host, port)

    // Pick some curve
    var curve = ecdh.getCurve('secp256k1');

    // Choose algorithm to the hash function
    var algorithm = 'sha256';

    // Generate random keys for Alice
    var aliceKeys = ecdh.generateKeys(curve);

    console.log("Private Key: " + aliceKeys.privateKey.buffer.toString('hex'));
    console.log("Public Key: " + aliceKeys.publicKey.buffer.toString('hex'));


// Hash something so we can have a digest to sign
    var message = new Buffer('Hello World');
    var  hash = crypto.createHash(algorithm).update(message).digest();
    console.log('Hashed message to sign:', hash.toString('hex'));


// Sign it with Alice's key
    var signature = aliceKeys.privateKey.sign(hash, algorithm);
    console.log('Signature:', signature.toString('hex'));


// Verify it with Alice public key
    var valid = aliceKeys.publicKey.verifySignature(hash, signature);
    console.log('Signature is', valid ? 'valid :)' : 'invalid!!');






    // Public-Key mittels micro-ecc generiert
    var keyBuffer = new Buffer("b325028165169438fc8ebfbe3096fff977efdf9393ad055ac134c7e364dd8fe1c6f5d051b26467d0c47b2dbd978cf32931c658b2b8839cd6d1d5574d8afc7f3b", "hex");
    var rootPublicKey = ecdh.PublicKey.fromBuffer(curve, keyBuffer)
    console.log("Root Public Key: " + rootPublicKey.buffer.toString('hex'));

    // Signatur mittels micro-ecc generiert ("Hello World" signiert ohne Hash) aufteilen in r und s (je 32 Byte)
    // serializeSig() erstellt einen Hex-String im DER-Format
    var signatureBuffer = serializeSig("673e2002cb7e6b0f48d326080099f991c913f8529cfcf9569786147baf281320", "68fa9350ec5adfe52ee4217fa53435085dae356f601758b2c1785ebf2749fc6e");

    var signedMessage = new Buffer('Hello World');

    var isValid = rootPublicKey.verifySignature(signedMessage, signatureBuffer);
    console.log('MicroEcc-Signature is', isValid ? 'valid :)' : 'invalid!!');


})


var DER_SEQUENCE = 0x30,
    DER_INTEGER = 0x02;

function serializeSig(r, s) {
    var rBa = new Buffer(r, 'hex');
    var sBa = new Buffer(s, 'hex');

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