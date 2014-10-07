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

/** Register a callback to be notified when the closest beacon changes
*
* @param {config} config
* @param {successCallback} win
* @param {failCallback} fail
*
* @example
com.blueCats.BlueCatsSDKCDVPlugin.monitorClosestBeaconChange(
	{
        secondsBeforeExitBeacon:10.0,
        minimumTriggerIntervalInSeconds:5.0,
        filter:
        {
            sitesNamed:['My Site'],
            categoriesNamed:['Entrance'],
            minimumProximity:'BC_PROXIMITY_IMMEDIATE',
            maximumProximity:'BC_PROXIMITY_NEAR',
            minimumAccuracy:0.0,
            maximumAccuarcy:2.0
        }
    },
	function(eventData)
	{
		console.log('Closest beacon changed');
	},
	function(error)
	{
		console.log('Closest beacon failed' + error);
	}
);
*/
exports.monitorClosestBeaconChange = function(config, win, fail) {
	exec(win, fail, 'BlueCatsSDKCDVPlugin', 'monitorClosestBeaconChange', [config]);
};

/** Register a callback to be notified when entering range of beacon
*
* @param {config} config
* @param {successCallback} win
* @param {failCallback} fail
*
* @example
com.blueCats.BlueCatsSDKCDVPlugin.monitorEnterBeacon(
	{
        secondsBeforeExitBeacon:10.0,
        minimumTriggerIntervalInSeconds:5.0,
        filter:
        {
            sitesNamed:['My Site'],
            categoriesNamed:['Entrance'],
            minimumProximity:'BC_PROXIMITY_IMMEDIATE',
            maximumProximity:'BC_PROXIMITY_NEAR',
            minimumAccuracy:0.0,
            maximumAccuarcy:2.0
        }
    },
	function(eventData)
	{
		console.log('Entered beacon');
	},
	function(error)
	{
		console.log('Enter beacon failed' + error);
	}
);
*/
exports.monitorEnterBeacon = function(config, win, fail) {
	exec(win, fail, 'BlueCatsSDKCDVPlugin', 'monitorEnterBeacon', [config]);
};

/** Register a callback to be notified when exiting range of a beacon
*
* @param {config} config
* @param {successCallback} win
* @param {failCallback} fail
*
* @example
com.blueCats.BlueCatsSDKCDVPlugin.monitorExitBeacon(
	{
        secondsBeforeExitBeacon:10.0,
        minimumTriggerIntervalInSeconds:5.0,
        filter:
        {
            sitesNamed:['My Site'],
            categoriesNamed:['Entrance'],
            minimumProximity:'BC_PROXIMITY_IMMEDIATE',
            maximumProximity:'BC_PROXIMITY_NEAR',
            minimumAccuracy:0.0,
            maximumAccuarcy:2.0
        }
    },
	function(eventData)
	{
		console.log('Exited beacon');
	},
	function(error)
	{
		console.log('Exit beacon failed' + error);
	}
);
*/
exports.monitorExitBeacon = function(config, win, fail) {
	exec(win, fail, 'BlueCatsSDKCDVPlugin', 'monitorExitBeacon', [config]);
};