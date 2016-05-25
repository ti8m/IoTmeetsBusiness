var KeyEncoder = require('key-encoder')
var keyEncoder = new KeyEncoder('secp256k1')


var EC = require('elliptic').ec;

// Create and initialize EC context
// (better do it once and reuse it)
var ec = new EC('secp256k1');

var msg = "Hello World";

var keys = ec.genKeyPair();
var pub = keys.getPublic('hex');
//console.log(pub)
var copy = ec.keyFromPublic(pub, 'hex');
//console.log(copy.getPublic('hex'))

var signature1 = ec.sign(msg, keys);
console.log(signature1)
var dsign = signature1.toDER('hex');
console.log(dsign)



// CHECK WITH NO PRIVATE KEY

// Add 04 at the beginning of micro-ecc key!
var pub = '04b325028165169438fc8ebfbe3096fff977efdf9393ad055ac134c7e364dd8fe1c6f5d051b26467d0c47b2dbd978cf32931c658b2b8839cd6d1d5574d8afc7f3b';

//// Import public key
var key = ec.keyFromPublic(pub, 'hex');


//// Signature MUST be either:
//// 1) hex-string of DER-encoded signature; or
//// 2) DER-encoded signature as buffer; or
//// 3) object with two hex-string properties (r and s)
//
var rawSignature = '673e2002cb7e6b0f48d326080099f991c913f8529cfcf9569786147baf281320 68fa9350ec5adfe52ee4217fa53435085dae356f601758b2c1785ebf2749fc6e';
//var signature = { r: 'b1fc...', s: '9c42...' }; // case 3

//var derSignature = "3045022100eb904c22cdb596f89a081b089aeddc73c0de94839fcd999643960e0198ead7020220305a62794cc5fe80cfe1cc68729105ff7b3fddedde79371517d616823dbb268f"

var rs = {r:"673e2002cb7e6b0f48d326080099f991c913f8529cfcf9569786147baf281320" , s:"68fa9350ec5adfe52ee4217fa53435085dae356f601758b2c1785ebf2749fc6e"};


//// Verify signature
console.log(key.verify(msg, rs));

