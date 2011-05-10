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

public class RemoteRequestProcessor {
    
    public static void getPolicy(final Context context, final String accountName) {
        Intent getPolicyIntent = new Intent(App.GET_POLICY_ACTION);
        getPolicyIntent.putExtra(App.ACCOUNT_KEY, accountName);
        context.startService(getPolicyIntent);
    }
    
    public static void registerDeviceWithServer(final Context context, final String regId,
        final String accountName) {
        Intent deviceRegIntent = new Intent(App.REGISTER_ACTION);
        deviceRegIntent.putExtra(App.DEVICE_REGID_KEY, regId);
        deviceRegIntent.putExtra(App.ACCOUNT_KEY, accountName);
        context.startService(deviceRegIntent);
    }
}
