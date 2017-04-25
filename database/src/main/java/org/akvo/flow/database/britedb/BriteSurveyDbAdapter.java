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

package org.akvo.flow.database.britedb;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import org.akvo.flow.database.Constants;
import org.akvo.flow.database.RecordColumns;
import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SyncTimeColumns;
import org.akvo.flow.database.Tables;
import org.akvo.flow.database.TransmissionColumns;
import org.akvo.flow.database.TransmissionStatus;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

public class BriteSurveyDbAdapter {

    private static final int DOES_NOT_EXIST = -1;

    private final BriteDatabase briteDatabase;

    public BriteSurveyDbAdapter(BriteDatabase briteDatabase) {
        this.briteDatabase = briteDatabase;
    }

    /**
     * Filters surveyed locales based on the parameters passed in.
     */
    public Observable<Cursor> getFilteredSurveyedLocales(long surveyGroupId, Double latitude,
            Double longitude, int orderBy) {
        // Note: This PROJECTION column indexes have to match the default RecordQuery PROJECTION ones,
        // as this one will only APPEND new columns to the resultset, making the generic getSurveyedLocale(Cursor)
        // fully compatible. TODO: This should be refactored and replaced with a less complex approach.
        String queryString = "SELECT sl.*,"
                + " MIN(r." + SurveyInstanceColumns.STATUS + ") as " + SurveyInstanceColumns.STATUS
                + " FROM "
                + Tables.RECORD + " AS sl LEFT JOIN " + Tables.SURVEY_INSTANCE + " AS r ON "
                + "sl." + RecordColumns.RECORD_ID + "=" + "r." + SurveyInstanceColumns.RECORD_ID;
        String whereClause = " WHERE sl." + RecordColumns.SURVEY_GROUP_ID + " =?";
        String groupBy = " GROUP BY sl." + RecordColumns.RECORD_ID;

        String orderByStr = "";
        switch (orderBy) {
            case Constants.ORDER_BY_DATE:
                orderByStr = " ORDER BY " + RecordColumns.LAST_MODIFIED + " DESC";// By date
                break;
            case Constants.ORDER_BY_DISTANCE:
                if (latitude != null && longitude != null) {
                    // this is to correct the distance for the shortening at higher latitudes
                    Double fudge = Math.pow(Math.cos(Math.toRadians(latitude)), 2);

                    // this uses a simple planar approximation of distance. this should be good
                    // enough for our purpose.
                    String orderByTempl = " ORDER BY CASE WHEN " + RecordColumns.LATITUDE
                            + " IS NULL THEN 1 ELSE 0 END,"
                            + " ((%s - " + RecordColumns.LATITUDE + ") * (%s - "
                            + RecordColumns.LATITUDE
                            + ") + (%s - " + RecordColumns.LONGITUDE + ") * (%s - "
                            + RecordColumns.LONGITUDE + ") * %s)";
                    orderByStr = String
                            .format(orderByTempl, latitude, latitude, longitude, longitude, fudge);
                }
                break;
            case Constants.ORDER_BY_STATUS:
                orderByStr = " ORDER BY " + " MIN(r." + SurveyInstanceColumns.STATUS + ")";
                break;
            case Constants.ORDER_BY_NAME:
                orderByStr = " ORDER BY " + RecordColumns.NAME + " COLLATE NOCASE ASC";// By name
                break;
            default:
                break;
        }

        String[] whereValues = new String[] { String.valueOf(surveyGroupId) };
        List<String> tables = new ArrayList<>(2);
        tables.add(Tables.RECORD);
        tables.add(Tables.SURVEY_INSTANCE);
        return briteDatabase
                .createQuery(tables, queryString + whereClause + groupBy + orderByStr, whereValues)
                .concatMap(
                        new Func1<SqlBrite.Query, Observable<Cursor>>() {
                            @Override
                            public Observable<Cursor> call(SqlBrite.Query query) {
                                return Observable.just(query.run());
                            }
                        });
    }

    public Observable<Cursor> getSurveyedLocales(long surveyGroupId) {
        String sqlQuery =
                "SELECT * FROM " + Tables.RECORD + " WHERE " + RecordColumns.SURVEY_GROUP_ID
                        + " = ?";
        return briteDatabase.createQuery(Tables.RECORD, sqlQuery,
                String.valueOf(surveyGroupId)).concatMap(
                new Func1<SqlBrite.Query, Observable<? extends Cursor>>() {
                    @Override
                    public Observable<? extends Cursor> call(SqlBrite.Query query) {
                        return Observable.just(query.run());
                    }
                });
    }

    /**
     * Update the last modification date, if necessary
     */
    public void updateRecordModifiedDate(String recordId, long timestamp) {
        ContentValues values = new ContentValues();
        values.put(RecordColumns.LAST_MODIFIED, timestamp);
        briteDatabase.update(Tables.RECORD, values,
                RecordColumns.RECORD_ID + " = ? AND " + RecordColumns.LAST_MODIFIED + " < ?",
                recordId, String.valueOf(timestamp));
    }

    public void updateSurveyedLocale(String surveyedLocaleId, ContentValues surveyedLocaleValues) {
        briteDatabase.update(Tables.RECORD, surveyedLocaleValues, RecordColumns.RECORD_ID + " = ?",
                surveyedLocaleId);
    }

