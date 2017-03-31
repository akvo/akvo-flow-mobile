/*
 *  Copyright (C) 2013-2017 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.app;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.data.migration.FlowMigrationListener;
import org.akvo.flow.data.migration.languages.MigrationLanguageMapper;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.database.UserColumns;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.User;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerApplicationComponent;
import org.akvo.flow.injector.module.ApplicationModule;
import org.akvo.flow.service.ApkUpdateService;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.logging.SentryHelper;

import java.util.Arrays;
import java.util.Locale;

public class FlowApp extends Application {
    private static FlowApp app;// Singleton

    //TODO: use shared pref?
    private Locale mLocale;

    private User mUser;
    private long mSurveyGroupId;// Hacky way of filtering the survey group in Record search
    private Prefs prefs;

    private ApplicationComponent applicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeInjector();
        prefs = new Prefs(getApplicationContext());
        initLogging();
        init();
        startUpdateService();
        app = this;
    }

    private void startUpdateService() {
        ApkUpdateService.scheduleFirstTask(this);
    }

    private void initializeInjector() {
        this.applicationComponent =
                DaggerApplicationComponent.builder().applicationModule(new ApplicationModule(this)).build();
        this.applicationComponent.inject(this);
    }

    public ApplicationComponent getApplicationComponent() {
        return this.applicationComponent;
    }

    private void initLogging() {
        SentryHelper helper = new SentryHelper(this);
        helper.initDebugTree();
        helper.initSentry();
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
        //TODO: only set the language if it is different than the device locale
        setAppLanguage(language, false);

        loadLastUser();

        // Load last survey group
        mSurveyGroupId = prefs.getLong(Prefs.KEY_SURVEY_GROUP_ID, SurveyGroup.ID_NONE);
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
        Context context = getApplicationContext();
        SurveyDbAdapter database = new SurveyDbAdapter(context,
                new FlowMigrationListener(prefs, new MigrationLanguageMapper(context)));
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

}
