/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.api.FlowApi;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.exception.HttpException;
import org.akvo.flow.util.ConstantUtil;

public class SurveyedLocaleSyncService extends IntentService {
    private static final String TAG = SurveyedLocaleSyncService.class.getSimpleName();

    public static final String SURVEY_GROUP = "survey_group";
    
    private Handler mHandler = new Handler();
    
    public SurveyedLocaleSyncService() {
        super(TAG);
        // Tell the system to restart the service if it was unexpectedly stopped before completion
        setIntentRedelivery(true);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        final long surveyGroupId = intent.getLongExtra(SURVEY_GROUP, SurveyGroup.ID_NONE);
        int syncedRecords = 0;
        FlowApi api = new FlowApi();
        SurveyDbAdapter database = new SurveyDbAdapter(getApplicationContext()).open();
        displayNotification(getString(R.string.syncing_records), 
                getString(R.string.pleasewait), false);
        try {
            Set<String> batch, lastBatch = null;
            while (true) {
                batch = sync(database, api, surveyGroupId);
                if (lastBatch != null && lastBatch.containsAll(batch)) {
                    break;
                }
                syncedRecords += batch.size();
                sendBroadcastNotification();// Keep the UI fresh!
                displayNotification(getString(R.string.syncing_records),
                        String.format(getString(R.string.synced_records), syncedRecords), false);
                lastBatch = batch;
            }
            displayNotification(getString(R.string.sync_finished),
                    String.format(getString(R.string.synced_records), syncedRecords), true);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            displayToast(getString(R.string.network_error));
            displayNotification(getString(R.string.sync_error), 
                    getString(R.string.network_error), true);
        } catch (HttpException e) {
            Log.e(TAG, e.getMessage());
            String message = e.getMessage();
            switch (e.getStatus()) {
                case HttpException.Status.SC_FORBIDDEN:
                    // A missing assignment might be the issue. Let's hint the user.
                    message = getString(R.string.error_assignment_text);
                    break;
            }
            displayToast(message);
            displayNotification(getString(R.string.sync_error), message, true);
        } finally {
            database.close();
        }

        sendBroadcastNotification();
    }

    /**
     * Sync a Record batch, and return the Set of Record IDs within the response
     */
    private Set<String> sync(SurveyDbAdapter database, FlowApi api, long surveyGroupId)
            throws IOException, HttpException {
        final String syncTime = database.getSyncTime(surveyGroupId);
        Set<String> records = new HashSet<String>();
        Log.d(TAG, "sync() - SurveyGroup: " + surveyGroupId + ". SyncTime: " + syncTime);
        List<SurveyedLocale> locales = api.getSurveyedLocales(surveyGroupId, syncTime);
        if (locales != null) {
            for (SurveyedLocale locale : locales) {
                database.syncSurveyedLocale(locale);
                records.add(locale.getId());
            }
        }
        return records;
    }
    
    private void displayToast(final String text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void displayNotification(String title, String text, boolean finished) {
        int icon = finished ? android.R.drawable.stat_sys_download_done
                : android.R.drawable.stat_sys_download;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setTicker(title);

        builder.setOngoing(!finished);// Ongoing if still syncing the records

        // Progress will only be displayed in Android versions > 4.0
        builder.setProgress(1, 1, !finished);
        
        // Dummy intent. Do nothing when clicked
        PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(), 0);
        builder.setContentIntent(intent);
        
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(ConstantUtil.NOTIFICATION_RECORD_SYNC, builder.build());
    }
    
    /**
     * Dispatch a Broadcast notification to notify of SurveyedLocales synchronization.
     * This notification will be received in SurveyedLocalesActivity, in order to
     * refresh its data
     */
    private void sendBroadcastNotification() {
        Intent intentBroadcast = new Intent(getString(R.string.action_locales_sync));
        sendBroadcast(intentBroadcast);
    }

}
