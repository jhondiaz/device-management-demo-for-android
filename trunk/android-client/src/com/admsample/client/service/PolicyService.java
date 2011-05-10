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

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.json.JSONException;

import com.admsample.client.App;
import com.admsample.client.AppEngineClient;
import com.admsample.client.Policy;
import com.admsample.client.AppEngineClient.PendingAuthException;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class PolicyService extends IntentService {
    
    private static final String TAG = "PolicySvc";

    public PolicyService() {
        super(TAG);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        if (App.GET_POLICY_ACTION.equals(action)) {
            getPolicy(intent);
        }
    }

    private void getPolicy(Intent intent) {
        final String accountName = intent.getStringExtra(App.ACCOUNT_KEY);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "PolicyService received account: " + accountName);
        }
        Intent completeIntent = new Intent(App.UPDATE_DONE_ACTION);
        if (accountName != null) {
            AppEngineClient client = new AppEngineClient(this, accountName);
            HttpResponse resp = null;
            try {
                resp = client.makeRequest("/getpolicy", null);
                final int statusCode = resp.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resp.getEntity().getContent(), "UTF-8"), 256);
                    StringBuilder buffer = new StringBuilder(256);
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }
                    
                    try {
                        if (buffer.length() == 2 && buffer.substring(0, 2).equals("{}")) {
                            completeIntent.putExtra(App.STATUS_EXTRA, App.EMPTY_POLICY_STATUS);
                        } else {
                            Policy policy = new Policy(this, buffer.toString());
                            policy.writeToLocal();
                            completeIntent.putExtra(App.STATUS_EXTRA, App.UPDATED_POLICY_STATUS);
                        }
                    } catch (JSONException exception) {
                        completeIntent.putExtra(App.STATUS_EXTRA, App.INVALID_POLICY_STATUS);
                    }
                } else if (statusCode == 400) {
                    completeIntent.putExtra(App.STATUS_EXTRA, App.AUTH_ERROR_STATUS);
                }
            } catch (AppEngineClient.PendingAuthException e) {
                completeIntent.putExtra(App.STATUS_EXTRA, App.PENDING_AUTH);
            }
            catch (Exception e) {
                Log.e(TAG, "Error in getPolicy()");
                e.printStackTrace();
                completeIntent.putExtra(App.STATUS_EXTRA, App.ERROR_STATUS);
            }
        }
        sendBroadcast(completeIntent);
    }
}
