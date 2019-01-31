/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.interactor.AllDeviceNotifications;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UploadAllDataPoints;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.NotificationHelper;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Task service which will only run if there is a connection
 */
public class DataPointUploadService extends GcmTaskService {

    private static final String TAG = "DataPointUploadService";

    @Inject
    AllDeviceNotifications checkDeviceNotification;

    @Inject
    UploadAllDataPoints upload;

    public static void scheduleUpload(Context context, boolean isMobileSyncAllowed) {
        final int requiredNetwork = isMobileSyncAllowed ?
                Task.NETWORK_STATE_CONNECTED : //require a connection to a network
                Task.NETWORK_STATE_UNMETERED; //require a connection to a wifi network
        GcmNetworkManager gcmNetworkManager = GcmNetworkManager.getInstance(context);
        Task periodicTask = new OneoffTask.Builder()
                .setRequiredNetwork(requiredNetwork)
                .setTag(TAG)
                .setExecutionWindow(0, 30)
                .setService(DataPointUploadService.class)
                .setPersisted(true)
                .setUpdateCurrent(true)
                .setRequiresCharging(false)
                .build();
        gcmNetworkManager.schedule(periodicTask);
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
        checkDeviceNotification.dispose();
        upload.dispose();
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

    @Override
    public void onInitializeTasks() {
        super.onInitializeTasks();
        scheduleUpload(getApplicationContext(), false);
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        NotificationHelper.showSyncingNotification(getApplicationContext());
        checkDeviceNotification();
        NotificationHelper.hideSyncingNotification(getApplicationContext());
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    private void checkDeviceNotification() {
        checkDeviceNotification.execute(new DefaultObserver<Set<String>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                uploadFiles();
            }

            @Override
            public void onNext(Set<String> deletedFiles) {
                for (String formId : deletedFiles) {
                    displayFormDeletedNotification(formId);
                }
                uploadFiles();
            }
        });
    }

    private void uploadFiles() {
        upload.execute(new DefaultObserver<Set<String>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                broadcastDataPointStatusChange();
            }

            @Override
            public void onNext(Set<String> errorForms) {
                for (String formId : errorForms) {
                    displayErrorNotification(formId);
                }
                broadcastDataPointStatusChange();
            }
        });
    }

    private void displayFormDeletedNotification(String formId) {
        final int notificationId = formId(formId);
        if (notificationId != 0) {
            String text = String
                    .format(getString(R.string.data_sync_error_form_deleted_text), formId);
            String title = getString(R.string.data_sync_error_form_deleted_title);
            NotificationHelper
                    .displayNonOnGoingErrorNotification(this, notificationId, text, title);
        }
    }

    private void displayErrorNotification(String formId) {
        NotificationHelper.displayErrorNotification(getString(R.string.sync_error_title, formId),
                getString(R.string.sync_error_message), this, formId(formId));
    }

    /**
     * Coerce a form id into its numeric format
     */
    private int formId(String id) {
        try {
            return Integer.valueOf(id);
        } catch (NumberFormatException e) {
            Timber.e(id + " is not a valid form id");
            return 0;
        }
    }

    private void broadcastDataPointStatusChange() {
        Intent intentBroadcast = new Intent(ConstantUtil.ACTION_DATA_SYNC);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBroadcast);
    }
}
