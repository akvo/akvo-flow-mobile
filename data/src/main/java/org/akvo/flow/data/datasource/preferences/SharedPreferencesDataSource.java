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
import android.support.annotation.Nullable;

import org.akvo.flow.data.util.GsonMapper;
import org.akvo.flow.domain.entity.ApkData;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;

@Singleton
public class SharedPreferencesDataSource {

    private static final String KEY_CELL_UPLOAD = "data.cellular.upload";
    private static final boolean DEFAULT_VALUE_CELL_UPLOAD = false;
    private static final String KEY_BACKEND_SERVER = "backend.server";
    private static final String KEY_APK_DATA = "apk_data";
    private static final String KEY_APP_UPDATE_LAST_NOTIFIED = "update_notified_last_time";

    private final GsonMapper gsonMapper;
    private final SharedPreferences preferences;

    @Inject
    public SharedPreferencesDataSource(SharedPreferences prefs, GsonMapper gsonMapper) {
        this.preferences = prefs;
        this.gsonMapper = gsonMapper;
    }

    public Observable<Boolean> mobileSyncEnabled() {
        return Observable.just(getBoolean(KEY_CELL_UPLOAD, DEFAULT_VALUE_CELL_UPLOAD));
    }

    public Observable<String> getBaseUrl() {
        return Observable.just(getString(KEY_BACKEND_SERVER, null));
    }

    public String getString(String key, String defValue) {
        return preferences.getString(key, defValue);
    }

    private void setString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    public void setBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    public long getLong(String key, long defValue) {
        return preferences.getLong(key, defValue);
    }

    private void setLong(String key, long value) {
        preferences.edit().putLong(key, value).apply();
    }

    public int getInt(String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    public void setInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }

    private void removePreference(String key) {
        preferences.edit().remove(key).apply();
    }

    public Observable<Boolean> saveApkData(ApkData apkData) {
        setString(KEY_APK_DATA, gsonMapper.write(apkData, ApkData.class));
        return Observable.just(true);
    }

    public Observable<Boolean> clearAppUpdateNotified() {
        removePreference(KEY_APP_UPDATE_LAST_NOTIFIED);
        return Observable.just(true);
    }

    @Nullable
    public Observable<ApkData> getApkData() {
        String apkDataString = preferences.getString(KEY_APK_DATA, null);
        if (apkDataString == null) {
            return null;
        }
        return Observable.just(gsonMapper.read(apkDataString, ApkData.class));
    }

    public Observable<Boolean> saveAppUpdateNotifiedTime() {
        setLong(KEY_APP_UPDATE_LAST_NOTIFIED, System.currentTimeMillis());
        return Observable.just(true);
    }
}
