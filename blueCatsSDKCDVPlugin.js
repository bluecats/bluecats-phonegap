// API definition for BlueCats SDK Cordova plugin.
//
// Use jsdoc to generate documentation.

// The following line causes a jsdoc error.
// Use the jsdoc option -l to ignore the error.
var exec = cordova.require('cordova/exec'),
	utils = require("cordova/utils");

/** @module com.bluecats.beacons */

/**
     * Starts monitoring for BlueCats beacons
     *
     * @param String appToken              BlueCats appToken
     * @param {Function} success           The function to call when SDK is started. (OPTIONAL)
     * @param {Function} fail              The function to call when there is an error. (OPTIONAL)
     * @param {SDKOptions} options         Configuration options for the BlueCats SDK (OPTIONAL)
     */
exports.startPurringWithAppToken = function(appToken, success, fail, options) {
	exec(success, fail, 'BlueCatsSDKCDVPlugin', 'startPurringWithAppToken', [appToken, options]);
};

/**
     * Asynchronously notifies beacons within range
     *
     * @param {Function} success           The function to call each time new beacons are discovered
     * @param {Function} fail              The function to call when there is an error. (OPTIONAL)
     * @param {WatchBeaconOptions} options The options for watching beacons (OPTIONAL)
     * @return String                      The watch id that must be passed to #clearWatch to stop watching.
     */
exports.watchMicroLocation = function(success, fail, options){
	var watchId = utils.createUUID();
	exec(success, fail, 'BlueCatsSDKCDVPlugin', 'monitorMicroLocation', [watchId, options]);
	return watchId;
};

/**
     * Asynchronously notifies entering range of beacons
     *
     * @param {Function} success           The function to call each time new beacons are discovered
     * @param {Function} fail              The function to call when there is an error. (OPTIONAL)
     * @param {WatchBeaconOptions} options The options for watching beacons (OPTIONAL)
     * @return String                      The watch id that must be passed to #clearWatch to stop watching.
     */
exports.watchClosestBeaconChange = function(success, fail, options) {
	var watchId = utils.createUUID();
	exec(success, fail, 'BlueCatsSDKCDVPlugin', 'monitorClosestBeaconChange', [watchId, options]);
	return watchId;
};

/**
     * Asynchronously notifies entering range of beacons
     *
     * @param {Function} success           The function to call each time new beacons are discovered
     * @param {Function} fail              The function to call when there is an error. (OPTIONAL)
     * @param {WatchBeaconOptions} options The options for watching beacons (OPTIONAL)
     * @return String                      The watch id that must be passed to #clearWatch to stop watching.
     */
exports.watchEnterBeacon = function(success, fail, options) {
	var watchId = utils.createUUID();
	exec(success, fail, 'BlueCatsSDKCDVPlugin', 'monitorEnterBeacon', [watchId, options]);
	return watchId;
};

/**
     * Asynchronously notifies exiting range of beacons
     *
     * @param {Function} success           The function to call each time new beacons are discovered
     * @param {Function} fail              The function to call when there is an error. (OPTIONAL)
     * @param {WatchBeaconOptions} options The options for watching beacons (OPTIONAL)
     * @return String                      The watch id that must be passed to #clearWatch to stop watching.
     */
exports.watchExitBeacon = function(success, fail, options) {
	var watchId = utils.createUUID();
	exec(success, fail, 'BlueCatsSDKCDVPlugin', 'monitorExitBeacon', [watchId, options]);
	return watchId;
};

/**
     * Clear the watched beacon event
     *
     * @param String watchId      The watchId to clear
     * @param {Function} success  The function to call when the watched callback is cancelled (OPTIONAL)
     * @param {Function} fail     The function to call when there is an error. (OPTIONAL)
     */
exports.clearWatch = function(watchId, success, fail) {
	exec(success, fail, 'BlueCatsSDKCDVPlugin', 'removeMonitoredEvent', [watchId]);
};

/**
     * Calls success function when a local notification is opened
     *
     * @param {Function} success  The function to call when the watched callback is cancelled (OPTIONAL)
     * @param {Function} fail     The function to call when there is an error. (OPTIONAL)
     */
exports.localNotificationReceived = function(success, fail) {
	exec(success, fail, 'BlueCatsSDKCDVPlugin', 'registerLocalNotificationReceivedCallback', []);
};

/** Schedule a local notification to be triggered by a beacon.
*
* @param {localNotification} localNotification
* @param {successCallback} success
* @param {failCallback} fail
*
*/
exports.scheduleLocalNotification = function(localNotification, success, fail) {
	exec(success, fail, 'BlueCatsSDKCDVPlugin', 'scheduleLocalNotification', [localNotification]);
};

/** Cancel all scheduled beacon local notifications
*
* @param {successCallback} success
* @param {failCallback} fail
*
*/
exports.cancelAllLocalNotifications = function(success, fail) {
	exec(success, fail, 'BlueCatsSDKCDVPlugin', 'cancelAllLocalNotifications', []);
};
