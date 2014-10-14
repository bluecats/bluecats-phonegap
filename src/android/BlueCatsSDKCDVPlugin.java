package com.blueCats.BlueCatsSDKCDVPlugin;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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
import com.bluecats.sdk.BCEventFilter;
import com.bluecats.sdk.BCEventManager;
import com.bluecats.sdk.BCEventManagerCallback;
import com.bluecats.sdk.BCLocalNotification;
import com.bluecats.sdk.BCLocalNotificationManager;
import com.bluecats.sdk.BCMicroLocation;
import com.bluecats.sdk.BCMicroLocationManager;
import com.bluecats.sdk.BCSite;
import com.bluecats.sdk.BCTrigger;
import com.bluecats.sdk.BCTriggeredEvent;
import com.bluecats.sdk.BlueCatsSDK;
import com.bluecats.sdk.BCBeacon.BCProximity;
import com.bluecats.sdk.IBCEventFilter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BlueCatsSDKCDVPlugin extends CordovaPlugin {
	private static String TAG = "BlueCatsSDKCDVPlugin";

	private static final boolean DBG = true;

	public static final String ACTION_START_PURRING_WITH_APP_TOKEN = "startPurringWithAppToken";
	public static final String ACTION_MONITOR_MICRO_LOCATION = "monitorMicroLocation";
	public static final String ACTION_MONITOR_CLOSEST_BEACON_CHANGE = "monitorClosestBeaconChange";
	public static final String ACTION_MONITOR_ENTER_BEACON = "monitorEnterBeacon";
	public static final String ACTION_MONITOR_EXIT_BEACON = "monitorExitBeacon";
	public static final String ACTION_REMOVE_MONITORED_EVENT = "removeMonitoredEvent";
	public static final String ACTION_REGISTER_LOCAL_NOTIFICATION_RECEIVED_CALLBACK = "registerLocalNotificationReceivedCallback";
	public static final String ACTION_SCHEDULE_LOCAL_NOTIFICATION = "scheduleLocalNotification";
	public static final String ACTION_CANCEL_ALL_LOCAL_NOTIFICATIONS = "cancelAllLocalNotifications";

	private Map<String, WeakReference<CallbackContext>> mEventCallbackIds;
	private static WeakReference<CallbackContext> mLocalNotificationReceivedCallbackContext;
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
		if (DBG) Log.d(TAG, action);

		try {
			if (action.equalsIgnoreCase(ACTION_START_PURRING_WITH_APP_TOKEN)) {
				if (args.length() > 1) {
					JsonElement jElement = new JsonParser().parse(args.getJSONObject(1).toString());
					Map<String, String> options = getSDKOptionsFromArgument(jElement.getAsJsonObject());
					BlueCatsSDK.setOptions(options);
				}

				String appToken = args.getString(0);
				BlueCatsSDK.startPurringWithAppToken(this.cordova.getActivity().getApplicationContext(), appToken);

				mEventCallbackIds = new ConcurrentHashMap<String, WeakReference<CallbackContext>>();

				sendOkClearCallback(callbackContext);

				return true;
			} else if (action.equalsIgnoreCase(ACTION_MONITOR_MICRO_LOCATION)) {
				String eventId = args.getString(0);
				JsonElement jElement = new JsonParser().parse(args.getJSONObject(1).toString());
				JsonObject optionsArg = jElement.getAsJsonObject();
				List<IBCEventFilter> filters = getFiltersFromBeaconOptionsArgument(optionsArg);

				filters.add(BCEventFilter.filterByMinTimeIntervalBetweenTriggers(getMinimumTriggerIntervalInMillisecondsFromBeaconOptionsArgument(optionsArg)));

				BCTrigger trigger = new BCTrigger(eventId, filters);
				trigger.setRepeatCount(getRepeatCountFromBeaconOptionsArgument(optionsArg));

				BCEventManager eventManager = BCEventManager.getInstance();
				eventManager.monitorEventWithTrigger(trigger, mEventManagerCallback);

				mEventCallbackIds.put(eventId, new WeakReference<CallbackContext>(callbackContext));

				return true;
			} else if (action.equalsIgnoreCase(ACTION_MONITOR_CLOSEST_BEACON_CHANGE)) {
				String eventId = args.getString(0);
				JsonElement jElement = new JsonParser().parse(args.getJSONObject(1).toString());
				JsonObject optionsArg = jElement.getAsJsonObject();
				List<IBCEventFilter> filters = getFiltersFromBeaconOptionsArgument(optionsArg);
				
				filters.add(BCEventFilter.filterApplySmoothedAccuracyOverTimeInterval(5000));
			    filters.add(BCEventFilter.filterByMinTimeIntervalBetweenTriggers(getMinimumTriggerIntervalInMillisecondsFromBeaconOptionsArgument(optionsArg)));
			    filters.add(BCEventFilter.filterByClosestBeaconChanged());
			    
			    BCTrigger trigger = new BCTrigger(eventId, filters);
				trigger.setRepeatCount(getRepeatCountFromBeaconOptionsArgument(optionsArg));

				BCEventManager eventManager = BCEventManager.getInstance();
				eventManager.monitorEventWithTrigger(trigger, mEventManagerCallback);

				mEventCallbackIds.put(eventId, new WeakReference<CallbackContext>(callbackContext));

				return true;
			} else if (action.equalsIgnoreCase(ACTION_MONITOR_ENTER_BEACON)) {
				String eventId = args.getString(0);
				JsonElement jElement = new JsonParser().parse(args.getJSONObject(1).toString());
				JsonObject optionsArg = jElement.getAsJsonObject();
				List<IBCEventFilter> filters = getFiltersFromBeaconOptionsArgument(optionsArg);
				
				filters.add(BCEventFilter.filterByMinTimeIntervalBetweenTriggers(getMinimumTriggerIntervalInMillisecondsFromBeaconOptionsArgument(optionsArg)));
			    filters.add(BCEventFilter.filterByEnteredBeaconResetAfterTimeIntervalUnmatched(getMillisecondsBeforeExitBeaconFromBeaconOptionsArgument(optionsArg)));
			    
			    BCTrigger trigger = new BCTrigger(eventId, filters);
				trigger.setRepeatCount(getRepeatCountFromBeaconOptionsArgument(optionsArg));

				BCEventManager eventManager = BCEventManager.getInstance();
				eventManager.monitorEventWithTrigger(trigger, mEventManagerCallback);

				mEventCallbackIds.put(eventId, new WeakReference<CallbackContext>(callbackContext));

				return true;
			} else if (action.equalsIgnoreCase(ACTION_MONITOR_EXIT_BEACON)) {
				String eventId = args.getString(0);
				JsonElement jElement = new JsonParser().parse(args.getJSONObject(1).toString());
				JsonObject optionsArg = jElement.getAsJsonObject();
				List<IBCEventFilter> filters = getFiltersFromBeaconOptionsArgument(optionsArg);
				
				filters.add(BCEventFilter.filterByMinTimeIntervalBetweenTriggers(getMinimumTriggerIntervalInMillisecondsFromBeaconOptionsArgument(optionsArg)));
			    filters.add(BCEventFilter.filterByExitedBeaconAfterTimeIntervalUnmatched(getMillisecondsBeforeExitBeaconFromBeaconOptionsArgument(optionsArg)));
			    
			    BCTrigger trigger = new BCTrigger(eventId, filters);
				trigger.setRepeatCount(getRepeatCountFromBeaconOptionsArgument(optionsArg));

				BCEventManager eventManager = BCEventManager.getInstance();
				eventManager.monitorEventWithTrigger(trigger, mEventManagerCallback);

				mEventCallbackIds.put(eventId, new WeakReference<CallbackContext>(callbackContext));

				return true;
			} else if (action.equalsIgnoreCase(ACTION_REMOVE_MONITORED_EVENT)) {
				String eventId = args.getString(0);
				BCEventManager.getInstance().removeMonitoredEvent(eventId);
				mEventCallbackIds.remove(eventId);

				return true;
			} else if (action.equalsIgnoreCase(ACTION_REGISTER_LOCAL_NOTIFICATION_RECEIVED_CALLBACK)) {
				mLocalNotificationReceivedCallbackContext = new WeakReference<CallbackContext>(callbackContext);

				return true;
			} else if (action.equalsIgnoreCase(ACTION_SCHEDULE_LOCAL_NOTIFICATION)) {
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

				Intent contentIntent = new Intent(this.cordova.getActivity(), BlueCatsSDKCDVPluginLocalNotificationReveiverActivity.class);
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
				if (DBG) Log.d(TAG, action);

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

		if (DBG) Log.d(TAG, "onResume multitasking = " + multitasking);

		BCMicroLocationManager.getInstance().didEnterForeground();
	}

	@Override
	public void onPause(boolean multitasking) {
		super.onPause(multitasking);

		if (DBG) Log.d(TAG, "onPause multitasking = " + multitasking);

		BCMicroLocationManager.getInstance().didEnterBackground();
	}

	private BCEventManagerCallback mEventManagerCallback = new BCEventManagerCallback() {
		@Override
		public void triggeredEvent(BCTriggeredEvent triggeredEvent) {
			WeakReference<CallbackContext> weakCallbackContext = mEventCallbackIds.get(triggeredEvent.getEvent().getEventIdentifier());
			if (weakCallbackContext == null) {
				return;
			}

			CallbackContext callbackContext = weakCallbackContext.get();
			if (callbackContext != null) {
				try {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("filteredMicroLocation", getGson().toJson(triggeredEvent.getFilteredMicroLocation(), BCMicroLocation.class));
					jsonObject.put("triggeredCount", triggeredEvent.getTriggeredCount());

					sendJsonObject(jsonObject, callbackContext, true);
				} catch (JSONException e) {
					Log.e(TAG, e.toString());
				}
			}
		}
	};

	public static void sendLocalNotificationReceived(String alertAction, String alertBody, String userInfo) {
		if (mLocalNotificationReceivedCallbackContext == null) {
			return;
		}

		CallbackContext callbackContext = mLocalNotificationReceivedCallbackContext.get();
		if (callbackContext == null) {
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

			callbackContext.sendPluginResult(pluginResult);
		} catch (JSONException e) {
			Log.e(TAG, e.toString());
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

	private Map<String, String> getSDKOptionsFromArgument(JsonObject argument) {
		Map<String, String> optionsDictionary = new HashMap<String, String>();

		if (argument == null) {
			return optionsDictionary;
		}

		Map<String, String> argumentDictionary = getGson().fromJson(argument.toString(), HashMap.class);
		for (String key: argumentDictionary.keySet()) {
			String nativeKey = getSDKOptionForKey(key);
			if (nativeKey != null) {
				optionsDictionary.put(nativeKey, argumentDictionary.get(key));
			}
		}
		return optionsDictionary;
	}

	private String getSDKOptionForKey(String sdkOption) {
		if (sdkOption.equals("useStageApi")) {
			return BlueCatsSDK.BC_OPTION_USE_STAGE_API;
		} else if (sdkOption.equals("trackBeaconVisits")) {
			return BlueCatsSDK.BC_OPTION_BEACON_VISIT_TRACKING_ENABLED;
		} else if (sdkOption.equals("monitorBlueCatsRegionOnStartup")) {
			return BlueCatsSDK.BC_OPTION_MONITOR_BLUE_CATS_REGION_ON_STARTUP;
		} else if (sdkOption.equals("monitorAllAvailableRegionsOnStartup")) {
			return BlueCatsSDK.BC_OPTION_MONITOR_ALL_AVAILABLE_REGIONS_ON_STARTUP;
		} else if (sdkOption.equals("useEnergySaverScanStrategy")) {
			return BlueCatsSDK.BC_OPTION_USE_ENERGY_SAVER_SCAN_STRATEGY;
		} else if (sdkOption.equals("crowdSourceBeaconUpdates")) {
			return BlueCatsSDK.BC_OPTION_CROWD_SOURCE_BEACON_UPDATES;
		} else if (sdkOption.equals("useLocalStorage")) {
			return BlueCatsSDK.BC_OPTION_USE_LOCAL_STORAGE;
		} else if (sdkOption.equals("cacheAllBeaconsForApp")) {
			return BlueCatsSDK.BC_OPTION_CACHE_ALL_BEACONS_FOR_APP;
		} else if (sdkOption.equals("discoverBeaconsNearby")) {
			return BlueCatsSDK.BC_OPTION_DISCOVER_BEACONS_NEARBY;
		} else if (sdkOption.equals("cacheRefreshTimeIntervalInSeconds")) {
			return BlueCatsSDK.BC_OPTION_CACHE_REFRESH_TIME_INTERVAL_IN_MILLISECONDS;
		}
		return null;
	}

	private List<IBCEventFilter> getFiltersFromBeaconOptionsArgument(JsonObject config) {
		List<IBCEventFilter> filters = new ArrayList<IBCEventFilter>();
		JsonObject filterSettings = config.getAsJsonObject("filter");

		if (filterSettings.has("sitesNamed")) {
			List<String> siteNames = getGson().fromJson(filterSettings.get("sitesNamed").toString(), List.class);
			if (siteNames != null && siteNames.size() > 0) {
				filters.add(BCEventFilter.filterBySitesNamed(siteNames));
			}
		}

		if (filterSettings.has("categoriesNamed")) {
			List<String> categoryNames = getGson().fromJson(filterSettings.get("categoriesNamed").toString(), List.class);
			if (categoryNames != null && categoryNames.size() > 0) {
				filters.add(BCEventFilter.filterByCategoriesNamed(categoryNames));
			}
		}

		if (filterSettings.has("minimumProximity")) {
			String minimumProximity = filterSettings.get("minimumProximity").getAsString();
			if (minimumProximity.equals("BC_PROXIMITY_IMMEDIATE")) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_IMMEDIATE, BCProximity.BC_PROXIMITY_NEAR, BCProximity.BC_PROXIMITY_FAR, BCProximity.BC_PROXIMITY_UNKNOWN })));
			} else if (minimumProximity.equals("BC_PROXIMITY_NEAR")) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_NEAR, BCProximity.BC_PROXIMITY_FAR, BCProximity.BC_PROXIMITY_UNKNOWN })));
			} else if (minimumProximity.equals("BC_PROXIMITY_FAR")) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_FAR, BCProximity.BC_PROXIMITY_UNKNOWN })));
			} else if (minimumProximity.equals("BC_PROXIMITY_UNKNOWN")) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_UNKNOWN })));
			}
		}

		if (filterSettings.has("maximumProximity")) {
			String maximumProximity = filterSettings.get("maximumProximity").getAsString();
			if (maximumProximity.equals("BC_PROXIMITY_IMMEDIATE")) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_IMMEDIATE })));
			} else if (maximumProximity.equals("BC_PROXIMITY_NEAR")) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_IMMEDIATE, BCProximity.BC_PROXIMITY_NEAR })));
			} else if (maximumProximity.equals("BC_PROXIMITY_FAR")) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_IMMEDIATE, BCProximity.BC_PROXIMITY_NEAR, BCProximity.BC_PROXIMITY_FAR })));
			} else if (maximumProximity.equals("BC_PROXIMITY_UNKNOWN")) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_IMMEDIATE, BCProximity.BC_PROXIMITY_NEAR, BCProximity.BC_PROXIMITY_FAR, BCProximity.BC_PROXIMITY_UNKNOWN })));
			}
		}

		Double minimumAccuracy = 0d;
		Double maximumAccuracy = Double.MAX_VALUE;
		if (filterSettings.has("minimumAccuracy") || filterSettings.has("maximumAccuracy")) {
			if (filterSettings.has("minimumAccuracy") && filterSettings.get("minimumAccuracy").getAsDouble() > 0.0) {
				minimumAccuracy = filterSettings.get("minimumAccuracy").getAsDouble();
			}
			if (filterSettings.has("maximumAccuracy") && filterSettings.get("maximumAccuracy").getAsDouble() > 0.0) {
				maximumAccuracy = filterSettings.get("maximumAccuracy").getAsDouble();
			}
			filters.add(BCEventFilter.filterByAccuracyRangeFrom(minimumAccuracy, maximumAccuracy));
		}

		return filters;
	}

	private int getRepeatCountFromBeaconOptionsArgument(JsonObject config) {
		if (config.has("repeatCount") && config.get("repeatCount").getAsInt() >= 0) {
			return config.get("repeatCount").getAsInt();
		}
		return Integer.MAX_VALUE;
	}

	private long getMillisecondsBeforeExitBeaconFromBeaconOptionsArgument(JsonObject config) {
		if (config.has("secondsBeforeExitBeacon") && config.get("secondsBeforeExitBeacon").getAsInt() >= 0) {
			return config.get("secondsBeforeExitBeacon").getAsInt() * 1000;
		}
		return (10 * 1000);
	}

	private long getMinimumTriggerIntervalInMillisecondsFromBeaconOptionsArgument(JsonObject config) {
		if (config.has("minimumTriggerIntervalInSeconds") && config.get("minimumTriggerIntervalInSeconds").getAsInt() >= 0) {
			return config.get("minimumTriggerIntervalInSeconds").getAsInt() * 1000;
		}
		return 0;
	}

	private void sendOkClearCallback(CallbackContext callbackContext)	{
		PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
		pluginResult.setKeepCallback(false);
		callbackContext.sendPluginResult(pluginResult);
	}

	private void sendOkKeepCallback(CallbackContext callbackContext)	{
		PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
		pluginResult.setKeepCallback(true);
		callbackContext.sendPluginResult(pluginResult);
	}

	private void sendNoResultClearCallback(CallbackContext callbackContext)	{
		PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
		pluginResult.setKeepCallback(false);
		callbackContext.sendPluginResult(pluginResult);
	}

	private void sendNoResultKeepCallback(CallbackContext callbackContext)	{
		PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
		pluginResult.setKeepCallback(true);
		callbackContext.sendPluginResult(pluginResult);
	}

	private void sendErrorMessage(CallbackContext callbackContext, String errorMessage)	{
		PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, errorMessage);
		callbackContext.sendPluginResult(pluginResult);
	}

	private void sendJsonObject(JSONObject jsonObject, CallbackContext callbackContext, boolean keepCallback)	{
		PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
		pluginResult.setKeepCallback(keepCallback);
		callbackContext.sendPluginResult(pluginResult);
	}
}
