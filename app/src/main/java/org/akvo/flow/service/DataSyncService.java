/*
 *  Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.NotificationHelper;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

/**
 * Handle survey export and sync in a background thread. The export process takes
 * no arguments, and will try to zip all the survey instances with a SUBMIT_REQUESTED status
 * but with no EXPORT_DATE (export hasn't happened yet). Ideally, and if the service has been
 * triggered by a survey submission, only one survey instance will be exported. However, if for
 * whatever reason, a previous export attempt has failed, a new export will be tried on each
 * execution of the service, until the zip file finally gets exported. A possible scenario for
 * this is the submission of a survey when the external storage is not available, postponing the
 * export until it gets ready.
 * After the export of the zip files, the sync will be run, attempting to upload all the non synced
 * files to the datastore.
 *
 * @author Christopher Fagiani
 */
public class DataSyncService extends Service {

    public static final int NOTIFICATION_ID = 1234;

    @Inject
    @Named("makeDataPrivate")
    UseCase makeDataPrivate;

    @Named("uploadSync")
    @Inject
    UseCase upload;

    @Named("allowedToConnect")
    @Inject
    UseCase allowedToConnect;

    @Named("checkDeviceNotification")
    @Inject
    UseCase checkDeviceNotification;

    @Inject
    @Named("checkSubmittedFiles")
    UseCase checkPublishedFiles;

    @Inject
    @Named("exportSurveyInstances")
    UseCase exportSurveyInstances;

    @Override
    public void onCreate() {
        super.onCreate();
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        makeDataPrivate.dispose();
        upload.dispose();
        allowedToConnect.dispose();
        checkDeviceNotification.dispose();
        checkPublishedFiles.dispose();
        exportSurveyInstances.dispose();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID,
                NotificationHelper.getSyncingNotification(getApplicationContext()));
        makeDataPrivate.dispose();
        makeDataPrivate.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onComplete() {
                verify();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                verify();
            }
        }, null);
        return super.onStartCommand(intent, flags, startId);
    }

    private void verify() {
        checkPublishedFiles.dispose();
        checkPublishedFiles.execute(new DefaultObserver<List<Boolean>>() {
            @Override
            public void onError(Throwable e) {
               export();
            }

            @Override
            public void onComplete() {
                export();
            }

        }, null);
    }

    private void export() {
        exportSurveyInstances.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onComplete() {
                broadcastDataPointStatusChange();
                sync();
            }

            @Override
            public void onError(Throwable e) {
                broadcastDataPointStatusChange();
                sync();
            }
        }, null);
    }

    private void sync() {
        allowedToConnect.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onNext(Boolean connectAllowed) {
                if (connectAllowed) {
                    checkDeviceNotification();
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }
        }, null);
    }

    private void checkDeviceNotification() {
        checkDeviceNotification.dispose();
        checkDeviceNotification.execute(new DefaultObserver<List<String>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                syncFiles();
            }

            @Override
            public void onNext(List<String> deletedFiles) {
                for (String formId : deletedFiles) {
                    displayFormDeletedNotification(formId);
                }
                syncFiles();
            }
        }, null);

    }

    private void syncFiles() {
        upload.dispose();
        upload.execute(new DefaultObserver<Set<String>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                broadcastDataPointStatusChange();
                stopService();
            }

            @Override
            public void onNext(Set<String> errorForms) {
                for (String formId : errorForms) {
                    displayErrorNotification(formId);
                }
                broadcastDataPointStatusChange();
                stopService();
            }

        }, null);
    }

    private void stopService() {
        stopForeground(true);
        stopSelf();
    }

    private void broadcastDataPointStatusChange() {
        Intent intentBroadcast = new Intent(ConstantUtil.ACTION_DATA_SYNC);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBroadcast);
    }

    private void displayErrorNotification(String formId) {
        NotificationHelper.displayErrorNotification(getString(R.string.sync_error_title, formId),
                getString(R.string.sync_error_message), this, formId(formId));
    }

    private void displayFormDeletedNotification(String formId) {
        final int notificationId = formId(formId);

        String text = String.format(getString(R.string.data_sync_error_form_deleted_text), formId);
        String title = getString(R.string.data_sync_error_form_deleted_title);

        NotificationHelper.displayNonOnGoingErrorNotification(this, notificationId, text, title);
    }

    /**
     * Coerce a form id into its numeric format
     */
    private static int formId(String id) {
        try {
            return Integer.valueOf(id);
        } catch (NumberFormatException e) {
            Timber.e(id + " is not a valid form id");
            return 0;
        }
    }
}
