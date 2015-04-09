package org.akvo.flow.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Prefs is a SharedPreferences wrapper, with utility methods to
 * access and edit key/value pairs.
 */
public class Prefs {
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_LANGUAGES_PRESENT = "languages_present";
    public static final String KEY_DATA_ENABLED = "data_enabled";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_SEND_BEACONS = "send_beacons";
    public static final String KEY_SCREEN_ON = "screen_on";
    public static final String KEY_MAX_IMG_SIZE = "max_img_size";
    public static final String KEY_APP_ID = "app_id";
    public static final String KEY_APP_SERVER = "app_server";
    public static final String KEY_LOCALE = "locale";
    public static final String KEY_DEVICE_ID = "device_id";

    // Default preferences
    public static final boolean DEFAULT_DATA_ENABLED = true;
    public static final boolean DEFAULT_SEND_BEACONS = true;
    public static final boolean DEFAULT_SCREEN_ON = true;
    public static final int DEFAULT_MAX_IMG_SIZE = ConstantUtil.IMAGE_SIZE_320_240;

    private static final String PREFS_NAME = "flow_prefs";
    private static final int PREFS_MODE = Context.MODE_PRIVATE;

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, PREFS_MODE);
    }

    public static String getString(Context context, String key, String defValue) {
        return getPrefs(context).getString(key, defValue);
    }

    public static void setString(Context context, String key, String value) {
        getPrefs(context).edit().putString(key, value).commit();
    }

    public static boolean getBoolean(Context context, String key, boolean defValue) {
        return getPrefs(context).getBoolean(key, defValue);
    }

    public static void setBoolean(Context context, String key, boolean value) {
        getPrefs(context).edit().putBoolean(key, value).commit();
    }

    public static long getLong(Context context, String key, long defValue) {
        return getPrefs(context).getLong(key, defValue);
    }

    public static void setLong(Context context, String key, long value) {
        getPrefs(context).edit().putLong(key, value).commit();
    }

    public static int getInt(Context context, String key, int defValue) {
        return getPrefs(context).getInt(key, defValue);
    }

    public static void setInt(Context context, String key, int value) {
        getPrefs(context).edit().putInt(key, value).commit();
    }

}
