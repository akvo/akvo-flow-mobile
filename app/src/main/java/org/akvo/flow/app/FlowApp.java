/*
 *  Copyright (C) 2013-2015 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.app;

import android.app.Application;
import android.content.res.Configuration;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.UserColumns;
import org.akvo.flow.domain.Instance;
import org.akvo.flow.domain.User;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.Prefs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

public class FlowApp extends Application {
    private static final String TAG = FlowApp.class.getSimpleName();

    private static FlowApp app;// Singleton

    private Locale mLocale;
    private User mUser;
    private Instance mInstance;
    private long mSurveyGroupId;// Hacky way of filtering the survey group in Record search

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        app = this;
    }

    public static FlowApp getApp() {
        return app;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);

        // This config will contain system locale. We need a workaround
        // to enable our custom locale again. Note that this approach
        // is not very 'clean', but Android makes it really hard to
        // customize an application wide locale.
        if (mLocale != null && !mLocale.getLanguage().equalsIgnoreCase(
                newConfig.locale.getLanguage())) {
            // Re-enable our custom locale, using this newConfig reference
            newConfig.locale = mLocale;
            Locale.setDefault(mLocale);
            getBaseContext().getResources().updateConfiguration(newConfig, null);
        }
    }
    
    private void init() {
        // Load custom locale into the app. If the locale has not previously been configured
        // check if the device has a compatible language active. Otherwise, fall back to English
        String language = Prefs.getString(this, Prefs.KEY_LOCALE, null);
        if (TextUtils.isEmpty(language)) {
            language = Locale.getDefault().getLanguage();
            // Is that available in our language list?
            if (!Arrays.asList(getResources().getStringArray(R.array.app_language_codes))
                    .contains(language)) {
                language = ConstantUtil.ENGLISH_CODE;// TODO: Move this constant to @strings
            }
        }
        setAppLanguage(language, false);

        SurveyDbAdapter database = new SurveyDbAdapter(FlowApp.this).open();

        // Load user
        long id = Prefs.getLong(this, Prefs.KEY_USER_ID, -1);
        if (id != -1) {
            Cursor cur = database.getUser(id);
            if (cur != null && cur.getCount() > 0) {
                String userName = cur.getString(cur.getColumnIndexOrThrow(UserColumns.NAME));
                String email = cur.getString(cur.getColumnIndexOrThrow(UserColumns.EMAIL));
                mUser = new User(id, userName, email);
                cur.close();
            }
        }

        // Load instance
        String appId = Prefs.getString(this, Prefs.KEY_APP_ID, "");
        if (!TextUtils.isEmpty(appId)) {
            Cursor c = database.getInstance(appId);
            if (c != null && c.moveToFirst()) {
                mInstance = SurveyDbAdapter.getInstance(c);
                c.close();
            }
        } else {
            // Older versions of the app have a survey.properties file, with the instance properties.
            // We'll attempt to load that file, if found. This will be part of the upgrade process,
            // wherein the user will have the existing instance already available and set up.
            // Additionally, any existing file will be moved to the new app-specific directory.
            Instance i = getRawInstance();
            if (i != null && !Prefs.getBoolean(this, Prefs.KEY_UPGRADED, false)) {
                database.addInstance(i);
                FileUtil.onSingleAppUpgrade(this, i.getAppId());
                database.onSingleAppUpgrade(i.getAppId());
                setInstance(i);
                Prefs.setBoolean(this, Prefs.KEY_UPGRADED, true);
            }
        }

        database.close();
    }
    
    public void setUser(User user) {
        mUser = user;
        if (user != null) {
            Prefs.setLong(this, Prefs.KEY_USER_ID, user.getId());
        }
    }
    
    public User getUser() {
        return mUser;
    }

    public void setInstance(Instance instance) {
        mInstance = instance;
        if (instance != null) {
            Prefs.setString(this, Prefs.KEY_APP_ID, instance.getAppId());
        }
    }

    public Instance getInstance() {
        return mInstance;
    }

    public String getAppId() {
        return mInstance != null ? mInstance.getAppId() : null;
    }
    
    public void setSurveyGroupId(long surveyGroupId) {
        mSurveyGroupId = surveyGroupId;
    }
    
    public long getSurveyGroupId() {
        return mSurveyGroupId;
    }

    public String getAppDisplayLanguage() {
        String lang = mLocale.getDisplayLanguage();
        if (!TextUtils.isEmpty(lang)) {
            // Ensure the first letter is upper case
            char[] strArray = lang.toCharArray();
            strArray[0] = Character.toUpperCase(strArray[0]);
            lang = new String(strArray);
        }
        return lang;
    }

    public void setAppLanguage(String language, boolean requireRestart) {
        // Override system locale
        mLocale = new Locale(language);
        Locale.setDefault(mLocale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = mLocale;
        getBaseContext().getResources().updateConfiguration(config, null);

        // Save it in the preferences
        Prefs.setString(this, Prefs.KEY_LOCALE, language);

        if (requireRestart) {
            Toast.makeText(this, R.string.please_restart, Toast.LENGTH_LONG)
                    .show();
        }
    }

    private Instance getRawInstance() {
        //int id = getResources().getIdentifier("org.akvo.flow:raw/survey.properties", null, null);
        int id = getResources().getIdentifier("survey", "raw", getPackageName());
        if (id != 0) {
            Properties properties = new Properties();
            try {
                InputStream is = getResources().openRawResource(id);
                properties.load(is);

                String s3Bucket = properties.getProperty(ConstantUtil.S3_BUCKET);
                String s3AccessKey = properties.getProperty(ConstantUtil.S3_ACCESSKEY);
                String s3SecretKey = properties.getProperty(ConstantUtil.S3_SECRET);
                String serverBase = properties.getProperty(ConstantUtil.SERVER_BASE);
                String apiKey = properties.getProperty(ConstantUtil.API_KEY);

                // Match instance name from server base, for example:
                // https://akvoflow-X.appspot.com --> akvoflow-X
                String host = new URL(serverBase).getHost();
                if (TextUtils.isEmpty(host) || !host.contains(".")) {
                    return null;
                }
                String appId = host.substring(0, host.indexOf("."));
                return new Instance(appId, appId, serverBase, s3Bucket, s3AccessKey, s3SecretKey, apiKey);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return null;
    }

}
