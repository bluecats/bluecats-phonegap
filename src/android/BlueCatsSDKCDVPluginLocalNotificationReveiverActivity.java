package com.blueCats.BlueCatsSDKCDVPlugin;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;

public class BlueCatsSDKCDVPluginLocalNotificationReveiverActivity extends Activity {
    private static String TAG = "BlueCatsSDKCDVPluginLocalNotificationReveiverActivity";
    
    private Gson mGson;
    private Gson getGson() {
        if (mGson == null) {
            mGson = new Gson();
        }
        return mGson;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Received notification");
        
        // start the main activity
        Context context = getApplicationContext();
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(launchIntent);

        // unbundle notification and send local notification received
        Intent contentIntent = this.getIntent();
        Bundle bundle = contentIntent.getExtras();

        try {
            Map<String, String> userInfoMap = new HashMap<String, String>();
            Bundle userInfoBundle = bundle.getBundle("userInfo");
            for (String key: userInfoBundle.keySet()) {
                userInfoMap.put(key, userInfoBundle.getString(key));
            }
            
            String alertAction = bundle.getString("alertAction"); 
            String alertBody = bundle.getString("alertBody");
            String userInfo = getGson().toJson(userInfoMap, HashMap.class);

            BlueCatsSDKCDVPlugin.sendLocalNotificationReceived(alertAction, alertBody, userInfo);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
		finish();
    }
}
