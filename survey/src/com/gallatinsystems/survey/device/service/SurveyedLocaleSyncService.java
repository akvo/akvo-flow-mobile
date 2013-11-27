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
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            database.close();
        }
            
        displayNotification("Records Sync Finished",
                "Synced " + syncedRecords + " records", false);
        // notify the activity
        // TODO
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
        mBuilder.setProgress(0, 0, progress);
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
    
}
