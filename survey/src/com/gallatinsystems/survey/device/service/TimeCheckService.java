/*
 *  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
 * 
 *  This file is part of Akvo FLOW.
 * 
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 * 
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 * 
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package com.gallatinsystems.survey.device.service;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.gallatinsystems.survey.device.activity.TimeCheckActivity;
import com.gallatinsystems.survey.device.exception.PersistentUncaughtExceptionHandler;
import com.gallatinsystems.survey.device.util.HttpUtil;
import com.gallatinsystems.survey.device.util.StatusUtil;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class TimeCheckService extends IntentService {
    private static final String TAG = TimeCheckService.class.getSimpleName();
    private static final String TIME_CHECK_PATH = "/devicetimerest";
    private static final long OFFSET_THRESHOLD = 13 * 60 * 1000;// 13 minutes
    private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";// ISO 8601
    private static final String TIMEZONE = "UTC";

    public TimeCheckService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler
                .getInstance());
        checkTime();
    }

    private void checkTime() {
        if (!StatusUtil.hasDataConnection(this, false)) {
            Log.d(TAG, "No internet connection. Can't perform the time check.");
            return;
        }

        try {
            final String url = StatusUtil.getServerBase(this) + TIME_CHECK_PATH;
            String response = HttpUtil.httpGet(url);
            if (!TextUtils.isEmpty(response)) {
                JSONObject json = new JSONObject(response);
                String time = json.getString("time");
                if (!TextUtils.isEmpty(time) && !time.equalsIgnoreCase("null")) {
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
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching time: ", e);
            PersistentUncaughtExceptionHandler.recordException(e);
        }
    }

}
