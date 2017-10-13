/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.datasource.preferences;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;

@Singleton
public class SharedPreferencesDataSource {

    public static final String KEY_CELL_UPLOAD = "data.cellular.upload";
    public static final String KEY_LOCALE = "pref.locale";
    public static final String KEY_SCREEN_ON = "screen.keepon";
    public static final String KEY_DEVICE_IDENTIFIER = "device.identifier";
    public static final String KEY_MAX_IMG_SIZE = "media.img.maxsize";

    private static final boolean DEFAULT_VALUE_CELL_UPLOAD = false;
    public static final String DEFAULT_VALUE_DEVICE_IDENTIFIER = "unset";
    public static final int DEFAULT_VALUE_IMAGE_SIZE = 0;
    public static final boolean DEFAULT_VALUE_SCREEN_ON = true;

    private final SharedPreferences preferences;

    @Inject
    public SharedPreferencesDataSource(SharedPreferences prefs) {
        this.preferences = prefs;
    }

    public Observable<Boolean> mobileSyncEnabled() {
        return Observable.just(getBoolean(KEY_CELL_UPLOAD, DEFAULT_VALUE_CELL_UPLOAD));
    }

    public Observable<Boolean> keepScreenOn() {
        return Observable.just(getBoolean(KEY_SCREEN_ON, DEFAULT_VALUE_SCREEN_ON));
    }

    public Observable<String> getAppLanguage() {
        return Observable.just(getString(KEY_LOCALE, ""));
    }

    public Observable<Integer> getImageSize() {
        return Observable.just(getInt(KEY_MAX_IMG_SIZE, DEFAULT_VALUE_IMAGE_SIZE));
    }

    public Observable<String> getDeviceId() {
        return Observable.just(getString(KEY_DEVICE_IDENTIFIER, DEFAULT_VALUE_DEVICE_IDENTIFIER));
    }

    private int getInt(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    private boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    public void removePreference(String key) {
        preferences.edit().remove(key).apply();
    }

    private String getString(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    private void setString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    private void setBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    private void setInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }

    public Observable<Boolean> saveScreenOn(Boolean keepScreenOn) {
        setBoolean(KEY_SCREEN_ON, keepScreenOn);
        return Observable.just(true);
    }

    public Observable<Boolean> saveEnableMobileData(Boolean enable) {
        setBoolean(KEY_CELL_UPLOAD, enable);
        return Observable.just(true);
    }

    public Observable<Boolean> saveLanguage(String language) {
        setString(KEY_LOCALE, language);
        return Observable.just(true);
    }

    public Observable<Boolean> saveImageSize(Integer size) {
        setInt(KEY_MAX_IMG_SIZE, size);
        return Observable.just(true);
    }
}
