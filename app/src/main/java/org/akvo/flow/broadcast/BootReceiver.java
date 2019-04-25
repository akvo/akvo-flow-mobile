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

package org.akvo.flow.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.settings.publish.PublishedTimeHelper;
import org.akvo.flow.service.UnPublishDataService;
import org.akvo.flow.util.AlarmHelper;
import org.akvo.flow.util.BootReceiverHelper;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class BootReceiver extends BroadcastReceiver {

    private static final String BOOT_ACTION = "android.intent.action.BOOT_COMPLETED";

    @Inject
    @Named("getPublishDataTime")
    UseCase getPublishDataTime;

    @Inject
    AlarmHelper alarmHelper;

    @Inject
    PublishedTimeHelper publishedTimeHelper;

    @Inject
    BootReceiverHelper bootReceiverHelper;

    @Override
    public void onReceive(final Context context, Intent intent) {
        final Context appContext = context.getApplicationContext();
        initializeInjector(appContext);
        if (BOOT_ACTION.equals(intent.getAction())) {
            getPublishDataTime.execute(new DefaultObserver<Long>() {
                @Override
                public void onError(Throwable e) {
                    getPublishDataTime.dispose();
                    disableReceiverAndStartService(context);
                    Timber.e(e);
                }

                @Override
                public void onNext(Long publishTime) {
                    getPublishDataTime.dispose();
                    long timeSincePublished = publishedTimeHelper
                            .calculateTimeSincePublished(publishTime);
                    if (timeSincePublished < PublishedTimeHelper.MAX_PUBLISH_TIME_IN_MS) {
                        int timeLeft = publishedTimeHelper
                                .getRemainingPublishedTime(timeSincePublished);
                        alarmHelper.scheduleAlarm(timeLeft * 60 * 1000);
                    } else {
                        disableReceiverAndStartService(context);
                    }
                }

                private void disableReceiverAndStartService(Context context) {
                    bootReceiverHelper.disableBootReceiver();
                    ContextCompat
                            .startForegroundService(context,
                                    new Intent(context, UnPublishDataService.class));
                }
            }, null);
        }
    }

    private void initializeInjector(Context applicationContext) {
        FlowApp application = (FlowApp) applicationContext;
        application.getApplicationComponent().inject(this);
    }
}
