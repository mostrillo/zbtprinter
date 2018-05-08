var exec = require('cordova/exec');

exports.print = function(str, mac, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraBluetoothPrinter', 'print', [str,mac]);
};

exports.file = function(str, mac, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraBluetoothPrinter', 'file', [str,mac]);
};

exports.image = function(base64, mac, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraBluetoothPrinter', 'image', [base64, mac]);
};

