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

package com.admsample.server;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Policy {
    @PrimaryKey
    @Persistent
    private Key key;
    
    @Persistent
    private int passwordLength;
    
    @Persistent
    private String passwordType;
    
    @Persistent
    private Boolean remoteLock;
    
    public static final String PASSWORD_LENGTH = "passwordLength";
    public static final String PASSWORD_TYPE = "passwordType";
    public static final String REMOTE_LOCK = "remoteLock";

    public Policy(Key key, int passwordLength, String passwordType, boolean remoteLock) {
        this.key = key;
        this.passwordLength = passwordLength;
        this.passwordType = passwordType;
        this.remoteLock = new Boolean(remoteLock);
    }
    
    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
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

    public boolean getRemoteLock() {
        return remoteLock != null ? remoteLock.booleanValue() : false;
    }

    public void setRemoteLock(boolean remoteLock) {
        this.remoteLock = new Boolean(remoteLock);
    }
    
    public JSONObject toJSON() {
        JSONObject policyJson = new JSONObject();
        try {
            policyJson.put(PASSWORD_LENGTH, passwordLength);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        try {
            policyJson.put(PASSWORD_TYPE, passwordType);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        try {
            policyJson.put(REMOTE_LOCK, remoteLock);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return policyJson;
    }
}