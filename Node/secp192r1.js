
// https://www.npmjs.com/package/ecdh
var crypto = require('crypto');
ecdh = require('./node_modules/ecdh/index');



var DER_SEQUENCE = 0x30,
    DER_INTEGER = 0x02;

var serializeSig = function(r, s) {
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

var serializeSig2 = function(r, s) {
    var rBa = new Buffer(r);
    var sBa = new Buffer(s);

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




// Pick some curve
var curve = ecdh.getCurve('secp192r1');

// Choose algorithm to the hash function
var algorithm = 'sha1';

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






// Root Public-Key mittels micro-ecc generiert
var keyBuffer = new Buffer("005a142baee297105658b624c7a4a89937f60d9ed6a64196cec69a7ea147d893b90358532c4db2e26b1318f8cecf4eed", "hex");
var rootPublicKey = ecdh.PublicKey.fromBuffer(curve, keyBuffer)
console.log("Root Public Key: " + rootPublicKey.buffer.toString('hex'));

// Signatur mittels micro-ecc generiert ("Hello World" signiert ohne Hash) aufteilen in r und s (je 24 Byte)
// serializeSig() erstellt einen Hex-String im DER-Format

//var signatureBuffer = serializeSig("103aa4b5b267b264c519f04831034dc109caecf3f1923ec1", "63c581342a6127af072fc5eba77eb029af67e83979785c56");
var signatureBuffer = serializeSig2([204, 21, 195, 247, 211, 44, 84, 196, 153, 190, 70, 111, 100, 243, 229, 211, 194, 196, 107, 59, 205, 108, 147, 64], [181, 206, 160, 241, 85, 5, 205, 49, 74, 231, 142, 13, 123, 23, 153, 46, 251, 48, 161, 102, 216, 133, 231, 96]);

var messageBuffer = new Buffer('18fe34a37794'); // device MAC
//var devicePublicBuffer = new Buffer("022a6cc9cd579c7ba4eeaa9bc819b6fcb4c2df88f6d3f280419e820495a0a0469c64f98cc550521727be94dd77fa655a", "hex");
var devicePublicBuffer = new Buffer([92, 90, 29, 254, 35, 62, 64, 211, 75, 121, 167, 52, 129, 43, 13, 114, 31, 103, 116, 28, 219, 177, 0, 91, 242, 231, 109, 172, 11, 99, 101, 116, 232, 54, 0, 9, 79, 241, 251, 179, 254, 40, 132, 69, 50, 81, 99, 255]);

var bufLength = devicePublicBuffer.length + messageBuffer.length;

var dataBuffer = Buffer.concat([devicePublicBuffer, messageBuffer], bufLength);

// Hash device public-key & message (data)
var  dataHash = crypto.createHash(algorithm).update(dataBuffer).digest();

var isValid = rootPublicKey.verifySignature(dataHash, signatureBuffer);
console.log('MicroEcc-Signature is', isValid ? 'valid :)' : 'invalid!!');




