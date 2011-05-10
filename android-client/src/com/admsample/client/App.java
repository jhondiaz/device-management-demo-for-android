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

public class App {
    public static final String GET_POLICY_ACTION = "com.admsample.client.GET_POLICY";
    public static final String REGISTER_ACTION = "com.admsample.client.REGISTER_DEVICE";
    public static final String UNREGISTER_ACTION = "com.admsample.client.UNREGISTER_DEVICE";
    public static final String UPDATE_DONE_ACTION = "com.admsample.client.UPDATE_DONE";
    
    public static final String ACCOUNT_KEY = "accountName";
    public static final String DEVICE_REGID_KEY = "deviceRegistrationId";
    
    public static final String STATUS_EXTRA = "Status";
    
    // Customize these values for your own environment.
    // C2DM developer account.
    public static final String PUSH_MSG_SENDER_ID = "C2DM@ACCOUNT";
    // Policy server URL.
    public static final String SERVER_BASE_URL = "https://POLICY_SERVER_URL";
    
    public static final String PUSH_COMMAND = "command";
    public static final String PUSH_LOCK = "lock";
    public static final String PUSH_POLICY = "syncPolicy";
    
    
    public static final int UPDATED_POLICY_STATUS = 0;
    public static final int EMPTY_POLICY_STATUS = 1;
    public static final int INVALID_POLICY_STATUS = 2;
    public static final int AUTH_ERROR_STATUS = 3;
    public static final int ERROR_STATUS = 4;
    public static final int REGISTERED_STATUS = 5;
    public static final int UNREGISTERED_STATUS = 6;
    public static final int PENDING_AUTH = 7;
    
    // Notification IDs.
    public static final int C2DM_REG_NOTIFICATION_ID = 1;
    public static final int POLICY_ACTIVATION_ID = 2;
    public static final int POLICY_ENFORCEMENT_ID = 3;
}