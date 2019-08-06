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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UnPublishData;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.NotificationHelper;

import javax.inject.Inject;

import timber.log.Timber;

public class UnPublishDataService extends Service {

    @Inject
    UnPublishData unPublishData;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
        unPublishData.dispose();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(ConstantUtil.UN_PUBLISH_NOTIFICATION_ID,
                NotificationHelper.getUnPublishingNotification(getApplicationContext()));
        unPublishData.execute(new DefaultObserver<Boolean>() {

            @Override
            public void onNext(Boolean aBoolean) {
                stopService();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                stopService();
            }
        }, null);
        return super.onStartCommand(intent, flags, startId);
    }

    private void stopService() {
        unPublishData.dispose();
        stopForeground(true);
        stopSelf();
    }
}
