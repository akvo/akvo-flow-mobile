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

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.LocalBroadcastManager;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.interactor.CheckSubmittedFiles;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.ExportSurveyInstances;
import org.akvo.flow.domain.interactor.MakeDataPrivate;
import org.akvo.flow.util.ConstantUtil;

import java.util.List;

import javax.inject.Inject;

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
public class DataFixService extends JobIntentService {

    /**
     * Unique job ID for this service.
     */
    private static final int JOB_ID = 1000;

    @Inject
    MakeDataPrivate makeDataPrivate;

    @Inject
    CheckSubmittedFiles checkSubmittedFiles;

    @Inject
    ExportSurveyInstances exportSurveyInstances;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, DataFixService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("onCreate");
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy");
        makeDataPrivate.dispose();
        checkSubmittedFiles.dispose();
        exportSurveyInstances.dispose();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Timber.d("onHandleWork");
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
        });
    }

    private void verify() {
        checkSubmittedFiles.execute(new DefaultObserver<List<Boolean>>() {
            @Override
            public void onError(Throwable e) {
                export();
            }

            @Override
            public void onComplete() {
                export();
            }

        });
    }

    private void export() {
        exportSurveyInstances.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onComplete() {
                broadcastDataPointStatusChange();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                broadcastDataPointStatusChange();
            }
        });
    }

    private void broadcastDataPointStatusChange() {
        Intent intentBroadcast = new Intent(ConstantUtil.ACTION_DATA_SYNC);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBroadcast);
    }
}
