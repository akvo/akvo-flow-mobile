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

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.util.ConstantUtil;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class FileChangeTrackingService extends GcmTaskService {

    private static final long VERIFY_PERIOD_SECONDS = 30;
    private static final String TAG = "FileChangeTrackingService";

    @Inject
    ZipFileLister zipFileLister;

    @Override
    public void onCreate() {
        super.onCreate();
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
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

    public static void scheduleVerifier(Context context) {
        GcmNetworkManager gcmNetworkManager = GcmNetworkManager.getInstance(context);
        Task periodicTask = new PeriodicTask.Builder()
                .setPeriod(VERIFY_PERIOD_SECONDS)
                .setRequiredNetwork(PeriodicTask.NETWORK_STATE_ANY)
                .setTag(TAG)
                .setService(FileChangeTrackingService.class)
                .setPersisted(true)
                .setUpdateCurrent(true)
                .setRequiresCharging(true) //only run if we are connected via USB to PC
                .build();
        gcmNetworkManager.schedule(periodicTask);
    }

    @Override
    public void onInitializeTasks() {
        super.onInitializeTasks();
        scheduleVerifier(getApplicationContext());
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        List<File> files = zipFileLister.getZipFiles();
        if (!files.isEmpty()) {
            sendBroadcast();
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    private void sendBroadcast() {
        Intent bootStrapIntent = new Intent(ConstantUtil.BOOTSTRAP_INTENT);
        sendBroadcast(bootStrapIntent);
    }
}
