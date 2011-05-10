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

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.admsample.client.App;
import com.admsample.client.Policy;
import com.admsample.client.Prefs;
import com.admsample.client.PushProcessor;
import com.admsample.client.R;
import com.admsample.client.RemoteRequestProcessor;
import com.admsample.client.R.id;
import com.admsample.client.R.layout;
import com.admsample.client.R.menu;
import com.admsample.client.R.string;
import com.admsample.client.util.Helper;
import com.google.android.c2dm.C2DMessaging;

public class SetupActivity extends Activity {
    public static final String AUTH_PERMISSION_ACTION = "com.admsample.client.AUTH_PERMISSION";
    
    private boolean mPendingAuth = false;
    private int mScreenId = -1;
    private int mAccountSelectedPosition = 0;
    private String mAccountSelected;
    private ImageView mSetupProgressImage;
    private boolean mOnline = true;
    private String mAccounts[];
    
    private static final String SCREEN_ID_KEY = "savedScreenId";
    private static final String ACCOUNT_POSITION_KEY = "savedAcctPosId";
    private static final int STAGE_FETCH_POLICY = 0;
    private static final int STAGE_FETCH_POLICY_RETURN = 1;
    private static final int STAGE_UNPROTECTED = 2;
    private static final int STAGE_PROTECTED = 3;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Restores previously saved states, if any.
        if (savedInstanceState != null) {
            mAccountSelectedPosition = savedInstanceState.getInt(ACCOUNT_POSITION_KEY, 0);
        }

        SharedPreferences prefs = Prefs.get(this);
        int savedScreenId = prefs.getInt(SCREEN_ID_KEY, -1);
        if (savedScreenId == -1) {
            setScreenContent(R.layout.intro);
        } else if (prefs.getBoolean(PushProcessor.PENDING_POLICY, false)) {
            // Pending policy found, jump directly to the policy screen.
            setScreenContent(R.layout.account_policy);
        } else {
            setScreenContent(savedScreenId);
        }
        registerReceiver(mUpdateUIReceiver, new IntentFilter(App.UPDATE_DONE_ACTION));
        registerReceiver(mAuthPermissionReceiver, new IntentFilter(AUTH_PERMISSION_ACTION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resuming from the system account authentication dialog box.
        if (mPendingAuth) {
            // Pause here.  Wait for user to click on sync policy.
            mPendingAuth = false;
            return;
        }
        if (mScreenId == R.layout.account_policy) {
            showPolicyContent();
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        showPolicyContent();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mAuthPermissionReceiver);
        unregisterReceiver(mUpdateUIReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflator = this.getMenuInflater();
        inflator.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.register_push) {
            C2DMessaging.register(this, App.PUSH_MSG_SENDER_ID);
            return true;
        }
        if (item.getItemId() == R.id.online_offline) {
            if (mOnline) {
                mOnline = false;
                item.setTitle(R.string.online_mode);
                Toast.makeText(this,
                               R.string.offline_mode_on,
                               Toast.LENGTH_SHORT).show();
            } else {
                mOnline = true;
                item.setTitle(R.string.offline_mode);
                Toast.makeText(this,
                               R.string.online_mode_on,
                               Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAccountSelectedPosition != 0) {
            outState.putInt(ACCOUNT_POSITION_KEY, mAccountSelectedPosition);
        }
    }
    
    private void setScreenContent(int screenId) {
        mScreenId = screenId;
        setContentView(screenId);
        switch (screenId) {
            case R.layout.intro: {
                setIntroScreenContent();
                break;
            }
            case R.layout.select_account: {
                setSelectAccountScreenContent();
                break;
            }
            case R.layout.account_policy: {
                initViews();
                setAccountPolicyUI();
                break;
            }
        }
        SharedPreferences.Editor editor = Prefs.getEditor(this);
        editor.putInt(SCREEN_ID_KEY, screenId);
        editor.commit();
    }

    private void setIntroScreenContent() {
        TextView textView = (TextView) findViewById(R.id.intro_text);
        textView.setText(Html.fromHtml(getString(R.string.intro_text)));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        
        // Set up nav buttons.
        Button backButton = (Button) findViewById(R.id.back);
        if (backButton != null) {
            backButton.setVisibility(View.INVISIBLE);
        }

        Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setScreenContent(R.layout.select_account);
            }
        });
    }
    
