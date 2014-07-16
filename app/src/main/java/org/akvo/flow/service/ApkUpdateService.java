/*
* Copyright (C) 2010-2014 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.activity.AppUpdateActivity;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StatusUtil;

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
    private static final String APK_VERSION_SERVICE_PATH = "/deviceapprest?action=getLatestVersion&deviceType=androidPhone&appCode=flowapp";

    public ApkUpdateService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler
                .getInstance());
        checkUpdates();
    }

    /**
     * Check if new FLOW versions are available to installAppUpdate. If a new version is available,
     * we display a notification, requesting the user to download it.
     */
    private void checkUpdates() {
        if (!StatusUtil.hasDataConnection(this)) {
            Log.d(TAG, "No internet connection. Can't perform the requested operation");
            return;
        }

        try {
            final String url = StatusUtil.getServerBase(this) + APK_VERSION_SERVICE_PATH;
            String response = HttpUtil.httpGet(url);
            if (!TextUtils.isEmpty(response)) {
                JSONObject json = new JSONObject(response);
                String ver = json.getString("version");
                if (!TextUtils.isEmpty(ver) && !ver.equalsIgnoreCase("null")) {
                    String location = json.getString("fileName");
                    if (PlatformUtil.isNewerVersion(PlatformUtil.getVersionName(this), ver)
                            && !TextUtils.isEmpty(location)) {
                        // There is a newer version. Fire the 'Download and Install' Activity.
                        Intent i = new Intent(this, AppUpdateActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.putExtra(AppUpdateActivity.EXTRA_URL, location);
                        i.putExtra(AppUpdateActivity.EXTRA_VERSION, ver);
                        startActivity(i);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not call apk version service", e);
            PersistentUncaughtExceptionHandler.recordException(e);
        }
    }

}
