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

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.entity.ApkData;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.apk.RefreshApkData;
import org.akvo.flow.util.ConstantUtil;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

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

    @Inject
    RefreshApkData refreshApkData;

    public static void scheduleFirstTask(Context context) {
        schedulePeriodicTask(context, ConstantUtil.FIRST_REPEAT_INTERVAL_IN_SECONDS,
                ConstantUtil.FIRST_FLEX_INTERVAL_IN_SECOND);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            // GcmTaskService doesn't check for null intent
            Timber.w("Invalid GcmTask null intent.");
            stopSelf();
            return START_NOT_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
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

    @Override
    public void onCreate() {
        super.onCreate();
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        refreshApkData.dispose();
    }

    /**
     * Called when app is updated to a new version, reinstalled etc.
     * Repeating tasks have to be rescheduled
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
        Map<String, String> params = new HashMap<>(2);
        params.put(RefreshApkData.APP_VERSION_PARAM, BuildConfig.VERSION_NAME);
        refreshApkData.execute(new DefaultObserver<ApkData>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Could not call apk version service");
            }
        }, params);
        return GcmNetworkManager.RESULT_SUCCESS;
    }
}
