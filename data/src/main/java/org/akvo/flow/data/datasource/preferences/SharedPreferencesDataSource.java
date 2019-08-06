/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.domain.entity.ApkData;
import org.akvo.flow.domain.util.GsonMapper;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.Nullable;
import io.reactivex.Observable;

@Singleton
public class SharedPreferencesDataSource {

    private static final String KEY_LOCALE = "pref.locale";
    private static final String KEY_SCREEN_ON = "screen.keepon";
    private static final String KEY_DEVICE_IDENTIFIER = "device.identifier";
    private static final String KEY_MAX_IMG_SIZE = "media.img.maxsize";
    private static final String KEY_DATA_PUBLISH_TIME = "data_publish_time";
    private static final String KEY_SETUP = "setup";

    private static final String DEFAULT_VALUE_DEVICE_IDENTIFIER = "unset";
    private static final int DEFAULT_VALUE_IMAGE_SIZE = 0;
    private static final boolean DEFAULT_VALUE_SCREEN_ON = true;
    private static final String KEY_CELL_UPLOAD = "data.cellular.upload";
    private static final String KEY_SURVEY_GROUP_ID = "surveyGroupId";
    private static final String KEY_USER_ID = "userId";
    private static final boolean DEFAULT_VALUE_CELL_UPLOAD = false;
    private static final long LONG_VALUE_UNSET = -1;
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

    public Observable<Long> getSelectedSurvey() {
        return Observable.just(getLong(KEY_SURVEY_GROUP_ID, LONG_VALUE_UNSET));
    }

    public Observable<Boolean> setSelectedSurvey(long surveyId) {
        setLong(KEY_SURVEY_GROUP_ID, surveyId);
        return Observable.just(true);
    }

    public Observable<Boolean> clearSelectedSurvey() {
        return setSelectedSurvey(LONG_VALUE_UNSET);
    }

    public Observable<Long> getPublishDataTime() {
        return Observable.just(getLong(KEY_DATA_PUBLISH_TIME, LONG_VALUE_UNSET));
    }

    public Observable<Boolean> setPublishDataTime() {
        setLong(KEY_DATA_PUBLISH_TIME, System.currentTimeMillis());
        return Observable.just(true);
    }

    public Observable<Boolean> clearPublishDataTime() {
        setLong(KEY_DATA_PUBLISH_TIME, LONG_VALUE_UNSET);
        return Observable.just(true);
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

    public Observable<Long> getSelectedUser() {
        return Observable.just(getLong(KEY_USER_ID, LONG_VALUE_UNSET));
    }

    public Observable<Boolean> setSelectedUser(long userId) {
        setLong(KEY_USER_ID, userId);
        return Observable.just(true);
    }

    public Observable<Boolean> clearSelectedUser() {
        return setSelectedUser(LONG_VALUE_UNSET);
    }

    public Observable<Boolean> isDeviceSetup() {
        return Observable.just(preferences.getBoolean(KEY_SETUP, false));
    }

    public Observable<Boolean> clearUserPreferences() {
        clearSelectedSurvey();
        clearSelectedUser();
        clearSetUp();
        clearPublishDataTime();
        return Observable.just(true);
    }

    public Observable<Boolean> mobileUploadSet() {
        return Observable.just(preferences.contains(KEY_CELL_UPLOAD));
    }

    @Nullable
    public Observable<ApkData> getApkData() {
        String apkDataString = preferences.getString(KEY_APK_DATA, null);
        if (apkDataString == null) {
            return Observable.just(ApkData.NOT_SET_VALUE);
        }
        return Observable.just(gsonMapper.read(apkDataString, ApkData.class));
    }

    public Observable<Boolean> setApkData(ApkData apkData) {
        setString(KEY_APK_DATA, gsonMapper.write(apkData, ApkData.class));
        return Observable.just(true);
    }

    public Observable<Long> getAppUpdateNotifiedTime() {
        return Observable.just(getLong(KEY_APP_UPDATE_LAST_NOTIFIED, LONG_VALUE_UNSET));
    }

    public Observable<Boolean> setAppUpdateNotifiedTime() {
        setLong(KEY_APP_UPDATE_LAST_NOTIFIED, System.currentTimeMillis());
        return Observable.just(true);
    }

    public Observable<Boolean> clearAppUpdateNotified() {
        removePreference(KEY_APP_UPDATE_LAST_NOTIFIED);
        return Observable.just(true);
    }


    protected String getString(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    protected void setString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    private boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    private void setBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    private long getLong(String key, long defValue) {
        return preferences.getLong(key, defValue);
    }

    private void setLong(String key, long value) {
        preferences.edit().putLong(key, value).apply();
    }

    private int getInt(String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    private void setInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }

    private void clearSetUp() {
        removePreference(KEY_SETUP);
    }

    private void removePreference(String key) {
        preferences.edit().remove(key).apply();
    }
}
