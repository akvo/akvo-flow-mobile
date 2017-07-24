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
import org.akvo.flow.data.entity.ApiQuestionAnswer;
import org.akvo.flow.data.entity.ApiSurveyInstance;
import org.akvo.flow.database.Constants;
import org.akvo.flow.database.RecordColumns;
import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.database.SyncTimeColumns;
import org.akvo.flow.database.TransmissionStatus;
import org.akvo.flow.database.britedb.BriteSurveyDbAdapter;
import org.akvo.flow.domain.entity.User;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;

public class DatabaseDataSource {

    private final BriteSurveyDbAdapter briteSurveyDbAdapter;

    @Inject
    public DatabaseDataSource(BriteDatabase db) {
        this.briteSurveyDbAdapter = new BriteSurveyDbAdapter(db);
    }

    public Observable<Cursor> getSurveys() {
        return briteSurveyDbAdapter.getSurveys();
    }

    public Observable<Boolean> deleteSurvey(long surveyId) {
        return briteSurveyDbAdapter.deleteSurveyAndGroup(surveyId);
    }

    public Observable<Cursor> getDataPoints(@NonNull Long surveyGroupId, @Nullable Double latitude,
            @Nullable Double longitude, @Nullable Integer orderBy) {
        if (isRequestFiltered(orderBy)) {
            return briteSurveyDbAdapter
                    .getFilteredDataPoints(surveyGroupId, latitude, longitude, orderBy);
        } else {
            return briteSurveyDbAdapter.getDataPoints(surveyGroupId);
        }

    }

    private boolean isRequestFiltered(@Nullable Integer orderBy) {
        return orderBy != null && (orderBy == Constants.ORDER_BY_DISTANCE ||
                orderBy ==  Constants.ORDER_BY_DATE ||
                orderBy ==  Constants.ORDER_BY_STATUS ||
                orderBy ==  Constants.ORDER_BY_NAME);
    }

    public Cursor getSyncedTime(long surveyGroupId) {
        return briteSurveyDbAdapter.getSyncTime(surveyGroupId);
    }

    public Observable<List<ApiDataPoint>> syncDataPoints(List<ApiDataPoint> apiDataPoints) {
        if (apiDataPoints == null) {
            return Observable.<List<ApiDataPoint>>just(Collections.EMPTY_LIST);
        }
        syncDataPointsWithDataBase(apiDataPoints);
        return Observable.just(apiDataPoints);
    }

    private void syncDataPointsWithDataBase(@Nullable List<ApiDataPoint> apiDataPoints) {
        if (apiDataPoints == null || apiDataPoints.size() == 0) {
            return;
        }
        BriteDatabase.Transaction transaction = briteSurveyDbAdapter.beginTransaction();
        try {
            for (ApiDataPoint dataPoint : apiDataPoints) {
                final String id = dataPoint.getId();
                ContentValues values = new ContentValues();
                values.put(RecordColumns.RECORD_ID, id);
                values.put(RecordColumns.SURVEY_GROUP_ID, dataPoint.getSurveyGroupId());
                values.put(RecordColumns.NAME, dataPoint.getDisplayName());
                values.put(RecordColumns.LATITUDE, dataPoint.getLatitude());
                values.put(RecordColumns.LONGITUDE, dataPoint.getLongitude());

                syncSurveyInstances(dataPoint.getSurveyInstances(), id);

                briteSurveyDbAdapter.updateRecord(id, values, dataPoint.getLastModified());
            }
            updateLastUpdatedDateTime(apiDataPoints);
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    /**
     * JSON array responses are ordered to have the latest updated datapoint last so
     * we record it to make the next query using it
     * @param apiDataPoints
     */
    private void updateLastUpdatedDateTime(@NonNull List<ApiDataPoint> apiDataPoints) {
        ApiDataPoint apiDataPoint = apiDataPoints.isEmpty()? null : apiDataPoints.get(apiDataPoints.size() - 1);
        if (apiDataPoint != null) {
            String syncTime = String.valueOf(apiDataPoint.getLastModified());
            setSyncTime(apiDataPoint.getSurveyGroupId(), syncTime);
        }
    }

    /**
     * Save the time of last synchronization for a particular SurveyGroup
     *
     * @param surveyGroupId id of the SurveyGroup
     * @param time          String containing the timestamp
     */
    private void setSyncTime(long surveyGroupId, String time) {
        ContentValues values = new ContentValues();
        values.put(SyncTimeColumns.SURVEY_GROUP_ID, surveyGroupId);
        values.put(SyncTimeColumns.TIME, time);
        briteSurveyDbAdapter.insertSyncedTime(values);
    }

    private void syncSurveyInstances(List<ApiSurveyInstance> surveyInstances, String dataPointId) {
        for (ApiSurveyInstance surveyInstance : surveyInstances) {

            ContentValues values = new ContentValues();
            values.put(SurveyInstanceColumns.SURVEY_ID, surveyInstance.getSurveyId());
            values.put(SurveyInstanceColumns.SUBMITTED_DATE, surveyInstance.getCollectionDate());
            values.put(SurveyInstanceColumns.RECORD_ID, dataPointId);
            values.put(SurveyInstanceColumns.STATUS, SurveyInstanceStatus.DOWNLOADED);
            values.put(SurveyInstanceColumns.SYNC_DATE, System.currentTimeMillis());
            values.put(SurveyInstanceColumns.SUBMITTER, surveyInstance.getSubmitter());

            long id = briteSurveyDbAdapter.syncSurveyInstance(values, surveyInstance.getUuid());

            syncResponses(surveyInstance.getQasList(), id);

            // The filename is a unique column in the transmission table, and as we do not have
            // a file to hold this data, we set the value to the instance UUID
            briteSurveyDbAdapter
                    .createTransmission(id, String.valueOf(surveyInstance.getSurveyId()),
                            surveyInstance.getUuid(),
                            TransmissionStatus.SYNCED);
        }
        briteSurveyDbAdapter.deleteEmptyRecords();
    }

    private void syncResponses(List<ApiQuestionAnswer> responses, long surveyInstanceId) {
        for (ApiQuestionAnswer response : responses) {

            ContentValues values = new ContentValues();
            values.put(ResponseColumns.ANSWER, response.getAnswer());
            values.put(ResponseColumns.TYPE, response.getType());
            values.put(ResponseColumns.QUESTION_ID, response.getQuestionId());
            /*
             * true by default when parsing api response
             * Not sure what this fields is used for ??
             */
            values.put(ResponseColumns.INCLUDE, true);
            values.put(ResponseColumns.SURVEY_INSTANCE_ID, surveyInstanceId);

            briteSurveyDbAdapter.syncResponse(surveyInstanceId, values, response.getQuestionId());
        }
    }

    public Observable<Cursor> getUsers() {
        return briteSurveyDbAdapter.getUsers();
    }

    public Observable<Boolean> editUser(User user) {
        briteSurveyDbAdapter.updateUser(user.getId(), user.getName());
        return Observable.just(true);
    }

    public Observable<Boolean> deleteUser(User user) {
        briteSurveyDbAdapter.deleteUser(user.getId());
        return Observable.just(true);
    }

    public Observable<Long> createUser(String userName) {
        return Observable.just(briteSurveyDbAdapter.createUser(userName));
    }
}
