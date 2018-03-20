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

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UnPublishData;

import javax.inject.Inject;

import timber.log.Timber;

public class UnPublishDataService extends IntentService {

    private static final String TAG = "UnPublishDataService";

    @Inject
    UnPublishData unPublishData;

    public UnPublishDataService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Timber.d("Will un publish files");
        unPublishData.execute(new DefaultObserver<Boolean>() {

            @Override
            public void onNext(Boolean aBoolean) {
                unPublishData.dispose();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                unPublishData.dispose();
            }
        });
    }
}
