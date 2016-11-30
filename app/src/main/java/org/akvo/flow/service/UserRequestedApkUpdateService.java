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
import android.os.Handler;
import android.support.v4.util.Pair;
import android.util.Log;
import org.akvo.flow.R;
import org.akvo.flow.activity.AppUpdateActivity;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.apkupdate.ApkData;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.ViewUtil;

import javax.inject.Inject;

/**
 * This background service will check the rest api for a new version of the APK.
 * If found, it will display a notification, requesting permission to download and
 * installAppUpdate it. After clicking the notification, the app will download and installAppUpdate
 * the new APK.
 *
 * @author Christopher Fagiani
 */
public class UserRequestedApkUpdateService extends IntentService {

    private static final String TAG = "USER_REQ_APK_UPDATE";

    @Inject
    ApkUpdateHelper apkUpdateHelper;

    @Inject
    Navigator navigator;

    public UserRequestedApkUpdateService() {
        super(TAG);
    }

    private Handler uiHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
        this.uiHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler.getInstance());
        checkUpdates();
    }

    /**
     * Check if new FLOW versions are available to installAppUpdate. If a new version is available,
     * we display {@link AppUpdateActivity}, requesting the user to download it.
     */
    private void checkUpdates() {
        if (!StatusUtil.hasDataConnection(this)) {
            ViewUtil.displayToastFromService(getString(R.string.apk_update_service_error_no_internet), uiHandler,
                                             getApplicationContext());
            return;
        }

        try {
            Pair<Boolean, ApkData> result = apkUpdateHelper.shouldUpdate(this, StatusUtil.getServerBase(this));
            // There is a newer version. Fire the 'Download and Install' Activity.
            if (result.first) {
                navigator.navigateToAppUpdate(this, result.second);
            } else {
                ViewUtil.displayToastFromService(getString(R.string.apk_update_service_no_update), uiHandler,
                                                 getApplicationContext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking updates", e);
            PersistentUncaughtExceptionHandler.recordException(e);
            ViewUtil.displayToastFromService(getString(R.string.apk_update_service_error_update), uiHandler,
                                             getApplicationContext());
        }
    }
}
