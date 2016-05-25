
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




// Pick some curve
var curve = ecdh.getCurve('secp160r1');

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






// Public-Key mittels micro-ecc generiert
var keyBuffer = new Buffer("835a14191a5d29575aba0deb59f988b3521865f50015fe5a4a0649001064bf0a6abd99df12e3c49e", "hex");
var rootPublicKey = ecdh.PublicKey.fromBuffer(curve, keyBuffer)
console.log("Root Public Key: " + rootPublicKey.buffer.toString('hex'));

// Signatur mittels micro-ecc generiert ("Hello World" signiert ohne Hash) aufteilen in r und s (je 24 Byte)
// serializeSig() erstellt einen Hex-String im DER-Format
//
var signatureBuffer = serializeSig("46782da235b92184b51c3c64aa8f597cb38471e2", "43563b86 b7bc50a7d18936cb393f446b94c0890c");

var messageBuffer = new Buffer('Hello');

var bufLength = keyBuffer.length + messageBuffer.length;

var signedMessage = Buffer.concat([messageBuffer, keyBuffer], bufLength);

var testBuffer = new Buffer("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", "hex");

var isValid = rootPublicKey.verifySignature(messageBuffer, signatureBuffer);
console.log('MicroEcc-Signature is', isValid ? 'valid :)' : 'invalid!!');