    private void setSelectAccountScreenContent() {
        // Set up nav buttons.
        final Button backButton = (Button) findViewById(R.id.back);
        backButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setScreenContent(R.layout.intro);
            }
        });
        final Context ctx = this;
        final Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ListView listView = (ListView) findViewById(R.id.select_account);
                mAccountSelectedPosition = listView.getCheckedItemPosition();
                mAccountSelected = mAccounts[mAccountSelectedPosition];
                SharedPreferences.Editor editor = Prefs.getEditor(ctx);
                editor.putString(App.ACCOUNT_KEY, mAccountSelected).commit();
                backButton.setEnabled(false);
                nextButton.setEnabled(false);
                setScreenContent(R.layout.account_policy);
            }
        });
        
        mAccounts = getGoogleAccounts();
        if (mAccounts.length == 0) {
            TextView promptText = (TextView) findViewById(R.id.select_text);
            promptText.setText(R.string.no_accounts);
            TextView nextText = (TextView) findViewById(R.id.click_next_text);
            nextText.setVisibility(TextView.INVISIBLE);
            nextButton.setEnabled(false);
        } else {
            mAccountSelected = mAccounts[mAccountSelectedPosition];
            ListView listView = (ListView) findViewById(R.id.select_account);
            listView.setAdapter(new ArrayAdapter<String>(this,
                    R.layout.account, mAccounts));
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setItemChecked(mAccountSelectedPosition, true); 
        }
    }
    
    private void setAccountPolicyUI() {
        // Set up nav buttons.
        final Button backButton = (Button) findViewById(R.id.back);
        backButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Prefs.getEditor(SetupActivity.this).remove(PushProcessor.PENDING_POLICY).commit();
                setScreenContent(R.layout.select_account);
            }
        });
        final Context ctx = this;
        final Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(ctx, ProtectedActivity.class));
            }
        });
        TextView textView = (TextView) findViewById(R.id.account_name);
        if (mAccountSelected == null) {
            mAccountSelected = Prefs.get(this).getString(App.ACCOUNT_KEY, null);
        }
        textView.setText(mAccountSelected);
        backButton.setEnabled(true);
        nextButton.setEnabled(false);
        
        // Enabling the 'Sync Policy' button..
        final Button actionButton = (Button) findViewById(R.id.action_button);
        setupButton(actionButton, View.VISIBLE, true, R.string.sync_policy,
            syncPolicyListener);
        final TextView statusText = (TextView) findViewById(R.id.status_text);
        statusText.setText(R.string.click_sync_policy_text);
        statusText.setVisibility(View.VISIBLE);
    }
    
    private void setupButton(final Button button, final int visibility, final boolean enabled,
        final int resText, final View.OnClickListener listener) {
        if (button == null) return;
        button.setVisibility(visibility);
        if (visibility == View.INVISIBLE) return;
        button.setEnabled(enabled);
        button.setText(resText);
        button.setOnClickListener(listener);
    }
    
    protected void syncPolicy(String accountName) {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        TextView textView = (TextView) findViewById(R.id.status_text);
        textView.setText(R.string.syncing_text);
        textView.setVisibility(ProgressBar.VISIBLE);
        setProgressImage(STAGE_FETCH_POLICY);
        if (mOnline) {
            RemoteRequestProcessor.getPolicy(this, accountName);
        } else {
            // Offline mode.  Delay firing of completion intent by 2 seconds to simulate
            // network latency.
            Intent intent = new Intent(App.UPDATE_DONE_ACTION);
            intent.putExtra(App.STATUS_EXTRA, App.UPDATED_POLICY_STATUS);
            OfflineHelper.getHardcodedPolicy(this).writeToLocal();
            PendingIntent pi =
                PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 2000, pi);
        }
    }

    private String[] getGoogleAccounts() {
        ArrayList<String> accountNames = new ArrayList<String>();
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) {
            if (account.type.equals("com.google")) {
                accountNames.add(account.name);
            }
        }

        String[] result = new String[accountNames.size()];
        accountNames.toArray(result);
        return result;
    }
    
    private final BroadcastReceiver mUpdateUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mScreenId == R.layout.account_policy) {
                handlePolicyUpdate(intent.getIntExtra(App.STATUS_EXTRA,
                                                      App.ERROR_STATUS));
            }
        }
    };
    
    private final BroadcastReceiver mAuthPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getBundleExtra("AccountManagerBundle");
            if (extras != null) {
                Intent authIntent = (Intent) extras.get(AccountManager.KEY_INTENT);
                if (authIntent != null) {
                    mPendingAuth = true;
                    startActivity(authIntent);
                }
            }
        }
    };

    private void initViews () {
        mSetupProgressImage = (ImageView) findViewById(R.id.setup_progress);
    }
    
    private void setProgressImage(final int stage) {
        if (mSetupProgressImage == null) return;
        mSetupProgressImage.setImageLevel(stage);
    }
    
    protected void handlePolicyUpdate(int status) {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        int textResId = -1;
        switch (status) {
        case (App.UPDATED_POLICY_STATUS):
            final String C2DMRegId = C2DMessaging.getRegistrationId(this);
            if ("".equals(C2DMRegId)) {
                C2DMessaging.register(this, App.PUSH_MSG_SENDER_ID);
            }
            // Displays policy content.
            setProgressImage(STAGE_FETCH_POLICY_RETURN);
            showPolicyContent();
            break;
        case (App.AUTH_ERROR_STATUS):
        case (App.EMPTY_POLICY_STATUS):    
            textResId = R.string.user_no_policy;
            break;
        case (App.INVALID_POLICY_STATUS):
            textResId = R.string.policy_parse_error;
            break;
        case (App.REGISTERED_STATUS):
            // Show a notification.
            Helper.sendNotification(this, App.C2DM_REG_NOTIFICATION_ID,
                getResources().getString(R.string.push_reg_ok), null);
            break;
        case (App.PENDING_AUTH):
            textResId = R.string.click_sync_policy_text;
            break;            
        default:
            textResId = R.string.policy_error;
        }
        if (textResId != -1) {
            TextView connectingText = (TextView) findViewById(R.id.status_text);
            connectingText.setText(textResId);
        }
        ((Button) findViewById(R.id.back)).setEnabled(true);
    }
    
    private final View.OnClickListener activateListener = new View.OnClickListener() {
        public void onClick(View v) {
            activatePolicy();
            final Button actionButton = (Button) findViewById(R.id.action_button);
            actionButton.setEnabled(false);
            actionButton.setOnClickListener(enforcePasswordListener);
            final TextView statusText = (TextView) findViewById(R.id.status_text);
            statusText.setVisibility(View.INVISIBLE); 
        }
    };
    
    private final View.OnClickListener syncPolicyListener = new View.OnClickListener() {
        public void onClick(View v) {
            syncPolicy(mAccountSelected);
        }
    };

    private final View.OnClickListener enforcePasswordListener = new View.OnClickListener() {
        public void onClick(View v) {
            enforcePasswordPolicy();
        }
    };
    
    private void activatePolicy() {
        Policy policy = new Policy(this);
        policy.readFromLocal();
        if (policy.enforceAdmin() == false) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, policy.getPolicyAdmin());
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                String.format(getResources().getString(R.string.device_admin_activate_explain),
                              mAccountSelected));
            startActivityForResult(intent, Policy.ADD_DEVICE_ADMIN_REQUEST);
            return;
        }
        if (policy.enforcePasswordLength() == false) {
            // Enabling the 'Enforce Password Policy' button..
            final Button actionButton = (Button) findViewById(R.id.action_button);
            setupButton(actionButton, View.VISIBLE, true, R.string.enforce_pw,
                    enforcePasswordListener);
            final TextView statusText = (TextView) findViewById(R.id.status_text);
            statusText.setText(R.string.enforce_pw_text);
            statusText.setVisibility(View.VISIBLE); 
        }
    }
    
    private void enforcePasswordPolicy() {
        Policy policy = new Policy(this);
        policy.readFromLocal();
        if (policy.enforcePasswordLength() == false) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
            startActivityForResult(intent, Policy.SET_NEW_PASSWORD_REQUEST);
            return;
        }
        // Show a notification.
        Resources res = getResources();
        Helper.sendNotification(this, App.POLICY_ACTIVATION_ID,
                             res.getString(R.string.security_policy),
                             res.getString(R.string.activated));
        Prefs.getEditor(this).remove(PushProcessor.PENDING_POLICY).commit();
    }
        
    private void showPolicyContent() {
        Policy policy = new Policy(this);
        policy.readFromLocal();
        
        final TextView passwordLengthValue =
            (TextView) findViewById(R.id.password_length_policy);
        final TextView passwordTypeValue =
            (TextView) findViewById(R.id.password_type_policy);
        final ImageView passwordLengthStatus =
            (ImageView) findViewById(R.id.password_length_status);
        final ImageView passwordTypeStatus =
            (ImageView) findViewById(R.id.password_type_status);
        final Button actionButton = (Button) findViewById(R.id.action_button);
        // We have not fetched the policy.
        if (policy.getPasswordType() == null) {
            passwordLengthValue.setText(R.string.na);
            passwordTypeValue.setText(R.string.na);
            passwordLengthStatus.setImageLevel(Policy.STATUS_UNKNOWN);
            passwordTypeStatus.setImageLevel(Policy.STATUS_UNKNOWN);
            setProgressImage(STAGE_FETCH_POLICY);
            // Enabling the 'Sync Policy' button..
            setupButton(actionButton, View.VISIBLE, true, R.string.sync_policy,
                syncPolicyListener);
            return;
        }
        
        passwordLengthValue.setText(String.valueOf(policy.getPasswordLength()));
        passwordTypeValue.setText(policy.getPasswordType());
        // Got the policy, but the app has not been activated as a device administrator.
        if (!policy.isAdminActive()) {
            passwordLengthStatus.setImageLevel(Policy.STATUS_UNKNOWN);
            passwordTypeStatus.setImageLevel(Policy.STATUS_UNKNOWN);
            // Enabling the 'Activate' button..            
            setupButton(actionButton, View.VISIBLE, true, R.string.activate,
                activateListener);
            final TextView statusText = (TextView) findViewById(R.id.status_text);
            statusText.setText(R.string.activate_text);
            statusText.setVisibility(View.VISIBLE);
            ((Button) findViewById(R.id.next)).setEnabled(false);
            
        } else {
            if (policy.enforcePasswordLength()) {
                passwordLengthStatus.setImageLevel(Policy.STATUS_OK);
                passwordTypeStatus.setImageLevel(Policy.STATUS_OK);
                setProgressImage(STAGE_PROTECTED);
                actionButton.setVisibility(View.INVISIBLE);
                final Button nextButton = (Button) findViewById(R.id.next);
                nextButton.setEnabled(true);
                final TextView statusText = (TextView) findViewById(R.id.status_text);
                statusText.setText(R.string.device_secured);
                statusText.setVisibility(View.VISIBLE);
            } else {
                passwordLengthStatus.setImageLevel(Policy.STATUS_WARNING);
                passwordTypeStatus.setImageLevel(Policy.STATUS_WARNING);
                setProgressImage(STAGE_UNPROTECTED);
                this.setupButton(actionButton, View.VISIBLE, true, R.string.enforce_pw,
                    enforcePasswordListener);
                final TextView statusText = (TextView) findViewById(R.id.status_text);
                statusText.setText(R.string.enforce_pw_text);
                statusText.setVisibility(View.VISIBLE);
                ((Button) findViewById(R.id.next)).setEnabled(false);
            }
        }
    }
    
    private static class OfflineHelper {
        public static Policy getHardcodedPolicy(final Context ctx) {
            Policy policy = new Policy(ctx);
            policy.setPasswordLength(10);
            policy.setPasswordType(Policy.PASSWORD_PIN);
            policy.setRemoteLock(Boolean.TRUE);
            return policy;
        }
    }
}
