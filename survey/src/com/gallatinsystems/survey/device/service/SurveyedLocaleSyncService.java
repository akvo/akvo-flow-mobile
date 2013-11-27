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
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.api.FlowApi;
import com.gallatinsystems.survey.device.api.response.SurveyedLocalesResponse;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyGroup;

public class SurveyedLocaleSyncService extends IntentService {
    private static final String TAG = SurveyedLocaleSyncService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 100;
    
    public static final String SURVEY_GROUP = "survey_group";
    
    public SurveyedLocaleSyncService() {
        super(TAG);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        final int surveyGroupId = intent.getIntExtra(SURVEY_GROUP, SurveyGroup.ID_NONE);
        int syncedRecords = 0;
        FlowApi api = new FlowApi();
        SurveyDbAdapter database = new SurveyDbAdapter(getApplicationContext()).open();
        displayNotification("Synchronising Records...", "Please, wait", true);
        try {
            int batchSize = 0;
            while ((batchSize = sync(database, api, surveyGroupId)) != 0) {
                syncedRecords += batchSize;
                sendBroadcastNotification();// Keep the UI fresh!
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            database.close();
        }
            
        displayNotification("Records Sync Finished",
                "Synced " + syncedRecords + " records", false);
    }
        
    private int sync(SurveyDbAdapter database, FlowApi api, 
            int surveyGroupId) throws IOException {
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
    
    private void displayNotification(String title, String text, boolean progress) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.info)
                .setContentTitle(title)
                .setContentText(text);
        mBuilder.setProgress(1, 1, progress);
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
