/*
* Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.util.Pair;

import org.akvo.flow.R;
import org.akvo.flow.activity.AppUpdateActivity;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.domain.apkupdate.ViewApkData;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.util.ViewUtil;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * This background service will check the rest api for a new version of the APK.
 * If found, it will display a notification, requesting permission to download and
 * installAppUpdate it. After clicking the notification, the app will download and installAppUpdate
 * the new APK.
 *
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
        checkUpdates();
    }

    /**
     * Check if new FLOW versions are available to installAppUpdate. If a new version is available,
     * we display {@link AppUpdateActivity}, requesting the user to download it.
     */
    private void checkUpdates() {
        try {
            Pair<Boolean, ViewApkData> result = apkUpdateHelper.shouldUpdate();
            // There is a newer version. Fire the 'Download and Install' Activity.
            if (result.first) {
                navigator.navigateToAppUpdate(this, result.second);
            } else {
                ViewUtil.displayToastFromService(getString(R.string.apk_update_service_no_update),
                        uiHandler,
                        getApplicationContext());
            }
        } catch (Exception e) {
            Timber.e(e, "Error checking updates");
            ViewUtil.displayToastFromService(getString(R.string.apk_update_service_error_update),
                    uiHandler,
                    getApplicationContext());
        }
    }
}
