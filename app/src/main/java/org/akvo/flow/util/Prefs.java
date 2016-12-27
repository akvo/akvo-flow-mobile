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
import android.support.annotation.Nullable;

import org.akvo.flow.domain.apkupdate.GsonMapper;
import org.akvo.flow.domain.apkupdate.ViewApkData;

/**
 * Prefs is a SharedPreferences wrapper, with utility methods to
 * access and edit key/value pairs.
 */
public class Prefs {
    public static final String KEY_SURVEY_GROUP_ID = "surveyGroupId";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_SETUP = "setup";
    public static final String KEY_APK_DATA = "apk_data";
    public static final String PREF_LOCALE = "pref.locale";
    public static final String CELL_UPLOAD_SETTING_KEY = "data.cellular.upload";
    public static final String SERVER_SETTING_KEY = "backend.server";
    public static final String SCREEN_ON_KEY = "screen.keepon";
    public static final String DEVICE_IDENT_KEY = "device.identifier";
    public static final String MAX_IMG_SIZE = "media.img.maxsize";

    private static final String PREFS_NAME = "flow_prefs";
    private static final int PREFS_MODE = Context.MODE_PRIVATE;

    private static GsonMapper gsonMapper = new GsonMapper();

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, PREFS_MODE);
    }

    public static String getString(Context context, String key, String defValue) {
        return getPrefs(context).getString(key, defValue);
    }

    public static void setString(Context context, String key, String value) {
        getPrefs(context).edit().putString(key, value).apply();
    }

    public static boolean getBoolean(Context context, String key, boolean defValue) {
        return getPrefs(context).getBoolean(key, defValue);
    }

    public static void setBoolean(Context context, String key, boolean value) {
        getPrefs(context).edit().putBoolean(key, value).apply();
    }

    public static long getLong(Context context, String key, long defValue) {
        return getPrefs(context).getLong(key, defValue);
    }

    public static void setLong(Context context, String key, long value) {
        getPrefs(context).edit().putLong(key, value).apply();
    }

    public static int getInt(Context context, String key, int defValue) {
        return getPrefs(context).getInt(key, defValue);
    }

    public static void setInt(Context context, String key, int value) {
        getPrefs(context).edit().putInt(key, value).apply();
    }

    public static void saveApkData(Context context, ViewApkData apkData) {
        setString(context, KEY_APK_DATA, gsonMapper.write(apkData, ViewApkData.class));
    }

    @Nullable
    public static ViewApkData getApkData(Context context) {
        String apkDataString = getString(context, KEY_APK_DATA, null);
        if (apkDataString == null) {
            return null;
        }
        return gsonMapper.read(apkDataString, ViewApkData.class);
    }

    public static void clearApkData(Context context) {
        getPrefs(context).edit().remove(KEY_APK_DATA).apply();
    }
}

