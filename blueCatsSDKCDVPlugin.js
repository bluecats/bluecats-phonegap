// API definition for BlueCats SDK Cordova plugin.
//
// Use jsdoc to generate documentation.

// The following line causes a jsdoc error.
// Use the jsdoc option -l to ignore the error.
var exec = cordova.require('cordova/exec');

/** @module com.blueCats.BlueCatsSDKCDVPlugin */

/** Starts monitoring of BlueCats beacons.
*
* @param {appToken} blueCats SDK app token. See http://www.bluecats.com for info.
* @param {successCallback} win
* @param {failCallback} fail
*
* @example
com.blueCats.BlueCatsSDKCDVPlugin.startPurringWithAppToken(
	function()
	{
		console.log('BlueCats is purring...');
	},
	function(errorCode)
	{
		console.log('BlueCats won't purr: ' + errorCode);
	}
);
*/
exports.startPurringWithAppToken = function(appToken, win, fail) {
	exec(win, fail, 'BlueCatsSDKCDVPlugin', 'startPurringWithAppToken', [appToken]);
};

/** Register callback for info about ranged beacons.
*
* @param {microLocationUpdateCallback} win
* @param {failCallback} fail
*
* @example
com.blueCats.BlueCatsSDKCDVPlugin.startUpdatingMicroLocation(
	function(microLocation)
	{
		console.log('MicroLocation was updated: ' + JSON.stringify(microLocation));
	},
	function(errorCode)
	{
		console.log('MicroLocation update failed ' + errorCode);
	}
);
*/
exports.startUpdatingMicroLocation = function(win, fail) {
	exec(win, fail, 'BlueCatsSDKCDVPlugin', 'startUpdatingMicroLocation', []);
};

/** Cancel callback for ranged beacons.
*
* @param {successCallback} win
* @param {failCallback} fail
*
* @example
com.blueCats.BlueCatsSDKCDVPlugin.stopUpdatingMicroLocation(
	function()
	{
		console.log('MicroLocation updates stopped');
	},
	function(errorCode)
	{
		console.log('MicroLocation update failed ' + errorCode);
	}
);
*/
exports.stopUpdatingMicroLocation = function(win, fail) {
	exec(win, fail, 'BlueCatsSDKCDVPlugin', 'stopUpdatingMicroLocation', []);
};

/** Register a callback to handle local notifications
*
* @param {successCallback} function to be called when a local notification is received
* @param {failCallback} fail
*
* @example
com.blueCats.BlueCatsSDKCDVPlugin.registerLocalNotificationReceivedCallback(
	function(localNotifictionUserInfo)
	{
		console.log('Local notification received');
	},
	function(errorCode)
	{
		console.log('Local notification schedule failed ' + errorCode);
	}
);
*/
exports.registerLocalNotificationReceivedCallback = function(win, fail) {
	exec(win, fail, 'BlueCatsSDKCDVPlugin', 'registerLocalNotificationReceivedCallback', []);
};

/** Schedule a local notification to be triggered by a beacon.
*
* @param {localNotification} localNotification
* @param {successCallback} win
* @param {failCallback} fail
*
* @example
com.blueCats.BlueCatsSDKCDVPlugin.scheduleLocalNotification(
	{
        fireInCategories : [{ name : 'MyTriggerCategory' }],
        fireAfterDelayInSeconds : 5,
        alertAction : 'Test Action',
        alertBody : 'This is my phonegap notification'
    },
	function()
	{
		console.log('Local notification scheduled');
	},
	function(errorCode)
	{
		console.log('Local notification schedule failed ' + errorCode);
	}
);
*/
exports.scheduleLocalNotification = function(localNotification, win, fail) {
	exec(win, fail, 'BlueCatsSDKCDVPlugin', 'scheduleLocalNotification', [localNotification]);
};

/** Cancel all scheduled beacon local notifications
*
* @param {successCallback} win
* @param {failCallback} fail
*
* @example
com.blueCats.BlueCatsSDKCDVPlugin.cancelAllLocalNotifications(
	function()
	{
		console.log('All beacon local notifications cancelled');
	},
	function(errorCode)
	{
		console.log('Local notification cancel failed ' + errorCode);
	}
);
*/
exports.cancelAllLocalNotifications = function(win, fail) {
	exec(win, fail, 'BlueCatsSDKCDVPlugin', 'cancelAllLocalNotifications', []);
};
