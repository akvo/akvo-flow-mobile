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

import org.akvo.flow.data.migration.FlowMigrationListener;
import org.akvo.flow.data.migration.languages.MigrationLanguageMapper;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.database.UserColumns;
import org.akvo.flow.domain.User;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerApplicationComponent;
import org.akvo.flow.injector.module.ApplicationModule;
import org.akvo.flow.service.ApkUpdateService;
import org.akvo.flow.service.FileChangeTrackingService;
import org.akvo.flow.util.logging.LoggingHelper;

import java.util.Locale;

import javax.inject.Inject;

public class FlowApp extends Application {
    private static FlowApp app;// Singleton

    @Nullable
    private User mUser;

    private Prefs prefs;

    private ApplicationComponent applicationComponent;

    @Inject
    LoggingHelper loggingHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeInjector();
        prefs = new Prefs(getApplicationContext());
        initLogging();
        init();
        startUpdateService();
        app = this;
        startBootstrapFolderTracker();
        updateLoggingInfo();
    }

    private void updateLoggingInfo() {
        String username = mUser == null? null: mUser.getName();
        String deviceId = prefs.getString(Prefs.KEY_DEVICE_IDENTIFIER, null);
        loggingHelper.initLoginData(username, deviceId);
    }

    private void startBootstrapFolderTracker() {
        FileChangeTrackingService.scheduleVerifier(this);
    }

    private void startUpdateService() {
        ApkUpdateService.scheduleFirstTask(this);
    }

    private void initializeInjector() {
        this.applicationComponent =
                DaggerApplicationComponent.builder().applicationModule(new ApplicationModule(this))
                        .build();
        this.applicationComponent.inject(this);
    }

    public ApplicationComponent getApplicationComponent() {
        return this.applicationComponent;
    }

    private void initLogging() {
       loggingHelper.init();
    }

    public static FlowApp getApp() {
        return app;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // This config will contain system locale. We need a workaround
        // to enable our custom locale again. Note that this approach
        // is not very 'clean', but Android makes it really hard to
        // customize an application wide locale.
        Locale savedLocale = getSavedLocale();
        if (localeNeedsUpdating(savedLocale, newConfig.locale)) {
            // Re-enable our custom locale, using this newConfig reference
            Locale.setDefault(savedLocale);
            updateConfiguration(savedLocale, newConfig);
        }
    }

    private void init() {
        updateLocale();
        loadLastUser();
    }

    private void updateLocale() {
        Locale savedLocale = getSavedLocale();
        Locale currentLocale = Locale.getDefault();
        if (localeNeedsUpdating(savedLocale, currentLocale)) {
            Locale.setDefault(savedLocale);
            updateConfiguration(savedLocale, new Configuration());
        }
    }

    private boolean localeNeedsUpdating(Locale savedLocale, Locale currentLocale) {
        return savedLocale != null && currentLocale != null && !currentLocale.getLanguage()
                .equalsIgnoreCase(savedLocale.getLanguage());
    }

    private void updateConfiguration(Locale savedLocale, Configuration config) {
        config.locale = savedLocale;
        getBaseContext().getResources().updateConfiguration(config, null);
    }

    @Nullable
    private Locale getSavedLocale() {
        String languageCode = loadLocalePref();
        Locale savedLocale = null;
        if (!TextUtils.isEmpty(languageCode)) {
            savedLocale = new Locale(languageCode);
        }
        return savedLocale;
    }

    public void setUser(User user) {
        mUser = user;
        prefs.setLong(Prefs.KEY_USER_ID, mUser != null ? mUser.getId() : -1);
    }

    public User getUser() {
        return mUser;
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

        long id = prefs.getLong(Prefs.KEY_USER_ID, Prefs.DEFAULT_VALUE_USER_ID);
        if (id != Prefs.DEFAULT_VALUE_USER_ID) {
            Cursor cur = database.getUser(id);
            if (cur.moveToFirst()) {
                String userName = cur.getString(cur.getColumnIndexOrThrow(UserColumns.NAME));
                mUser = new User(id, userName);
                cur.close();
            }
        }

        database.close();
    }

    @Nullable
    private String loadLocalePref() {
        return prefs.getString(Prefs.KEY_LOCALE, null);
    }
}
