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
import android.content.Intent;

import org.akvo.flow.util.StatusUtil;

import timber.log.Timber;

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

    private final ApkUpdateHelper apkUpdateHelper = new ApkUpdateHelper();

    public ApkUpdateService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        checkUpdates();
    }

    /**
     * Check if new FLOW versions are available to installAppUpdate. If a new version is available,
     * we display a notification, requesting the user to download it.
     */
    private void checkUpdates() {
        if (!StatusUtil.hasDataConnection(this)) {
            Timber.d("No internet connection. Can't perform the requested operation");
            return;
        }

        try {
            apkUpdateHelper.shouldUpdate(this);
        } catch (Exception e) {
            Timber.e(e, "Could not call apk version service");
        }
    }
}
