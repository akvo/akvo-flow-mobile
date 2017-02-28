/*
 *  Copyright (C) 2013-2017 Stichting Akvo (Akvo Foundation)
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
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;

import org.akvo.flow.api.FlowApi;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyInstance;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.exception.HttpException;
import org.akvo.flow.presentation.datapoints.list.DataPointsListFragment;
import org.akvo.flow.util.ConstantUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class SurveyedDataPointSyncService extends IntentService {

    private static final String TAG = SurveyedDataPointSyncService.class.getSimpleName();

    public static final String SURVEY_GROUP = "survey_group";

    public SurveyedDataPointSyncService() {
        super(TAG);
        // Tell the system to restart the service if it was unexpectedly stopped before completion
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final long surveyGroupId = intent.getLongExtra(SURVEY_GROUP, SurveyGroup.ID_NONE);
        FlowApi api = new FlowApi(getApplicationContext());
        SurveyDbDataSource database = new SurveyDbDataSource(getApplicationContext(), null);
        database.open();
        boolean correctSync = true;
        int resultCode = ConstantUtil.DATA_SYNC_RESULT_SUCCESS;
        int syncedRecords = 0;
        try {
            Set<String> batch, lastBatch = new HashSet<>();
            while (true) {
                Pair<Set<String>, Boolean> syncResult = sync(database, api, surveyGroupId);
                batch = syncResult.first;
                if (!syncResult.second) {
                    //at least one of the data points seems corrupted
                    correctSync = syncResult.second;
                }
                batch.removeAll(lastBatch);// Remove duplicates.
                if (batch.isEmpty()) {
                    break;
                }
                syncedRecords += batch.size();
                sendUpdateUiBroadcastNotification();// Keep the UI fresh!
                lastBatch = batch;
            }
            if (!correctSync) {
                resultCode = ConstantUtil.DATA_SYNC_RESULT_ERROR_UNKNOWN;
            }
        } catch (HttpException e) {
            Timber.e(e, e.getMessage());
            switch (e.getStatus()) {
                case HttpURLConnection.HTTP_FORBIDDEN:
                    // A missing assignment might be the issue. Let's hint the user.
                    resultCode = ConstantUtil.DATA_SYNC_RESULT_ERROR_MISSING_ASSIGNMENT;
                    break;
                default:
                    resultCode = ConstantUtil.DATA_SYNC_RESULT_ERROR_UNKNOWN;
                    break;
            }
        } catch (IOException e) {
            Timber.e(e, e.getMessage());
            resultCode = ConstantUtil.DATA_SYNC_RESULT_ERROR_NETWORK;
        } finally {
            database.close();
        }
        sendResultBroadcastNotification(resultCode, syncedRecords);
    }

    /**
     * Sync a Record batch, and return the Set of Record IDs within the response
     */
    @NonNull
    private Pair<Set<String>, Boolean> sync(@NonNull SurveyDbDataSource database, @NonNull FlowApi api,
            long surveyGroupId) throws IOException {
        final String syncTime = database.getSyncTime(surveyGroupId);
        Set<String> records = new HashSet<>();
        Timber.d("sync() - SurveyGroup: " + surveyGroupId + ". SyncTime: " + syncTime);
        List<SurveyedLocale> locales = api.getSurveyedLocales(surveyGroupId, syncTime);
        boolean correctData = true;
        if (locales != null) {
            for (SurveyedLocale locale : locales) {
                List<SurveyInstance> surveyInstances = locale.getSurveyInstances();
                if (surveyInstances == null || surveyInstances.isEmpty()) {
                    correctData = false;
                }
                //database.syncSurveyedLocale(locale);
                records.add(locale.getId());
            }
        }
        //Delete empty or corrupted data received from server
        //database.deleteEmptyRecords();
        return new Pair<>(records, correctData);
    }

    /**
     * Dispatch a Broadcast notification to notify of SurveyedLocales synchronization.
     * This notification will be received in {@link DataPointsListFragment}
     * or {@link org.akvo.flow.presentation.datapoints.map.DataPointsMapFragment},
     * in order to load data from DB
     */
    private void sendUpdateUiBroadcastNotification() {
        Intent intentBroadcast = new Intent(ConstantUtil.ACTION_LOCALE_SYNC_UPDATE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBroadcast);
    }

    private void sendResultBroadcastNotification(int resultCode, int syncedRecords) {
        Intent intentBroadcast = new Intent(ConstantUtil.ACTION_LOCALE_SYNC_RESULT);
        intentBroadcast.putExtra(ConstantUtil.EXTRA_DATAPOINT_SYNC_RESULT, resultCode);
        if (resultCode == ConstantUtil.DATA_SYNC_RESULT_SUCCESS && syncedRecords > 0) {
            intentBroadcast.putExtra(ConstantUtil.EXTRA_DATAPOINT_NUMBER, syncedRecords);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBroadcast);
    }
}
