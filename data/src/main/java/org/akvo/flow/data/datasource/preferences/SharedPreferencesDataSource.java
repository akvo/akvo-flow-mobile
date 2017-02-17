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

import android.support.annotation.Nullable;

import org.akvo.flow.data.util.GsonMapper;
import org.akvo.flow.domain.entity.ApkData;

import javax.inject.Inject;

import rx.Observable;

public class SharedPreferencesDataSource {

    public static final String KEY_APK_DATA = "apk_data";
    public static final String KEY_APP_UPDATE_LAST_NOTIFIED = "update_notified_last_time";
    public static final long NOT_NOTIFIED = -1;

    private final GsonMapper gsonMapper;
    private final Prefs preferences;

    @Inject
    public SharedPreferencesDataSource(GsonMapper gsonMapper, Prefs prefs) {
        this.gsonMapper = gsonMapper;
        this.preferences = prefs;
    }

    public Observable<Boolean> saveApkData(ApkData apkData) {
        preferences.setString(KEY_APK_DATA, gsonMapper.write(apkData, ApkData.class));
        return Observable.just(true);
    }

    public Observable<Boolean> clearAppUpdateNotified() {
        preferences.removePreference(KEY_APP_UPDATE_LAST_NOTIFIED);
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
        preferences.setLong(KEY_APP_UPDATE_LAST_NOTIFIED, System.currentTimeMillis());
        return Observable.just(true);
    }
}
