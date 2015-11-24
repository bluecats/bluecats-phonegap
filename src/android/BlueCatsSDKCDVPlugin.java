package com.blueCats.BlueCatsSDKCDVPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

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
import com.google.gson.JsonSyntaxException;

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

	private WeakHashMap<String, CallbackContext> mEventCallbackIds;
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
	public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
		if (DBG) Log.d(TAG, action);
		
		final Activity mCordovaActivity = BlueCatsSDKCDVPlugin.this.cordova.getActivity();

		try {
			if (action.equalsIgnoreCase(ACTION_START_PURRING_WITH_APP_TOKEN)) {
				cordova.getThreadPool().execute(new Runnable() {
		            public void run() {
						if (args.length() > 1) {
							try {
								JsonElement jElement = new JsonParser().parse(args.getJSONObject(1).toString());
								JsonObject argument = jElement.getAsJsonObject();
								Map<String, String> options = getSDKOptionsFromArgument(argument);
								BlueCatsSDK.setOptions(options);
								
								if (argument.has("importLocalDatabase")) {
									boolean importLocalDatabase = argument.get("importLocalDatabase").getAsBoolean();
									if (importLocalDatabase) {
										importSQLiteDatabase(mCordovaActivity);
									}
								}

								String appToken = args.getString(0);
								BlueCatsSDK.startPurringWithAppToken(mCordovaActivity.getApplicationContext(), appToken);

								mEventCallbackIds = new WeakHashMap<String, CallbackContext>();

								sendOkClearCallback(callbackContext);
							} catch (JsonSyntaxException e) {
								Log.e(TAG, e.toString());
							} catch (JSONException e) {
								Log.e(TAG, e.toString());
							}
						}
		            }
		        });

				return true;
			} else if (action.equalsIgnoreCase(ACTION_MONITOR_MICRO_LOCATION)) {
				cordova.getThreadPool().execute(new Runnable() {
					@Override
					public void run() {
						try {
							String eventId = args.getString(0);
							JsonElement jElement = new JsonParser().parse(args.getJSONObject(1).toString());
							JsonObject optionsArg = jElement.getAsJsonObject();
							List<IBCEventFilter> filters = getFiltersFromBeaconOptionsArgument(optionsArg);

							filters.add(BCEventFilter.filterByMinTimeIntervalBetweenTriggers(getMinimumTriggerIntervalInMillisecondsFromBeaconOptionsArgument(optionsArg)));

							BCTrigger trigger = new BCTrigger(eventId, filters);
							trigger.setRepeatCount(getRepeatCountFromBeaconOptionsArgument(optionsArg));

							BCEventManager eventManager = BCEventManager.getInstance();
							eventManager.monitorEventWithTrigger(trigger, mEventManagerCallback);

							synchronized(mEventCallbackIds) {
								mEventCallbackIds.put(eventId, callbackContext);
							}
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
					}
				});

				return true;
			} else if (action.equalsIgnoreCase(ACTION_MONITOR_CLOSEST_BEACON_CHANGE)) {
				cordova.getThreadPool().execute(new Runnable() {
					@Override
					public void run() {
						try {
							String eventId = args.getString(0);
							JsonElement jElement = new JsonParser().parse(args.getJSONObject(1).toString());
							JsonObject optionsArg = jElement.getAsJsonObject();
							List<IBCEventFilter> filters = getFiltersFromBeaconOptionsArgument(optionsArg);

							filters.add(BCEventFilter.filterByMinTimeIntervalBetweenTriggers(getMinimumTriggerIntervalInMillisecondsFromBeaconOptionsArgument(optionsArg)));
							filters.add(BCEventFilter.filterByClosestBeaconChanged());

							BCTrigger trigger = new BCTrigger(eventId, filters);
							trigger.setRepeatCount(getRepeatCountFromBeaconOptionsArgument(optionsArg));

							BCEventManager eventManager = BCEventManager.getInstance();
							eventManager.monitorEventWithTrigger(trigger, mEventManagerCallback);

							synchronized(mEventCallbackIds) {
								mEventCallbackIds.put(eventId, callbackContext);
							}
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
					}
				});

				return true;
			} else if (action.equalsIgnoreCase(ACTION_MONITOR_ENTER_BEACON)) {
				cordova.getThreadPool().execute(new Runnable() {
					@Override
					public void run() {
						try {
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

							synchronized(mEventCallbackIds) {
								mEventCallbackIds.put(eventId, callbackContext);
							}
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
					}
				});

				return true;
			} else if (action.equalsIgnoreCase(ACTION_MONITOR_EXIT_BEACON)) {
				cordova.getThreadPool().execute(new Runnable() {
					@Override
					public void run() {
						try {
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

							synchronized(mEventCallbackIds) {
								mEventCallbackIds.put(eventId, callbackContext);
							}
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
					}
				});

				return true;
			} else if (action.equalsIgnoreCase(ACTION_REMOVE_MONITORED_EVENT)) {
				cordova.getThreadPool().execute(new Runnable() {
					@Override
					public void run() {
						try {
							String eventId = args.getString(0);
							BCEventManager.getInstance().removeMonitoredEvent(eventId);
							mEventCallbackIds.remove(eventId);
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
					}
				});

				return true;
			} else if (action.equalsIgnoreCase(ACTION_REGISTER_LOCAL_NOTIFICATION_RECEIVED_CALLBACK)) {
				mLocalNotificationReceivedCallbackContext = new WeakReference<CallbackContext>(callbackContext);

				return true;
			} else if (action.equalsIgnoreCase(ACTION_SCHEDULE_LOCAL_NOTIFICATION)) {
				cordova.getThreadPool().execute(new Runnable() {
					@Override
					public void run() {
						// update the notification id counter
						mNotificationCounter++;

						try {
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
						} catch (JsonSyntaxException e) {
							Log.e(TAG, e.toString());
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
					}
				});

				return true;
			} else if (action.equalsIgnoreCase(ACTION_CANCEL_ALL_LOCAL_NOTIFICATIONS)) {
				cordova.getThreadPool().execute(new Runnable() {
					@Override
					public void run() {
						if (DBG) Log.d(TAG, action);

						BCLocalNotificationManager.getInstance().cancelAllLocalNotifications();

						sendNoResultClearCallback(callbackContext);
					}
				});

				return true;
			} else {
				cordova.getThreadPool().execute(new Runnable() {
					@Override
					public void run() {
						Log.e(TAG, "Invalid action: " + action);

						sendErrorMessage(callbackContext, "Invalid action: " + action);
					}
				});

				return false;
			}
		} catch (final Exception e) {
			cordova.getThreadPool().execute(new Runnable() {
				@Override
				public void run() {
					Log.e(TAG, e.toString());

					sendErrorMessage(callbackContext, e.toString());
				}
			});

			return false;
		}
	}

	@Override
	public void onResume(boolean multitasking) {
		super.onResume(multitasking);

		if (DBG) Log.d(TAG, "onResume multitasking = " + multitasking);

		BlueCatsSDK.didEnterForeground();
	}

	@Override
	public void onPause(boolean multitasking) {
		super.onPause(multitasking);

		if (DBG) Log.d(TAG, "onPause multitasking = " + multitasking);

		BlueCatsSDK.didEnterBackground();
	}

	private BCEventManagerCallback mEventManagerCallback = new BCEventManagerCallback() {
		@Override
		public void onTriggeredEvent(BCTriggeredEvent triggeredEvent) {
			if (DBG) Log.d(TAG, triggeredEvent.getEvent().getEventIdentifier());
			
			synchronized(mEventCallbackIds) {
				CallbackContext callbackContext = mEventCallbackIds.get(triggeredEvent.getEvent().getEventIdentifier());
				if (callbackContext != null) {
					try {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("filteredMicroLocation", new JSONObject(getGson().toJson(triggeredEvent.getFilteredMicroLocation(), BCMicroLocation.class)));
						jsonObject.put("triggeredCount", triggeredEvent.getTriggeredCount());

						sendJsonObject(jsonObject, callbackContext, true);
					} catch (JSONException e) {
						Log.e(TAG, e.toString());
					}
				}
			}
		}
	};

	public static void sendLocalNotificationReceived(String alertAction, String alertBody, String userInfo) {
		if (mLocalNotificationReceivedCallbackContext == null) {
			return;
		}

		final CallbackContext callbackContext = mLocalNotificationReceivedCallbackContext.get();
		if (callbackContext == null) {
			return;
		}

		final JSONObject jsonObject;
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

		Map<String, Object> argumentDictionary = getGson().fromJson(argument.toString(), HashMap.class);
		for (String key: argumentDictionary.keySet()) {
			String nativeKey = getSDKOptionForKey(key);
			if (nativeKey != null) {
				Object value = argumentDictionary.get(key);
				if (value instanceof Boolean) {
					optionsDictionary.put(nativeKey, Boolean.toString(Boolean.class.cast(value)));
				} else if (value instanceof Long) {
					optionsDictionary.put(nativeKey, Long.toString(Long.class.cast(value)));
				} else if (value instanceof Double) {
					optionsDictionary.put(nativeKey, Long.toString(Double.class.cast(value).longValue()));
				} 
			}
		}
		return optionsDictionary;
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
			BCProximity minimumProximity = getProximityKeyToNativeValue(filterSettings.get("minimumProximity").getAsString());
			if (minimumProximity == BCProximity.BC_PROXIMITY_IMMEDIATE) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_IMMEDIATE, BCProximity.BC_PROXIMITY_NEAR, BCProximity.BC_PROXIMITY_FAR, BCProximity.BC_PROXIMITY_UNKNOWN })));
			} else if (minimumProximity == BCProximity.BC_PROXIMITY_NEAR) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_NEAR, BCProximity.BC_PROXIMITY_FAR, BCProximity.BC_PROXIMITY_UNKNOWN })));
			} else if (minimumProximity == BCProximity.BC_PROXIMITY_FAR) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_FAR, BCProximity.BC_PROXIMITY_UNKNOWN })));
			} else if (minimumProximity == BCProximity.BC_PROXIMITY_UNKNOWN) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_UNKNOWN })));
			}
		}

		if (filterSettings.has("maximumProximity")) {
			BCProximity maximumProximity = getProximityKeyToNativeValue(filterSettings.get("maximumProximity").getAsString());
			if (maximumProximity == BCProximity.BC_PROXIMITY_IMMEDIATE) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_IMMEDIATE })));
			} else if (maximumProximity == BCProximity.BC_PROXIMITY_NEAR) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_IMMEDIATE, BCProximity.BC_PROXIMITY_NEAR })));
			} else if (maximumProximity == BCProximity.BC_PROXIMITY_FAR) {
				filters.add(BCEventFilter.filterByProximities(Arrays.asList(new BCProximity[] { BCProximity.BC_PROXIMITY_IMMEDIATE, BCProximity.BC_PROXIMITY_NEAR, BCProximity.BC_PROXIMITY_FAR })));
			} else if (maximumProximity == BCProximity.BC_PROXIMITY_UNKNOWN) {
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

	private String getSDKOptionForKey(String sdkOption) {
		if (sdkOption.equals("useStageApi")) {
			return BlueCatsSDK.BC_OPTION_USE_STAGE_API;
		} else if (sdkOption.equals("trackBeaconVisits")) {
			return BlueCatsSDK.BC_OPTION_BEACON_VISIT_TRACKING_ENABLED;
		} else if (sdkOption.equals("monitorBlueCatsRegionOnStartup")) {
			return BlueCatsSDK.BC_OPTION_MONITOR_BLUE_CATS_REGION_ON_STARTUP;
		} else if (sdkOption.equals("monitorAllAvailableRegionsOnStartup")) {
			return BlueCatsSDK.BC_OPTION_MONITOR_ALL_AVAILABLE_REGIONS_ON_STARTUP;
		//} else if (sdkOption.equals("useEnergySaverScanStrategy")) {
		//	return BlueCatsSDK.BC_OPTION_USE_ENERGY_SAVER_SCAN_STRATEGY;
		} else if (sdkOption.equals("crowdSourceBeaconUpdates")) {
			return BlueCatsSDK.BC_OPTION_CROWD_SOURCE_BEACON_UPDATES;
		} else if (sdkOption.equals("useLocalStorage")) {
			return BlueCatsSDK.BC_OPTION_USE_LOCAL_STORAGE;
		} else if (sdkOption.equals("cacheAllBeaconsForApp")) {
			return BlueCatsSDK.BC_OPTION_CACHE_ALL_BEACONS_FOR_APP;
		} else if (sdkOption.equals("discoverBeaconsNearby")) {
			return BlueCatsSDK.BC_OPTION_DISCOVER_BEACONS_NEARBY;
		} else if (sdkOption.equals("useRSSISmoothing")) {
			return BlueCatsSDK.BC_OPTION_USE_RSSI_SMOOTHING;
		} else if (sdkOption.equals("cacheRefreshTimeIntervalInSeconds")) {
			return BlueCatsSDK.BC_OPTION_CACHE_REFRESH_TIME_INTERVAL_IN_MILLISECONDS;
		}
		return null;
	}

	private BCProximity getProximityKeyToNativeValue(String proximityKey) {
		if (proximityKey.equals("BC_PROXIMITY_IMMEDIATE")) {
			return BCProximity.BC_PROXIMITY_IMMEDIATE;
		} else if (proximityKey.equals("BC_PROXIMITY_NEAR")) {
			return BCProximity.BC_PROXIMITY_NEAR;
		} else if (proximityKey.equals("BC_PROXIMITY_FAR")) {
			return BCProximity.BC_PROXIMITY_FAR;
		}
		return BCProximity.BC_PROXIMITY_UNKNOWN;
	}
	
	private void importSQLiteDatabase(Activity activity) {
		String packageName = activity.getPackageName();
		String sourceDatabaseName = String.format("%s.%s", packageName, "bluecats.SQLite.db");
		
		String destinationDBPath = activity.getDatabasePath(sourceDatabaseName).getAbsolutePath();
		File destinationDB = new File(destinationDBPath);
		
		if (destinationDB.exists()) {
			if (DBG) Log.d(TAG, "Database already exists"); 
		} else {
			File dbPath = new File(destinationDB.getParent());
			if (!dbPath.exists()) {
				dbPath.mkdir();
			}
			
			InputStream in = null;
	        OutputStream out = null;
	        try {
	        	in = activity.getAssets().open(sourceDatabaseName);
	        	out = new FileOutputStream(destinationDB);

	            byte[] buffer = new byte[1024];
	            int length;
	            while ((length = in.read(buffer)) > 0) {
	            	out.write(buffer, 0, length);
	            }
	            out.flush();
	            
				if (DBG) Log.d(TAG, "Exported database to " + destinationDB.getAbsolutePath()); 
	        } catch (IOException e) {
	        	Log.e(TAG, e.toString());
	        } finally {
	        	try {
	        		if (in != null) {
	        			in.close();
		            }
		            if (out != null) {
		            	out.close();
		            }
	        	} catch (IOException e) {
	        		Log.e(TAG, e.toString());  
	            }
	        }
		}
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
