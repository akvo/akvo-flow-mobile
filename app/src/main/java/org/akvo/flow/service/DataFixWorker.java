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

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.interactor.CheckSubmittedFiles;
import org.akvo.flow.domain.interactor.ExportSurveyInstances;
import org.akvo.flow.domain.interactor.MakeDataPrivate;
import org.akvo.flow.util.ConstantUtil;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.reactivex.observers.DisposableCompletableObserver;
import timber.log.Timber;

public class DataFixWorker extends Worker {

    public static final String TAG = "DataFixWorker";

    @Inject
    MakeDataPrivate makeDataPrivate;

    @Inject
    CheckSubmittedFiles checkSubmittedFiles;

    @Inject
    ExportSurveyInstances exportSurveyInstances;

    public DataFixWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    @NonNull
    @Override
    public Result doWork() {
        makeDataPrivate.execute(new DisposableCompletableObserver() {
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
        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        makeDataPrivate.dispose();
        checkSubmittedFiles.dispose();
        exportSurveyInstances.dispose();
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
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentBroadcast);
    }
}
