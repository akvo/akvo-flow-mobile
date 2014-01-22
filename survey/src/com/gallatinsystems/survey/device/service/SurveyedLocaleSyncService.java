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

package com.gallatinsystems.survey.device.service;

import java.io.IOException;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.api.FlowApi;
import com.gallatinsystems.survey.device.api.response.SurveyedLocalesResponse;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyGroup;

public class SurveyedLocaleSyncService extends IntentService {
    private static final String TAG = SurveyedLocaleSyncService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 100;
    
    public static final String SURVEY_GROUP = "survey_group";
    
    private Handler mHandler = new Handler();
    
    public SurveyedLocaleSyncService() {
        super(TAG);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        final long surveyGroupId = intent.getLongExtra(SURVEY_GROUP, SurveyGroup.ID_NONE);
        int syncedRecords = 0;
        FlowApi api = new FlowApi();
        SurveyDbAdapter database = new SurveyDbAdapter(getApplicationContext()).open();
        displayNotification(getString(R.string.syncing_records), 
                getString(R.string.pleasewait), true);
        try {
            int batchSize = 0;
            while ((batchSize = sync(database, api, surveyGroupId)) != 0) {
                syncedRecords += batchSize;
                sendBroadcastNotification();// Keep the UI fresh!
                // TODO: Update notification with new batch info?
            }
            
            displayNotification(getString(R.string.sync_finished),
                    String.format(getString(R.string.synced_records), syncedRecords), false);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            displayToast(getString(R.string.network_error));
            displayNotification(getString(R.string.sync_error), 
                    getString(R.string.network_error), false);
        } finally {
            database.close();
        }
    }
        
    private int sync(SurveyDbAdapter database, FlowApi api, 
            long surveyGroupId) throws IOException {
        final String syncTime = database.getSyncTime(surveyGroupId);
        Log.d(TAG, "sync() - SurveyGroup: " + surveyGroupId + ". SyncTime: " + syncTime);
        SurveyedLocalesResponse response = api.getSurveyedLocales(surveyGroupId, syncTime);
        if (response != null && !(response.getSyncTime().equals(syncTime))) {
            database.syncSurveyedLocales(response.getSurveyedLocales());
            database.setSyncTime(surveyGroupId, response.getSyncTime());
            return response.getSurveyedLocales().size();
        }
            
        return 0;
    }
    
    private void displayToast(final String text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG)
                        .show();
            }
        });
    }
    
    private void displayNotification(String title, String text, boolean progress) {
        int icon = progress ? android.R.drawable.stat_sys_download
                : android.R.drawable.stat_sys_download_done;
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setTicker(title);
        
        // Progress will only be displayed in Android versions > 4.0
        mBuilder.setProgress(1, 1, progress);
        
        // Dummy intent. Do nothing when clicked
        PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(), 0);
        mBuilder.setContentIntent(intent);
        
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
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
