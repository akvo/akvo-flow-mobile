/*
 *  Copyright (C) 2013-2017 Stichting Akvo (Akvo Foundation)
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
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.data.database.UserColumns;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.User;
import org.akvo.flow.service.ApkUpdateService;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.LangsPreferenceUtil;
import org.akvo.flow.data.preference.Prefs;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class FlowApp extends Application {
    private static final String TAG = FlowApp.class.getSimpleName();
    private static FlowApp app;// Singleton

    //TODO: use shared pref?
    private Locale mLocale;

    private User mUser;
    private long mSurveyGroupId;// Hacky way of filtering the survey group in Record search
    private Prefs prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new Prefs(getApplicationContext());
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        init();
        startUpdateService();
        app = this;
    }

    private void startUpdateService() {
        ApkUpdateService.scheduleRepeat(this);
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
        String language = loadLocalePref();

        //TODO: this is not necessary as by default locale is english anyway
        if (TextUtils.isEmpty(language)) {
            language = Locale.getDefault().getLanguage();
            // Is that available in our language list?
            if (!Arrays.asList(getResources().getStringArray(R.array.app_language_codes))
                    .contains(language)) {
                language = ConstantUtil.ENGLISH_CODE;// TODO: Move this constant to @strings
            }
        }
        //TODO: only set the language if it is diferent than the device locale
        setAppLanguage(language, false);

        loadLastUser();

        // Load last survey group
        mSurveyGroupId = prefs.getLong(Prefs.KEY_SURVEY_GROUP_ID, SurveyGroup.ID_NONE);

        mSurveyChecker.run();// Ensure surveys have put their languages
    }
    
    public void setUser(User user) {
        mUser = user;
        prefs.setLong(Prefs.KEY_USER_ID, mUser != null ? mUser.getId() : -1);
    }
    
    public User getUser() {
        return mUser;
    }
    
    public void setSurveyGroupId(long surveyGroupId) {
        mSurveyGroupId = surveyGroupId;
        prefs.setLong(Prefs.KEY_SURVEY_GROUP_ID, surveyGroupId);
    }
    
    public long getSurveyGroupId() {
        return mSurveyGroupId;
    }

    public String getAppLanguageCode() {
        return mLocale.getLanguage();
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
        SurveyDbAdapter database = new SurveyDbAdapter(FlowApp.this);
        database.open();

        // Consider the app set up if the DB contains users. This is relevant for v2.2.0 app upgrades
        if (!prefs.getBoolean(Prefs.KEY_SETUP, false)) {
            prefs.setBoolean(Prefs.KEY_SETUP, database.getUsers().getCount() > 0);
        }

        long id = prefs.getLong(Prefs.KEY_USER_ID, -1);
        if (id != -1) {
            Cursor cur = database.getUser(id);
            if (cur.moveToFirst()) {
                String userName = cur.getString(cur.getColumnIndexOrThrow(UserColumns.NAME));
                mUser = new User(id, userName);
                cur.close();
            }
        }

        database.close();
    }

    public void setAppLanguage(String language, boolean requireRestart) {
        // Override system locale
        mLocale = new Locale(language);
        Locale.setDefault(mLocale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = mLocale;
        getBaseContext().getResources().updateConfiguration(config, null);

        // Save it in the preferences
        saveLocalePref(language);

        if (requireRestart) {
            Toast.makeText(this, R.string.please_restart, Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Nullable
    private String loadLocalePref() {
        return prefs.getString(Prefs.KEY_LOCALE, null);
    }

    private void saveLocalePref(String language) {
        prefs.setString(Prefs.KEY_LOCALE, language);
    }

    /**
     * Old versions of the app may not have translations support, thus they will
     * not store languages preference in the database. This task ensures that
     * any language stored in the device has properly set its languages in the
     * database, making them available to the user through the settings menu.
     */
    private final Runnable mSurveyChecker = new Runnable() {

        @Override
        public void run() {
            SurveyDbAdapter database = new SurveyDbAdapter(FlowApp.this);
            database.open();

            // We check for the key not present in old devices: 'survey.languagespresent'
            // NOTE: 'survey.language' DID exist
            if (database.getPreference(ConstantUtil.SURVEY_LANG_PRESENT_KEY) == null) {
                Log.d(TAG, "Recomputing available languages...");
                Toast.makeText(getApplicationContext(), R.string.configuring_languages,
                        Toast.LENGTH_SHORT)
                        .show();

                // First, we add the default property, to avoid null cases within
                // the process
                database.savePreference(ConstantUtil.SURVEY_LANG_SETTING_KEY, "");
                database.savePreference(ConstantUtil.SURVEY_LANG_PRESENT_KEY, "");

                // Recompute all the surveys, and store their languages
                List<Survey> surveyList = database.getSurveyList(SurveyGroup.ID_NONE);
                for (Survey survey : surveyList) {
                    String[] langs = LangsPreferenceUtil.determineLanguages(FlowApp.this, survey);
                    Log.d(TAG, "Adding languages: " + Arrays.toString(langs));
                    database.addLanguages(langs);
                }
            }

            database.close();
        }
    };

}
