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

import org.json.JSONException;
import org.json.JSONObject;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;

import com.admsample.client.util.Helper;

public class Policy {
    private final JSONObject mJsonPolicy;
    private int passwordLength = -1;
    private String passwordType = null;
    private Boolean remoteLock = null;
    private DevicePolicyManager mDPM = null;
    private ComponentName mPolicyAdmin = null;
    private Context mCtx = null;
    
    public static final String POLICY = "policy";
    public static final String PASSWORD_LENGTH = "passwordLength";
    public static final String PASSWORD_TYPE = "passwordType";
    public static final String REMOTE_LOCK = "remoteLock";
    public static final String PASSWORD_PIN = "PIN";
    public static final String PASSWORD_ALPHA = "ALPHA";
    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_WARNING = 1;
    public static final int STATUS_OK = 2;
    
    public static final int ADD_DEVICE_ADMIN_REQUEST = 1000;
    public static final int SET_NEW_PASSWORD_REQUEST = 1001;
    
    /**
     * Create a security policy based on the input JSON.
     * 
     * @param policy JSON representation of the policy.
     * @throws JSONException
     */
    public Policy(Context ctx, String policy) throws JSONException {
        mJsonPolicy = new JSONObject(policy);
        parse();
        mCtx = ctx;
    }
    
    public Policy(Context ctx) {
        mCtx = ctx;
        mJsonPolicy = null;
        mPolicyAdmin = new ComponentName(mCtx, PolicyAdminListener.class);
        mDPM = (DevicePolicyManager) mCtx.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }
    
    public void writeToLocal() {
        SharedPreferences.Editor editor = Prefs.getEditor(mCtx);
        editor.remove(PASSWORD_LENGTH);
        editor.remove(PASSWORD_TYPE);
        editor.remove(REMOTE_LOCK);
        if (passwordLength > -1) {
            editor.putString(PASSWORD_LENGTH, String.valueOf(passwordLength));
        }
        if (passwordType != null) {
            editor.putString(PASSWORD_TYPE, passwordType);
        }
        if (remoteLock != null) {
            editor.putBoolean(REMOTE_LOCK, remoteLock);
        }
        editor.commit();    
    }
    
    public boolean enforceLock() {
        readFromLocal();
        if (remoteLock == false) {
            return false;
        }
        if (!mDPM.isAdminActive(mPolicyAdmin)) {
            return false;
        }
        mDPM.lockNow();
        return true;
    }
    
    public void readFromLocal() {
        SharedPreferences prefs = Prefs.get(mCtx);
        passwordLength = Integer.valueOf(prefs.getString(PASSWORD_LENGTH, "-1"));
        passwordType = prefs.getString(PASSWORD_TYPE, null);
        remoteLock = prefs.getBoolean(REMOTE_LOCK, false);
    }
    
    private void parse() throws JSONException {
        if (mJsonPolicy == null) {
            return;
        }
        JSONObject policy = mJsonPolicy.getJSONObject(POLICY);
        if (policy.has(PASSWORD_LENGTH)) {
            passwordLength = policy.getInt(PASSWORD_LENGTH);
        }
        if (policy.has(PASSWORD_TYPE)) {
            passwordType = policy.getString(PASSWORD_TYPE);
        }
        if (policy.has(REMOTE_LOCK)) {
            remoteLock = Boolean.valueOf(policy.getBoolean(REMOTE_LOCK));
        }
    }

    public int getPasswordLength() {
        return passwordLength;
    }

    public void setPasswordLength(int passwordLength) {
        this.passwordLength = passwordLength;
    }

    public String getPasswordType() {
        return passwordType;
    }

    public void setPasswordType(String passwordType) {
        this.passwordType = passwordType;
    }
    
    public void setRemoteLock(Boolean remoteLock) {
        this.remoteLock = remoteLock;
    }

    public Boolean getRemoteLock() {
        return (remoteLock == null ? Boolean.FALSE : remoteLock);
    }
    
    public boolean enforceAdmin() {
        if (!mDPM.isAdminActive(mPolicyAdmin)) {
            return false;
        }
        return true;
    }
    
    public boolean enforcePasswordLength() {
        if (passwordLength > -1) {
            mDPM.setPasswordMinimumLength(mPolicyAdmin, passwordLength);
        }
        if (PASSWORD_PIN.equals(passwordType)) {
            mDPM.setPasswordQuality(mPolicyAdmin,
                                    DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        } else if (PASSWORD_ALPHA.equals(passwordType)) {
            mDPM.setPasswordQuality(mPolicyAdmin,
                    DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC);
        }
        if (!mDPM.isActivePasswordSufficient()) {
            return false;
        }
        return true;
    }
    
    public boolean isAdminActive() {
        return mDPM.isAdminActive(mPolicyAdmin);
    }
    
    public boolean isActivePasswordSufficient() {
        return mDPM.isActivePasswordSufficient();
    }
    
    public boolean isSecured() {
        return isAdminActive() && isActivePasswordSufficient();
    }
    
    public boolean isEncrypted() {
        if (Build.VERSION.SDK_INT < 11) {
            return false;
        }
        final int RESULT = mDPM.getStorageEncryptionStatus();
        if (RESULT > DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE) {
            return true;
        } else {
            return false;
        }
    }
    
    public boolean deactiveAdmin() {
        if (!mDPM.isAdminActive(mPolicyAdmin)) {
            return false;
        }
        mDPM.removeActiveAdmin(mPolicyAdmin);
        return true;
    }
    
    public ComponentName getPolicyAdmin() {

        return mPolicyAdmin;
    }
    
    public static class PolicyAdminListener extends DeviceAdminReceiver {

        @Override
        public void onDisabled(Context context, Intent intent) {
            super.onDisabled(context, intent);
            // Show a notification.
            Resources res = context.getResources();
            Helper.sendNotification(context, App.POLICY_ACTIVATION_ID,
                String.format("%s: %s",
                    res.getString(R.string.security_policy),
                    res.getString(R.string.deactivated)), null);
        }
    }
}
