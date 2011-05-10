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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.admsample.client.service.DeviceRegService;
import com.google.android.c2dm.C2DMBaseReceiver;

public class C2DMReceiver extends C2DMBaseReceiver {
    public C2DMReceiver() {
        super(App.PUSH_MSG_SENDER_ID);
    }

    @Override
    public void onRegistered(Context context, String registration) {
        DeviceRegService.registerWithServer(context, registration);
    }

    @Override
    public void onUnregistered(Context context) {
        SharedPreferences prefs = Prefs.get(context);
        String deviceRegistrationID = prefs.getString("deviceRegistrationID", null);
        DeviceRegService.unregisterWithServer(context, deviceRegistrationID);
    }

    @Override
    public void onError(Context context, String errorId) {
        context.sendBroadcast(new Intent(App.UPDATE_DONE_ACTION));
    }

    @Override
    public void onMessage(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        String pushCommand = null;
        if (extras != null) {
            pushCommand = (String) extras.get(App.PUSH_COMMAND);
            if (pushCommand != null) {
                PushProcessor.process(context, pushCommand);
            }
        }
    }
}
