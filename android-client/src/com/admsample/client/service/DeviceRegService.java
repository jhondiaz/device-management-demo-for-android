/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.admsample.client.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.admsample.client.App;
import com.admsample.client.AppEngineClient;
import com.admsample.client.Prefs;
import com.admsample.client.AppEngineClient.PendingAuthException;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import android.util.Log;

/**
 * Register/unregister with the Device Management for Android Demo App Engine server.
 */
public class DeviceRegService extends IntentService {

    private static final String TAG = "DeviceRegSvc";
    private static final String REGISTER_PATH = "/register";
    private static final String UNREGISTER_PATH = "/unregister";
    
    DeviceRegService() {
        super(TAG);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        final String deviceRegistrationID = intent.getStringExtra(App.DEVICE_REGID_KEY);
        if (App.REGISTER_ACTION.equals(action)) {
            registerWithServer(this, deviceRegistrationID);
        } else if (App.UNREGISTER_ACTION.equals(action)) {
            unregisterWithServer(this, deviceRegistrationID);
        }
    }
    
    public static void registerWithServer(final Context context, final String deviceRegistrationID) {
        Intent updateUIIntent = new Intent(App.UPDATE_DONE_ACTION);
        try {
            HttpResponse res = makeRequest(context, deviceRegistrationID, REGISTER_PATH);
            if (res.getStatusLine().getStatusCode() == 200) {
                SharedPreferences.Editor editor = Prefs.get(context).edit();
                editor.putString("deviceRegistrationID", deviceRegistrationID);
                editor.commit();
                updateUIIntent.putExtra(App.STATUS_EXTRA, App.REGISTERED_STATUS);
            } else if (res.getStatusLine().getStatusCode() == 400) {
                updateUIIntent.putExtra(App.STATUS_EXTRA, App.AUTH_ERROR_STATUS);
            } else {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.w(TAG, "Registration error " +
                            String.valueOf(res.getStatusLine().getStatusCode()));
                }
                updateUIIntent.putExtra(App.STATUS_EXTRA, App.ERROR_STATUS);
            }
            context.sendBroadcast(updateUIIntent);
        } catch (AppEngineClient.PendingAuthException pae) {
            // Ignore - we'll reregister later.
        } catch (Exception e) {
            Log.w(TAG, "Registration error " + e.getMessage());
            updateUIIntent.putExtra(App.STATUS_EXTRA, App.ERROR_STATUS);
            context.sendBroadcast(updateUIIntent);
        }
    }

    public static void unregisterWithServer(final Context context, final String deviceRegistrationID) {
        Intent updateUIIntent = new Intent(App.UPDATE_DONE_ACTION);
        try {
            HttpResponse res = makeRequest(context, deviceRegistrationID, UNREGISTER_PATH);
            if (res.getStatusLine().getStatusCode() == 200) {
                SharedPreferences settings = Prefs.get(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.remove(App.DEVICE_REGID_KEY);
                editor.commit();
                updateUIIntent.putExtra(App.STATUS_EXTRA, App.UNREGISTERED_STATUS);
            } else {
                Log.w(TAG, "Unregistration error " +
                        String.valueOf(res.getStatusLine().getStatusCode()));
                updateUIIntent.putExtra(App.STATUS_EXTRA, App.ERROR_STATUS);
            }
        } catch (Exception e) {
            updateUIIntent.putExtra(App.STATUS_EXTRA, App.ERROR_STATUS);
            Log.w(TAG, "Unegistration error " + e.getMessage());
        }
    
        // Update dialog activity
        context.sendBroadcast(updateUIIntent);
    }
    
    private static HttpResponse makeRequest(Context context, String deviceRegistrationID,
            String urlPath) throws Exception {
        SharedPreferences settings = Prefs.get(context);
        String accountName = settings.getString(App.ACCOUNT_KEY, null);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("devregid", deviceRegistrationID));

        String deviceId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        if (deviceId != null) {
            params.add(new BasicNameValuePair("deviceId", deviceId));
        }

        AppEngineClient client = new AppEngineClient(context, accountName);
        return client.makeRequest(urlPath, params);
    }
}
