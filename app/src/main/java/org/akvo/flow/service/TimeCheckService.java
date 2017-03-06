/*
* Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.activity.TimeCheckActivity;
import org.akvo.flow.api.FlowApi;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.util.ConnectivityStateManager;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import timber.log.Timber;

import static org.akvo.flow.util.StringUtil.isValid;

public class TimeCheckService extends IntentService {
    private static final String TAG = TimeCheckService.class.getSimpleName();
    private static final long OFFSET_THRESHOLD = 13 * 60 * 1000;// 13 minutes
    private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";// ISO 8601
    private static final String TIMEZONE = "UTC";

    private ConnectivityStateManager connectivityStateManager;
    private Prefs prefs;

    public TimeCheckService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        this.connectivityStateManager = new ConnectivityStateManager(getApplicationContext());
        this.prefs = new Prefs(getApplicationContext());
        checkTime();
    }

    private void checkTime() {
        if (!connectivityStateManager.isConnectionAvailable(
                prefs.getBoolean(Prefs.KEY_CELL_UPLOAD, Prefs.DEFAULT_VALUE_CELL_UPLOAD))) {
            Timber.d("No internet connection available. Can't perform the time check.");
            return;
        }

        // Since a misconfigured date/time might be considering the SSL certificate as expired,
        // we'll use HTTP by default, instead of HTTPS
        try {
            FlowApi flowApi = new FlowApi(getApplicationContext());
            String time = flowApi.getServerTime();

            if (isValid(time)) {
                DateFormat df = new SimpleDateFormat(PATTERN);
                df.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
                final long remote = df.parse(time).getTime();
                final long local = System.currentTimeMillis();
                boolean onTime = Math.abs(remote - local) < OFFSET_THRESHOLD;

                if (!onTime) {
                    Intent i = new Intent(this, TimeCheckActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            }
        } catch (IOException | ParseException e) {
            Timber.e(e, "Error fetching time");
        }
    }
}
