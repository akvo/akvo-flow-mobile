/*
 *  Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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
import org.akvo.flow.domain.interactor.ExportSurveyInstances;
import org.akvo.flow.domain.interactor.MakeDataPrivate;
import org.akvo.flow.util.ConstantUtil;

import javax.inject.Inject;

import io.reactivex.observers.DisposableCompletableObserver;
import timber.log.Timber;

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
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        makeDataPrivate.dispose();
        checkSubmittedFiles.dispose();
        exportSurveyInstances.dispose();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        makeDataPrivate.execute(new DisposableCompletableObserver()  {
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
        checkSubmittedFiles.execute(new DisposableCompletableObserver() {
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
        exportSurveyInstances.execute(new DisposableCompletableObserver() {
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
