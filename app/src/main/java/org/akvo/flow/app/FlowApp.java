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
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.UserColumns;
import org.akvo.flow.domain.User;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.Prefs;

import java.util.Arrays;
import java.util.Locale;

public class FlowApp extends Application {
    private static FlowApp app;// Singleton

    private Locale mLocale;
    private User mUser;
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
        loadLastUser();
    }
    
    public void setUser(User user) {
        mUser = user;
    }
    
    public User getUser() {
        return mUser;
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

    /**
     * Checks if the user preference to persist logged-in users is set and, if
     * so, loads the last logged-in user from the DB
     */
    private void loadLastUser() {
        // First check if they want to keep users logged in
        if (Prefs.getBoolean(this, Prefs.KEY_KEEP_LOGIN, Prefs.DEFAULT_KEEP_LOGIN)) {
            long id = Prefs.getLong(this, Prefs.KEY_USER_ID, -1);
            SurveyDbAdapter database = new SurveyDbAdapter(FlowApp.this);
            database.open();
            Cursor cur = database.getUser(id);
            if (cur != null) {
                String userName = cur.getString(cur.getColumnIndexOrThrow(UserColumns.NAME));
                String email = cur.getString(cur.getColumnIndexOrThrow(UserColumns.EMAIL));
                mUser = new User(id, userName, email);
                cur.close();
            }
            database.close();
        }
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

}
