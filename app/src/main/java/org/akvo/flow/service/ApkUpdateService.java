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
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.StatusUtil;

/**
 * This background service will check the rest api for a new version of the APK.
 * If found, it will display a notification, requesting permission to download and
 * installAppUpdate it. After clicking the notification, the app will download and installAppUpdate
 * the new APK.
 *
 * @author Christopher Fagiani
 */
public class ApkUpdateService extends GcmTaskService {

    private static final String TAG = "APK_UPDATE_SERVICE";

    private final ApkUpdateHelper apkUpdateHelper = new ApkUpdateHelper();

    public static void scheduleRepeat(Context context) {
        //in this method, single Repeating task is scheduled (the target service that will be called is
        // AppUpdateService.class)
        try {
            savedUrl = SharedPreferencesHelper.getSavedUrl(context);
            if (savedUrl == null) {
                savedUrl = BaseUrlsConstants.CONFIG_FILE_URL;
            }
            PeriodicTask periodic = new PeriodicTask.Builder()
                    //specify target service - must extend GcmTaskService
                    .setService(ApkUpdateService.class)
                    //repeat every x seconds
                    .setPeriod(ConstantUtil.REPEAT_INTERVAL_IN_SECONDS)
                    //specify how much earlier the task can be executed (in seconds)
                    .setFlex(ConstantUtil.FLEX_IN_SECONDS)
                    //tag that is unique to this task (can be used to cancel task)
                    .setTag(TAG)
                    //whether the task persists after device reboot
                    .setPersisted(true)
                    //if another task with same tag is already scheduled, replace it with this task
                    .setUpdateCurrent(true)
                    //set required network state, this line is optional
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    //request that charging must be connected, this line is optional
                    .setRequiresCharging(false).build();
            GcmNetworkManager.getInstance(context).schedule(periodic);
        } catch (Exception e) {
            Log.e(TAG, "scheduleRepeat failed", e);
        }
    }

    /**
     * Cancels the repeated task
     */
    public static void cancelRepeat(Context context) {
        GcmNetworkManager.getInstance(context).cancelTask(TAG, ApkUpdateService.class);
    }

    @Override
    public void onInitializeTasks() {
        //called when app is updated to a new version, reinstalled etc.
        //you have to schedule your repeating tasks again
        super.onInitializeTasks();
        scheduleRepeat(this);
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        checkUpdates();
        return GcmNetworkManager.RESULT_SUCCESS;
    }

//    @Override
//    protected void onHandleIntent(Intent intent) {
//        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler.getInstance());
//        checkUpdates();
//    }

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
            apkUpdateHelper.shouldUpdate(this);
        } catch (Exception e) {
            Log.e(TAG, "Could not call apk version service", e);
            PersistentUncaughtExceptionHandler.recordException(e);
        }
    }
}
