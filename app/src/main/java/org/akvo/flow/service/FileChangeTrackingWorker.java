/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.service;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.util.ConstantUtil;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class FileChangeTrackingWorker extends Worker {

    private static final String TAG = "FileChangeTrackingWorker";

    @Inject
    ZipFileLister zipFileLister;

    public FileChangeTrackingWorker(@NonNull Context context,
            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        FlowApp application = (FlowApp) context.getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    @NonNull
    @Override
    public Result doWork() {
        List<File> files = zipFileLister.getZipFiles();
        if (!files.isEmpty()) {
            sendBroadcast();
        }
        return Result.success();
    }

    public static void scheduleVerifier(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(true)
                .build();
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest
                .Builder(FileChangeTrackingWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .addTag(TAG)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, workRequest);
    }

    private void sendBroadcast() {
        Intent bootStrapIntent = new Intent(ConstantUtil.BOOTSTRAP_INTENT);
        getApplicationContext().sendBroadcast(bootStrapIntent);
    }
}
