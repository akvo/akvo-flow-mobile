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

package org.akvo.flow.service;

import android.content.Context;
import android.support.v4.util.Pair;

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

import timber.log.Timber;

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

    public static void scheduleFirstTask(Context context) {
        schedulePeriodicTask(context, ConstantUtil.FIRST_REPEAT_INTERVAL_IN_SECONDS,
                ConstantUtil.FIRST_FLEX_INTERVAL_IN_SECOND);
    }

    private static void schedulePeriodicTask(Context context, int repeatIntervalInSeconds,
            int flexIntervalInSeconds) {
        try {
            PeriodicTask periodic = new PeriodicTask.Builder()
                    .setService(ApkUpdateService.class)
                    //repeat every x seconds
                    .setPeriod(repeatIntervalInSeconds)
                    //specify how much earlier the task can be executed (in seconds)
                    .setFlex(flexIntervalInSeconds)
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
            Timber.e(e, "scheduleRepeat failed");
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
        scheduleFirstTask(this);
    }

    /**
     * Check if new FLOW versions are available to installAppUpdate. If a new version is available,
     * we display {@link org.akvo.flow.activity.AppUpdateActivity}, requesting the user to download
     * it.
     */
    @Override
    public int onRunTask(TaskParams taskParams) {
        //after the first time the task is run we reschedule to a higher interval
        schedulePeriodicTask(this, ConstantUtil.REPEAT_INTERVAL_IN_SECONDS,
                ConstantUtil.FLEX_INTERVAL_IN_SECONDS);
        Context applicationContext = getApplicationContext();
        apkUpdateHelper = new ApkUpdateHelper(applicationContext);
        ConnectivityStateManager connectivityStateManager = new ConnectivityStateManager(
                applicationContext);
        Prefs prefs = new Prefs(applicationContext);
        if (!syncOverMobileNetworksAllowed(prefs) && !connectivityStateManager.isWifiConnected()) {
            Timber.d("No available authorised connection. Can't perform the requested operation");
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
            Timber.e(e, "Error with apk version service");
            return GcmNetworkManager.RESULT_FAILURE;
        }
    }

    private boolean syncOverMobileNetworksAllowed(Prefs prefs) {
        return prefs.getBoolean(Prefs.KEY_CELL_UPLOAD,
                Prefs.DEFAULT_VALUE_CELL_UPLOAD);
    }
}
