package com.blueCats.BlueCatsSDKCDVPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.bluecats.sdk.BCBeacon;
import com.bluecats.sdk.BCCategory;
import com.bluecats.sdk.BCLocalNotification;
import com.bluecats.sdk.BCLocalNotificationManager;
import com.bluecats.sdk.BCMicroLocation;
import com.bluecats.sdk.BCMicroLocationManager;
import com.bluecats.sdk.BCSite;
import com.bluecats.sdk.BlueCatsSDK;
import com.bluecats.sdk.IBlueCatsSDKCallback;
import com.bluecats.sdk.BCBeacon.BCProximity;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BlueCatsSDKCDVPlugin extends CordovaPlugin {
	private static String TAG = "BlueCatsSDKCDVPlugin";

	public static final String ACTION_START_PURRING_WITH_APP_TOKEN = "startPurringWithAppToken";
	public static final String ACTION_START_UPDATING_MICRO_LOCATION = "startUpdatingMicroLocation";
	public static final String ACTION_STOP_UPDATING_MICRO_LOCATION = "stopUpdatingMicroLocation";
	public static final String ACTION_REGISTER_LOCAL_NOTIFICATION_RECEIVED_CALLBACK = "registerLocalNotificationReceivedCallback";
	public static final String ACTION_SCHEDULE_LOCAL_NOTIFICATION = "scheduleLocalNotification";
	public static final String ACTION_CANCEL_ALL_LOCAL_NOTIFICATIONS = "cancelAllLocalNotifications";

	private Activity mCordovaActivity;
	private static CallbackContext mCallbackContext;
	private static CallbackContext mLocalNotificationReceivedCallbackContext;
	private int mNotificationCounter = 0;

	private Gson mGson;
	private Gson getGson() {
		if (mGson == null) {
			mGson = new Gson();
		}
		return mGson;
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		mCordovaActivity = this.cordova.getActivity();

		Log.d(TAG, "actvity = " + mCordovaActivity.getLocalClassName());

		try {
			if (action.equalsIgnoreCase(ACTION_START_PURRING_WITH_APP_TOKEN)) {
				Log.d(TAG, action);

				String appToken = args.getString(0);
				Log.d(TAG, "appToken = " + appToken);

				BlueCatsSDK.startPurringWithAppToken(mCordovaActivity.getApplicationContext(), appToken);

				sendOkClearCallback(callbackContext);

				return true;
			} else if (action.equalsIgnoreCase(ACTION_START_UPDATING_MICRO_LOCATION)) {
				Log.d(TAG, action);

				mCallbackContext = callbackContext;

				BCMicroLocationManager.getInstance().startUpdatingMicroLocation(mBlueCatsSDKCallback);

				return true;
			} else if (action.equalsIgnoreCase(ACTION_STOP_UPDATING_MICRO_LOCATION)) {
				Log.d(TAG, action);

				BCMicroLocationManager.getInstance().stopUpdatingMicroLocation(mBlueCatsSDKCallback); 

				if (mCallbackContext != null) {
					sendNoResultClearCallback(mCallbackContext);

					mCallbackContext = null;
				}

				return true;
			} else if (action.equalsIgnoreCase(ACTION_REGISTER_LOCAL_NOTIFICATION_RECEIVED_CALLBACK)) {
				Log.d(TAG, action);

				mLocalNotificationReceivedCallbackContext = callbackContext;

				return true;
			} else if (action.equalsIgnoreCase(ACTION_SCHEDULE_LOCAL_NOTIFICATION)) {
				Log.d(TAG, action);

				// update the notification id counter
				mNotificationCounter++;

				JsonElement jElement = new JsonParser().parse(args.getJSONObject(0).toString());
				JsonObject jObject = jElement.getAsJsonObject();

				long fireAfterDelayInSeconds = 0;
				if (jObject.get("fireAfterDelayInSeconds") != null) {
					fireAfterDelayInSeconds = Long.parseLong(jObject.get("fireAfterDelayInSeconds").getAsString());
				}

				BCSite fireInSite = null;
				if (jObject.get("fireInSite") != null) {
					fireInSite = getGson().fromJson(jObject.get("fireInSite"), BCSite.class);
				}

				String fireInProximity = BCProximity.BC_PROXIMITY_UNKNOWN.toString();
				if (jObject.get("fireInProximity") != null) {
					fireInProximity = jObject.get("fireInProximity").getAsString();
				}

				String alertContentTitle = "";
				if (jObject.get("alertAction") != null) {
					alertContentTitle = jObject.get("alertAction").getAsString();
				}

				String alertContentText = "";
				if (jObject.get("alertBody") != null) {
					alertContentText = jObject.get("alertBody").getAsString();
				}

				List<BCCategory> categories = new ArrayList<BCCategory>();
				if (jObject.getAsJsonArray("fireInCategories") != null) {
					JsonArray jArray = jObject.getAsJsonArray("fireInCategories");
					categories = Arrays.asList(getGson().fromJson(jArray.getAsJsonArray(), BCCategory[].class));
				}

				Bundle userInfoBundle = new Bundle();
				if (jObject.get("userInfo") != null) {
					Map<String, String> userInfo = getGson().fromJson(jObject.get("userInfo").toString(), HashMap.class);
					for (Entry<String, String> entry: userInfo.entrySet()) {
						userInfoBundle.putString(entry.getKey(), entry.getValue());
					}
				}

				// pack up the notification contents
				Bundle bundle = new Bundle();
				bundle.putString("alertAction", alertContentTitle);
				bundle.putString("alertBody", alertContentText);
				bundle.putBundle("userInfo", userInfoBundle);

				Intent contentIntent = new Intent(mCordovaActivity, BlueCatsSDKCDVPluginLocalNotificationReveiverActivity.class);
				contentIntent.putExtras(bundle);

				BCLocalNotification localNotification = new BCLocalNotification(mNotificationCounter);
				if (fireInSite != null) {
					localNotification.setFireInSite(fireInSite);
				}
				localNotification.setFireAfter(new Date(new Date().getTime() + (fireAfterDelayInSeconds * 1000)));
				localNotification.setFireInCategories(categories);
				localNotification.setFireInProximity(BCBeacon.getProximityFromString(fireInProximity));
				localNotification.setAlertContentTitle(alertContentTitle);
				localNotification.setAlertContentText(alertContentText);
				localNotification.setContentIntent(contentIntent);

				BCLocalNotificationManager.getInstance().scheduleLocalNotification(localNotification);

				sendNoResultClearCallback(callbackContext);

				return true;
			} else if (action.equalsIgnoreCase(ACTION_CANCEL_ALL_LOCAL_NOTIFICATIONS)) {
				Log.d(TAG, action);

				BCLocalNotificationManager.getInstance().cancelAllLocalNotifications();

				sendNoResultClearCallback(callbackContext);

				return true;
			} else {
				Log.e(TAG, "Invalid action: " + action);

				sendErrorMessage(callbackContext, "Invalid action: " + action);

				return false;
			}
		} catch(Exception e) {
			Log.e(TAG, e.toString());

			sendErrorMessage(callbackContext, e.toString());

			return false;
		}
	}

	@Override
	public void onResume(boolean multitasking) {
		super.onResume(multitasking);
		
		Log.d(TAG, "onResume multitasking = " + multitasking);
		
		BCMicroLocationManager.getInstance().didEnterForeground();
	}

	@Override
	public void onPause(boolean multitasking) {
		super.onPause(multitasking);
		
		Log.d(TAG, "onPause multitasking = " + multitasking);

		BCMicroLocationManager.getInstance().didEnterBackground();
	}

	private IBlueCatsSDKCallback mBlueCatsSDKCallback = new IBlueCatsSDKCallback() {
		@Override
		public void onDidEnterSite(final BCSite site) {
			Log.d(TAG, "ACTION_DID_ENTER_SITE site=" + site);
		}

		@Override
		public void onDidExitSite(final BCSite site) {
			Log.d(TAG, "ACTION_DID_EXIT_SITE site=" + site);
		}

		@Override
		public void onDidUpdateNearbySites(final List<BCSite> sites) {
			Log.d(TAG, "ACTION_DID_UPDATE_NEARBY_SITES sites=" + sites);
		}

		@Override
		public void onDidRangeBeaconsForSiteID(final BCSite site, final List<BCBeacon> beacons) {
			Log.d(TAG, "ACTION_DID_RANGE_BEACONS_FOR_SITE_ID site=" + site + " beacons=" + beacons.size());
		}

		@Override
		public void onDidUpdateMicroLocation(final List<BCMicroLocation> microLocations) {
			if(mCallbackContext == null) {
				return;
			}

			Log.d(TAG, "microLocations = " + microLocations.size());

			if (microLocations == null || microLocations.size() == 0) {
				return;
			}

			BCMicroLocation microLocation;
			if (microLocations.size() == 2) {
				microLocation = microLocations.get(1);
			}
			else
				microLocation = microLocations.get(0);

			sendMicroLocation(mCallbackContext, microLocation);
		}

		@Override
		public void onDidNotify(final int id) {

		}
	};

	private void sendMicroLocation(CallbackContext callbackContext, BCMicroLocation microLocation) {
		if (microLocation == null) {
			return;
		}

		JSONObject jsonObject;
		try {
			String jsonResult = getGson().toJson(microLocation, BCMicroLocation.class);
			jsonObject = new JSONObject(jsonResult);

			PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
			pluginResult.setKeepCallback(true);

			callbackContext.sendPluginResult(pluginResult);
		} catch (JSONException e) {
			Log.e(TAG, e.toString());
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

	public static void sendLocalNotificationReceived(String alertAction, String alertBody, String userInfo) {
		if (mLocalNotificationReceivedCallbackContext == null) {
			return;
		}

		JSONObject jsonObject;
		try {
			jsonObject = new JSONObject()
				.put("alertAction", alertAction)
				.put("alertBody", alertBody)
				.put("userInfo", new JSONObject(userInfo));

			PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
			pluginResult.setKeepCallback(true);

			mLocalNotificationReceivedCallbackContext.sendPluginResult(pluginResult);
		} catch (JSONException e) {
			Log.e(TAG, e.toString());
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

	private void sendOkClearCallback(CallbackContext callbackContext)	{
		// Clear callback on the JS side.
		PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
		pluginResult.setKeepCallback(false);
		callbackContext.sendPluginResult(pluginResult);
	}

	private void sendOkKeepCallback(CallbackContext callbackContext)	{
		PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
		pluginResult.setKeepCallback(false);
		callbackContext.sendPluginResult(pluginResult);
	}

	private void sendNoResultClearCallback(CallbackContext callbackContext)	{
		// Clear callback on the JS side.
		PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
		pluginResult.setKeepCallback(false);
		callbackContext.sendPluginResult(pluginResult);
	}

	private void sendErrorMessage(CallbackContext callbackContext, String errorMessage)	{
		// Clear callback on the JS side.
		PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, errorMessage);
		callbackContext.sendPluginResult(pluginResult);
	}
}
