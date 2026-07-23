const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.helloWorld = functions.https.onRequest((req, res) => {
  res.json({ message: "Hello from CYRYEL Cloud Functions!" });
});
