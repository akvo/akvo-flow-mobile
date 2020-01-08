/*
 * Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.data.preference;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;

/**
 * Prefs is a SharedPreferences wrapper, with utility methods to
 * access and edit key/value pairs.
 */
public class Prefs {

    public static final String KEY_SURVEY_GROUP_ID = "surveyGroupId";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_SETUP = "setup";
    public static final String KEY_SCREEN_ON = "screen.keepon";
    public static final String KEY_DEVICE_IDENTIFIER = "device.identifier";
    public static final String KEY_SPACE_AVAILABLE = "cardMBAvaliable";

    private static final String PREFS_NAME = "flow_prefs";
    private static final int PREFS_MODE = Context.MODE_PRIVATE;

    public static final boolean DEFAULT_VALUE_SCREEN_ON = true;
    public static final long DEF_VALUE_SPACE_AVAILABLE = 101L;
    public static final long DEFAULT_VALUE_USER_ID = -1;

    private final Context context;

    @Inject
    public Prefs(Context context) {
        this.context = context;
    }

    private SharedPreferences getPrefs() {
        return context.getSharedPreferences(PREFS_NAME, PREFS_MODE);
    }

    public String getString(String key, String defValue) {
        return getPrefs().getString(key, defValue);
    }

    public void setString(String key, String value) {
        getPrefs().edit().putString(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defValue) {
        return getPrefs().getBoolean(key, defValue);
    }

    public void setBoolean(String key, boolean value) {
        getPrefs().edit().putBoolean(key, value).apply();
    }

    public long getLong(String key, long defValue) {
        return getPrefs().getLong(key, defValue);
    }

    public void setLong(String key, long value) {
        getPrefs().edit().putLong(key, value).apply();
    }
}

