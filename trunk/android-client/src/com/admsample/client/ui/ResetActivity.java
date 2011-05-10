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

package com.admsample.client.ui;

import com.admsample.client.Policy;
import com.admsample.client.Prefs;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

/**
 * Entry point to reset app to it's original state.
 * 
 */
public class ResetActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.getEditor(this).clear().commit();
        getSharedPreferences("com.google.android.c2dm", Context.MODE_PRIVATE)
            .edit().clear().commit();
        (new Policy(this)).deactiveAdmin();
        finish();
    }
}
