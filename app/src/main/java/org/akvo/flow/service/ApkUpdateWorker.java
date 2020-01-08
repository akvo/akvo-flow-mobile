/*
* Copyright (C) 2010-2019 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.BuildConfig;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.entity.ApkData;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.apk.RefreshApkData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import timber.log.Timber;

/**
 * This worker will check the rest api for a new version of the APK.
 * If found, it will display a {@link org.akvo.flow.activity.AppUpdateActivity}, requesting
 * permission to download and installAppUpdate it.
 *
 */
public class ApkUpdateWorker extends Worker {

    /**
     * Tag that is unique to this task (can be used to cancel task)
     */
    public static final String TAG = "APK_UPDATE_SERVICE";

    @Inject
    RefreshApkData refreshApkData;

    public ApkUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    public static void enqueueWork(Context applicationContext) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest
                .Builder(ApkUpdateWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .addTag(ApkUpdateWorker.TAG)
                .build();
        WorkManager.getInstance(applicationContext)
                .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, workRequest);
    }

    @NonNull
    @Override
    public Result doWork() {
        Map<String, Object> params = new HashMap<>(2);
        params.put(RefreshApkData.APP_VERSION_PARAM, BuildConfig.VERSION_NAME);
        refreshApkData.execute(new DefaultObserver<ApkData>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Could not call apk version service");
            }
        }, params);
        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        refreshApkData.dispose();
    }
}
