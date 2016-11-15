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
*/

package org.akvo.flow.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.akvo.flow.api.service.ApkApiService;
import org.akvo.flow.domain.apkupdate.ApkData;
import org.akvo.flow.domain.apkupdate.ApkUpdateMapper;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.util.ApkUpdateHelper;
import org.akvo.flow.util.Prefs;
import org.akvo.flow.util.StatusUtil;
import org.json.JSONObject;

/**
 * This background service will check the rest api for a new version of the APK.
 * If found, it will display a notification, requesting permission to download and
 * installAppUpdate it. After clicking the notification, the app will download and installAppUpdate
 * the new APK.
 *
 * @author Christopher Fagiani
 */
public class ApkUpdateService extends IntentService {

    private static final String TAG = "APK_UPDATE_SERVICE";

    /**
     * FIXME: once updated to a later version of play services we can use
     *
     * @see GcmTaskService which runs on a provided interval so no need to check ourselves
     *
     * For now this is 24hours in ms
     **/
    public static final int RUN_INTERVAL_IN_MS = 1 * 60 * 60 * 24 * 1000;
    public static final int INVALID_TIMESTAMP = -1;

    private final ApkApiService apkApiService = new ApkApiService();
    private final ApkUpdateMapper apkUpdateMapper = new ApkUpdateMapper();
    private final Navigator navigator = new Navigator();

    public ApkUpdateService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler.getInstance());
        checkUpdates();
    }

    /**
     * Check if new FLOW versions are available to installAppUpdate. If a new version is available,
     * we display a notification, requesting the user to download it.
     */
    private void checkUpdates() {
        if (userHasSeenUpdateActivityToday(this) && !StatusUtil.hasDataConnection(this)) {
            Log.d(TAG, "No internet connection. Can't perform the requested operation");
            return;
        }

        try {
            JSONObject json = apkApiService.getApkDataObject(this);
            ApkData data = apkUpdateMapper.transform(json);
            if (ApkUpdateHelper.shouldAppBeUpdated(data, this)) {
                // There is a newer version. Fire the 'Download and Install' Activity.
                navigator.navigateToAppUpdate(this, data);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not call apk version service", e);
            PersistentUncaughtExceptionHandler.recordException(e);
        }
    }

    private boolean userHasSeenUpdateActivityToday(Context context) {
        long lastSeen = Prefs.getLong(context, Prefs.KEY_UPDATE_ACTIVITY_LAST_SEEN_TIME_MS, INVALID_TIMESTAMP);
        return (lastSeen != INVALID_TIMESTAMP) && (System.currentTimeMillis() - lastSeen > RUN_INTERVAL_IN_MS);
    }
}
