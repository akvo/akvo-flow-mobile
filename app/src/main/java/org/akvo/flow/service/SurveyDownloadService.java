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

import android.app.IntentService;
import android.content.Intent;
import androidx.annotation.Nullable;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.forms.DownloadForms;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.NotificationHelper;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * This activity will check for new surveys on the device and install as needed
 *
 * @author Christopher Fagiani
 */
public class SurveyDownloadService extends IntentService {

    private static final String TAG = "SURVEY_DOWNLOAD_SERVICE";

    @Inject
    DownloadForms downloadForms;

    public SurveyDownloadService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    public void onHandleIntent(@Nullable Intent intent) {
        NotificationHelper
                .displayFormsSyncingNotification(getApplicationContext());
        downloadForms.execute(new DefaultObserver<Integer>(){
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                NotificationHelper
                        .displayErrorNotification(getString(R.string.error_form_sync_title),
                                "", getApplicationContext(), ConstantUtil.NOTIFICATION_FORM);
            }

            @Override 
            public void onNext(Integer downloaded) {
                NotificationHelper
                        .displayFormsSyncedNotification(getApplicationContext(), downloaded);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        downloadForms.dispose();
    }
}
