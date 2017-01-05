/*
* Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.domain.apkupdate.ApkUpdateStore;
import org.akvo.flow.domain.apkupdate.GsonMapper;
import org.akvo.flow.domain.apkupdate.ViewApkData;
import org.akvo.flow.util.ConnectivityStateManager;
import org.akvo.flow.util.ConstantUtil;

/**
 * This background service will check the rest api for a new version of the APK.
 * If found, it will display a {@link org.akvo.flow.activity.AppUpdateActivity}, requesting
 * permission to download and installAppUpdate it.
 *
 * @author Christopher Fagiani
 */
public class ApkUpdateService extends GcmTaskService {

    /**
     * Tag that is unique to this task (can be used to cancel task)
     */
    private static final String TAG = "APK_UPDATE_SERVICE";

    private ApkUpdateHelper apkUpdateHelper;

    public static void scheduleRepeat(Context context) {
        try {
            PeriodicTask periodic = new PeriodicTask.Builder()
                    .setService(ApkUpdateService.class)
                    //repeat every x seconds
                    .setPeriod(ConstantUtil.REPEAT_INTERVAL_IN_SECONDS)
                    //specify how much earlier the task can be executed (in seconds)
                    .setFlex(ConstantUtil.FLEX_IN_SECONDS)
                    .setTag(TAG)
                    //whether the task persists after device reboot
                    .setPersisted(true)
                    //if another task with same tag is already scheduled, replace it with this task
                    .setUpdateCurrent(true)
                    //set required network state
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    //request that charging needs not be connected
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

    /**
     *  Called when app is updated to a new version, reinstalled etc.
     *  Repeating tasks have to be rescheduled
     */
    @Override
    public void onInitializeTasks() {
        super.onInitializeTasks();
        scheduleRepeat(this);
    }

    /**
     * Check if new FLOW versions are available to installAppUpdate. If a new version is available,
     * we display {@link org.akvo.flow.activity.AppUpdateActivity}, requesting the user to download
     * it.
     */
    @Override
    public int onRunTask(TaskParams taskParams) {
        Context applicationContext = getApplicationContext();
        apkUpdateHelper = new ApkUpdateHelper(applicationContext);
        ConnectivityStateManager connectivityStateManager = new ConnectivityStateManager(
                applicationContext);
        Prefs prefs = new Prefs(applicationContext);
        if (!syncOverMobileNetworksAllowed(prefs) && !connectivityStateManager.isWifiConnected()) {
            Log.d(TAG, "No available authorised connection. Can't perform the requested operation");
            return GcmNetworkManager.RESULT_SUCCESS;
        }

        try {
            Pair<Boolean, ViewApkData> booleanApkDataPair = apkUpdateHelper.shouldUpdate();
            if (booleanApkDataPair.first) {
                //save to shared preferences
                ApkUpdateStore store = new ApkUpdateStore(new GsonMapper(), prefs);
                store.updateApkData(booleanApkDataPair.second);
            }
            return GcmNetworkManager.RESULT_SUCCESS;
        } catch (Exception e) {
            Log.e(TAG, "Error with apk version service", e);
            return GcmNetworkManager.RESULT_FAILURE;
        }
    }

    private boolean syncOverMobileNetworksAllowed(Prefs prefs) {
        return prefs.getBoolean(Prefs.KEY_CELL_UPLOAD,
                Prefs.DEFAULT_VALUE_CELL_UPLOAD);
    }
}
