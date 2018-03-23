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

package org.akvo.flow.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.util.AlarmHelper;

import javax.inject.Inject;

public class BootReceiver extends BroadcastReceiver {

    @Inject
    AlarmHelper alarmHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        initializeInjector(context);
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            //TODO: calculate actual time left and if 0 start service directly
           alarmHelper.scheduleAlarm(90 * 1000);
        }
    }

    private void initializeInjector(Context context) {
        FlowApp application = (FlowApp) context.getApplicationContext();
        application.getApplicationComponent().inject(this);
    }
}