    public void updateRecord(String id, ContentValues values, long lastModified) {
        briteDatabase.insert(Tables.RECORD, values);
        // Update the record last modification date, if necessary
        updateRecordModifiedDate(id, lastModified);
    }

    /**
     * Get the synchronization time for a particular survey group.
     *
     * @param surveyGroupId id of the SurveyGroup
     * @return time if exists for this key, null otherwise
     */
    public Cursor getSyncTime(long surveyGroupId) {
        String sql =
                "SELECT " + SyncTimeColumns.SURVEY_GROUP_ID + "," + SyncTimeColumns.TIME + " FROM "
                        + Tables.SYNC_TIME + " WHERE " + SyncTimeColumns.SURVEY_GROUP_ID
                        + " = ?";
        return briteDatabase.query(sql, String.valueOf(surveyGroupId));
    }

    public void insertSyncedTime(ContentValues values) {
        briteDatabase.insert(Tables.SYNC_TIME, values);
    }

    public long syncSurveyInstance(ContentValues values, String surveyInstanceUuid) {
        String sql =
                "SELECT " + SurveyInstanceColumns._ID + "," + SurveyInstanceColumns.UUID + " FROM "
                        + Tables.SURVEY_INSTANCE + " WHERE " + SurveyInstanceColumns.UUID + " = ?";
        Cursor cursor = briteDatabase.query(sql, surveyInstanceUuid);

        long id = DOES_NOT_EXIST;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                id = cursor.getLong(0);
            }
            cursor.close();
        }
        if (id != DOES_NOT_EXIST) {
            briteDatabase.update(Tables.SURVEY_INSTANCE, values, SurveyInstanceColumns.UUID
                    + " = ?", surveyInstanceUuid);
        } else {
            values.put(SurveyInstanceColumns.UUID, surveyInstanceUuid);
            id = briteDatabase.insert(Tables.SURVEY_INSTANCE, values);
        }
        return id;
    }

    public BriteDatabase.Transaction beginTransaction() {
        return briteDatabase.newTransaction();
    }

    public void syncResponse(long surveyInstanceId, ContentValues values, String questionId) {
        Cursor cursor = getLastExistingResponse(surveyInstanceId, questionId);
        boolean anotherIterationExists = cursor != null && cursor.moveToFirst();
        if (anotherIterationExists) {
            int iteration = cursor.getInt(cursor.getColumnIndexOrThrow(ResponseColumns.ITERATION));
            boolean isFirstIterationFound = iteration == -1;
            if (isFirstIterationFound) {
                updateIterationOfFirstResponse(surveyInstanceId, questionId);
                iteration = 0;
            }
            values.put(ResponseColumns.ITERATION, iteration + 1);
            briteDatabase.insert(Tables.RESPONSE, values);
        } else {
            briteDatabase.insert(Tables.RESPONSE, values);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    private void updateIterationOfFirstResponse(long surveyInstanceId, String questionId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ResponseColumns.ITERATION, 0);
        briteDatabase.update(Tables.RESPONSE, contentValues,
                ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND "
                        + ResponseColumns.QUESTION_ID + " = ?",
                String.valueOf(surveyInstanceId), questionId);
    }

    public void deleteResponses(long surveyInstanceId, String questionId) {
        briteDatabase.delete(Tables.RESPONSE,
                ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND " + ResponseColumns.QUESTION_ID
                        + " = ?", new String[] { surveyInstanceId + "", questionId });
    }

    private Cursor getLastExistingResponse(long surveyInstanceId, @Nullable String questionId) {
        if (questionId == null) {
            return null;
        }
        String sql =
                "SELECT " + ResponseColumns.SURVEY_INSTANCE_ID
                        + ","
                        + ResponseColumns.QUESTION_ID
                        + ","
                        + ResponseColumns.ITERATION
                        + " FROM " + Tables.RESPONSE + " WHERE "
                        + ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND "
                        + ResponseColumns.QUESTION_ID + " = ? ORDER BY "
                        + ResponseColumns.ITERATION
                        + " DESC LIMIT 1";
        Cursor cursor = briteDatabase
                .query(sql, String.valueOf(surveyInstanceId), questionId);
        return cursor;
    }

    public void createTransmission(long surveyInstanceId, String formID, String filename,
            int status) {
        ContentValues values = new ContentValues();
        values.put(TransmissionColumns.SURVEY_INSTANCE_ID, surveyInstanceId);
        values.put(TransmissionColumns.SURVEY_ID, formID);
        values.put(TransmissionColumns.FILENAME, filename);
        values.put(TransmissionColumns.STATUS, status);
        if (TransmissionStatus.SYNCED == status) {
            final String date = String.valueOf(System.currentTimeMillis());
            values.put(TransmissionColumns.START_DATE, date);
            values.put(TransmissionColumns.END_DATE, date);
        }
        briteDatabase.insert(Tables.TRANSMISSION, values);
    }

    /**
     * Delete any Record that contains no SurveyInstance
     */
    public void deleteEmptyRecords() {
        briteDatabase.execute("DELETE FROM " + Tables.RECORD
                + " WHERE " + RecordColumns.RECORD_ID + " NOT IN "
                + "(SELECT DISTINCT " + SurveyInstanceColumns.RECORD_ID
                + " FROM " + Tables.SURVEY_INSTANCE + ")");
    }
}
