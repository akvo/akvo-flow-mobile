/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.interactor.AllDeviceNotifications;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UploadAllDataPoints;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.NotificationHelper;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import timber.log.Timber;

public class DataPointUploadWorker extends Worker {

    public static final String TAG = "DataPointUploadWorker";

    @Inject
    AllDeviceNotifications checkDeviceNotification;

    @Inject
    UploadAllDataPoints upload;

    public DataPointUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    public static void scheduleUpload(Context context, boolean isMobileSyncAllowed) {
        final NetworkType requiredNetwork = isMobileSyncAllowed ?
                NetworkType.CONNECTED : //require a connection to a network
                NetworkType.UNMETERED; //require a connection to a wifi network
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(requiredNetwork)
                .build();
        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(
                DataPointUploadWorker.class)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .addTag(TAG)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, uploadWorkRequest);
    }

    @NonNull
    @Override
    public Result doWork() {
        NotificationHelper.showSyncingNotification(getApplicationContext());
        checkDeviceNotification();
        NotificationHelper.hidePendingNotification(getApplicationContext());
        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        checkDeviceNotification.dispose();
        upload.dispose();
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
                    .displayNonOnGoingErrorNotification(getApplicationContext(), notificationId,
                            text, title);
        }
    }

    private String getString(@StringRes int resId) {
        return getApplicationContext().getString(resId);
    }

    private void displayErrorNotification(String formId) {
        Context applicationContext = getApplicationContext();
        NotificationHelper.displayErrorNotification(
                applicationContext.getString(R.string.sync_error_title, formId),
                getString(R.string.sync_error_message), applicationContext, formId(formId));
    }

    /**
     * Coerce a form id into its numeric format
     */
    private int formId(String id) {
        try {
            return Integer.valueOf(id);
        } catch (NumberFormatException e) {
            Timber.e("%s is not a valid form id", id);
            return 0;
        }
    }

    private void broadcastDataPointStatusChange() {
        Intent intentBroadcast = new Intent(ConstantUtil.ACTION_DATA_SYNC);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentBroadcast);
    }
}
