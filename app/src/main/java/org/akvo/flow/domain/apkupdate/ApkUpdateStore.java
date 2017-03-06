/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.apkupdate;

import android.support.annotation.Nullable;

import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;

//TODO: this should be moved to data package
public class ApkUpdateStore {

    public static final String KEY_APK_DATA = "apk_data";
    public static final String KEY_APP_UPDATE_LAST_NOTIFIED = "update_notified_last_time";
    public static final long NOT_NOTIFIED = -1;

    private final GsonMapper gsonMapper;
    private final Prefs preferences;

    public ApkUpdateStore(GsonMapper gsonMapper, Prefs prefs) {
        this.gsonMapper = gsonMapper;
        this.preferences = prefs;
    }

    public void updateApkData(ViewApkData apkData) {
        ViewApkData savedApkData = getApkData();
        if (savedApkData == null || PlatformUtil
                .isNewerVersion(savedApkData.getVersion(), apkData.getVersion())) {
            saveApkData(apkData);
            clearAppUpdateNotified();
        }
    }

    private void saveApkData(ViewApkData apkData) {
        preferences.setString(KEY_APK_DATA, gsonMapper.write(apkData, ViewApkData.class));
    }

    private void clearAppUpdateNotified() {
        preferences.removePreference(KEY_APP_UPDATE_LAST_NOTIFIED);
    }

    @Nullable
    public ViewApkData getApkData() {
        String apkDataString = preferences.getString(KEY_APK_DATA, null);
        if (apkDataString == null) {
            return null;
        }
        return gsonMapper.read(apkDataString, ViewApkData.class);
    }

    public void saveAppUpdateNotifiedTime() {
        preferences.setLong(KEY_APP_UPDATE_LAST_NOTIFIED, System.currentTimeMillis());
    }

    public boolean shouldNotifyNewVersion() {
        long lastNotified = preferences.getLong(KEY_APP_UPDATE_LAST_NOTIFIED, NOT_NOTIFIED);
        if (lastNotified == NOT_NOTIFIED) {
            return true;
        }
        return System.currentTimeMillis() - lastNotified
                >= ConstantUtil.UPDATE_NOTIFICATION_DELAY_IN_MS;
    }
}
