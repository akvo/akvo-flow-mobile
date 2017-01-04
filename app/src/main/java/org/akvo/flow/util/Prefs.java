/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Prefs is a SharedPreferences wrapper, with utility methods to
 * access and edit key/value pairs.
 */
public class Prefs {

    public static final String KEY_SURVEY_GROUP_ID = "surveyGroupId";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_SETUP = "setup";

    private static final String PREFS_NAME = "flow_prefs";
    private static final int PREFS_MODE = Context.MODE_PRIVATE;

    private final Context context;

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

    public int getInt(String key, int defValue) {
        return getPrefs().getInt(key, defValue);
    }

    public void setInt(String key, int value) {
        getPrefs().edit().putInt(key, value).apply();
    }

    public void removePreference(String key) {
        getPrefs().edit().remove(key).apply();
    }
}

