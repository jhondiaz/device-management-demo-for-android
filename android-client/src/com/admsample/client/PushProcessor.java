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

package com.admsample.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.admsample.client.util.Helper;

public class PushProcessor extends BroadcastReceiver {
    public static String PENDING_LOCK = "pendingLock";
    public static String PENDING_POLICY = "pendingPolicy";
    
    public static void process(Context ctx, String command) {
        SharedPreferences prefs = Prefs.get(ctx);
        String accountName = prefs.getString(App.ACCOUNT_KEY, null);
        if (accountName == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        // Remote screen lock.
        if (App.PUSH_LOCK.equals(command)) {        
            Helper.sendNotification(ctx, 
                    App.POLICY_ENFORCEMENT_ID, 
                    ctx.getResources().getString(R.string.process_lock),
                    null);
            editor.putBoolean(PENDING_LOCK, true).commit();
        // Remote policy push.
        } else if (App.PUSH_POLICY.equals(command)) {
            Helper.sendNotification(ctx, 
                    App.POLICY_ENFORCEMENT_ID, 
                    ctx.getResources().getString(R.string.process_policy_sync),
                    null);
            editor.putBoolean(PENDING_POLICY, true).commit();
        }
        RemoteRequestProcessor.getPolicy(ctx, accountName);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final int result = intent.getIntExtra(App.STATUS_EXTRA, App.ERROR_STATUS);
        if (result == App.UPDATED_POLICY_STATUS) {
            SharedPreferences prefs = Prefs.get(context);
            SharedPreferences.Editor editor = prefs.edit();
            if (prefs.getBoolean(PENDING_LOCK, false)) {
                Policy policy = new Policy(context);
                policy.readFromLocal();
                editor.remove(PENDING_LOCK).commit();
                policy.enforceLock();
            }
        }
    }
}
