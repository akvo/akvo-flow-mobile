/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.datasource;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.squareup.sqlbrite.BriteDatabase;

import org.akvo.flow.data.entity.ApiDataPoint;
import org.akvo.flow.data.entity.ApiSurveyInstance;
import org.akvo.flow.database.Constants;
import org.akvo.flow.database.RecordColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.database.SyncTimeColumns;
import org.akvo.flow.database.Tables;
import org.akvo.flow.database.TransmissionStatus;
import org.akvo.flow.database.britedb.BriteSurveyDbAdapter;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;

public class DatabaseDataSource {

    private final BriteSurveyDbAdapter briteSurveyDbAdapter;

    @Inject
    public DatabaseDataSource(BriteDatabase db) {
        this.briteSurveyDbAdapter = new BriteSurveyDbAdapter(db);
    }

    public Observable<Cursor> getDataPoints(@NonNull Long surveyGroupId, @Nullable Double latitude,
            @Nullable Double longitude, @Nullable Integer orderBy) {
        if (orderBy == null) {
            orderBy = Constants.ORDER_BY_NONE;
        }
        switch (orderBy) {
            case Constants.ORDER_BY_DISTANCE:
            case Constants.ORDER_BY_DATE:
            case Constants.ORDER_BY_STATUS:
            case Constants.ORDER_BY_NAME:
                return briteSurveyDbAdapter
                        .getFilteredSurveyedLocales(surveyGroupId, latitude, longitude, orderBy);
            default:
                return briteSurveyDbAdapter.getSurveyedLocales(surveyGroupId);
        }
    }

    public Observable<Cursor> getSyncedTime(long surveyGroupId) {
        return briteSurveyDbAdapter.getSyncTime(surveyGroupId);
    }

    public Observable<Boolean> syncSurveyedLocales(List<ApiDataPoint> apiDataPoints) {
        if (apiDataPoints == null) {
            return Observable.just(false);
        }
        for (ApiDataPoint apiDataPoint : apiDataPoints) {
            syncSurveyedLocale(apiDataPoint);
        }
        return null;
    }

    public void syncSurveyedLocale(ApiDataPoint surveyedLocale) {
        final String id = surveyedLocale.getId();
        try {

            ContentValues values = new ContentValues();
            values.put(RecordColumns.RECORD_ID, id);
            values.put(RecordColumns.SURVEY_GROUP_ID, surveyedLocale.getSurveyGroupId());
            values.put(RecordColumns.NAME, surveyedLocale.getDisplayName());
            values.put(RecordColumns.LATITUDE, surveyedLocale.getLatitude());
            values.put(RecordColumns.LONGITUDE, surveyedLocale.getLongitude());

            //THIS is not so good but temporary
            surveyDbAdapter.beginTransaction();

            syncSurveyInstances(surveyedLocale.getSurveyInstances(), id);

            briteSurveyDbAdapter.updateRecord(id, values, surveyedLocale.getLastModified());

            String syncTime = String.valueOf(surveyedLocale.getLastModified());
            setSyncTime(surveyedLocale.getSurveyGroupId(), syncTime);

            surveyDbAdapter.successfulTransaction();
        } finally {
            surveyDbAdapter.endTransaction();
        }
    }

    /**
     * Save the time of synchronization time for a particular SurveyGroup
     *
     * @param surveyGroupId id of the SurveyGroup
     * @param time          String containing the timestamp
     */
    public void setSyncTime(long surveyGroupId, String time) {
        ContentValues values = new ContentValues();
        values.put(SyncTimeColumns.SURVEY_GROUP_ID, surveyGroupId);
        values.put(SyncTimeColumns.TIME, time);
        briteSurveyDbAdapter.insertSyncedTime(values);
    }

    private void syncSurveyInstances(List<ApiSurveyInstance> surveyInstances,
            String surveyedLocaleId) {
        for (ApiSurveyInstance surveyInstance : surveyInstances) {

            ContentValues values = new ContentValues();
            values.put(SurveyInstanceColumns.SURVEY_ID, surveyInstance.getSurveyId());
            values.put(SurveyInstanceColumns.SUBMITTED_DATE, surveyInstance.getCollectionDate());
            values.put(SurveyInstanceColumns.RECORD_ID, surveyedLocaleId);
            values.put(SurveyInstanceColumns.STATUS, SurveyInstanceStatus.DOWNLOADED);
            values.put(SurveyInstanceColumns.SYNC_DATE, System.currentTimeMillis());
            values.put(SurveyInstanceColumns.SUBMITTER, surveyInstance.getSubmitter());

            long id = surveyDbAdapter.syncSurveyInstance(values, surveyInstance.getUuid());

            // Now the responses...
            syncResponses(surveyInstance.getResponses(), id);

            // The filename is a unique column in the transmission table, and as we do not have
            // a file to hold this data, we set the value to the instance UUID
            surveyDbAdapter
                    .createTransmission(id, surveyInstance.getSurveyId(), surveyInstance.getUuid(),
                            TransmissionStatus.SYNCED);
        }
    }
}
