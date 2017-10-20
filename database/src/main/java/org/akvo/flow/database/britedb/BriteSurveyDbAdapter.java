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

import com.squareup.sqlbrite2.BriteDatabase;
import com.squareup.sqlbrite2.SqlBrite;

import org.akvo.flow.database.RecordColumns;
import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.database.SurveyColumns;
import org.akvo.flow.database.SurveyGroupColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SyncTimeColumns;
import org.akvo.flow.database.Tables;
import org.akvo.flow.database.TransmissionColumns;
import org.akvo.flow.database.TransmissionStatus;
import org.akvo.flow.database.UserColumns;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import timber.log.Timber;

import static org.akvo.flow.database.Constants.ORDER_BY_DATE;
import static org.akvo.flow.database.Constants.ORDER_BY_DISTANCE;
import static org.akvo.flow.database.Constants.ORDER_BY_NAME;
import static org.akvo.flow.database.Constants.ORDER_BY_STATUS;

public class BriteSurveyDbAdapter {

    private static final int DOES_NOT_EXIST = -1;

    private final BriteDatabase briteDatabase;

    public BriteSurveyDbAdapter(BriteDatabase briteDatabase) {
        this.briteDatabase = briteDatabase;
    }

    /**
     * Filters surveyed locales based on the parameters passed in.
     */
    public Observable<Cursor> getFilteredDataPoints(long surveyGroupId, Double latitude,
            Double longitude, int orderBy) {
        String queryString = "SELECT sl.*,"
                + " MIN(r." + SurveyInstanceColumns.STATUS + ") as " + SurveyInstanceColumns.STATUS
                + " FROM "
                + Tables.RECORD + " AS sl LEFT JOIN " + Tables.SURVEY_INSTANCE + " AS r ON "
                + "sl." + RecordColumns.RECORD_ID + "=" + "r." + SurveyInstanceColumns.RECORD_ID;
        String whereClause = " WHERE sl." + RecordColumns.SURVEY_GROUP_ID + " =?";
        String groupBy = " GROUP BY sl." + RecordColumns.RECORD_ID;

        String orderByStr = "";
        switch (orderBy) {
            case ORDER_BY_DATE:
                orderByStr = " ORDER BY " + RecordColumns.LAST_MODIFIED + " DESC";
                break;
            case ORDER_BY_DISTANCE:
                if (latitude != null && longitude != null) {
                    orderByStr = getOrderByDistanceString(latitude, longitude);
                }
                break;
            case ORDER_BY_STATUS:
                orderByStr = " ORDER BY " + " MIN(r." + SurveyInstanceColumns.STATUS + ")";
                break;
            case ORDER_BY_NAME:
                orderByStr = " ORDER BY " + RecordColumns.NAME + " COLLATE NOCASE ASC";
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
                        new Function<SqlBrite.Query, Observable<Cursor>>() {
                            @Override
                            public Observable<Cursor> apply(SqlBrite.Query query) {
                                return Observable.just(query.run());
                            }
                        });
    }

    /**
     * Uses a simple planar approximation of distance
     */
    private String getOrderByDistanceString(Double latitude, Double longitude) {
        Double fudge = correctDistanceForShortening(latitude);

        String orderBy = " ORDER BY CASE WHEN " + RecordColumns.LATITUDE
                + " IS NULL THEN 1 ELSE 0 END,"
                + " ((%s - " + RecordColumns.LATITUDE + ") * (%s - "
                + RecordColumns.LATITUDE
                + ") + (%s - " + RecordColumns.LONGITUDE + ") * (%s - "
                + RecordColumns.LONGITUDE + ") * %s)";
        return String
                .format(orderBy, latitude, latitude, longitude, longitude, fudge);
    }

    /**
     * correct the distance for the shortening at higher latitudes
     *
     * @param latitude
     * @return
     */
    private double correctDistanceForShortening(Double latitude) {
        return Math.pow(Math.cos(Math.toRadians(latitude)), 2);
    }

    public Observable<Cursor> getDataPoints(long surveyGroupId) {
        String sqlQuery =
                "SELECT * FROM " + Tables.RECORD + " WHERE " + RecordColumns.SURVEY_GROUP_ID
                        + " = ?";
        return briteDatabase.createQuery(Tables.RECORD, sqlQuery,
                String.valueOf(surveyGroupId)).concatMap(
                new Function<SqlBrite.Query, Observable<? extends Cursor>>() {
                    @Override
                    public Observable<? extends Cursor> apply(SqlBrite.Query query) {
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

    public void updateDataPoint(String datapointId, ContentValues dataPointValues) {
        briteDatabase.update(Tables.RECORD, dataPointValues, RecordColumns.RECORD_ID + " = ?",
                datapointId);
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
        }
        briteDatabase.insert(Tables.RESPONSE, values);
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
                        + " = ?", surveyInstanceId + "", questionId);
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
        return briteDatabase.query(sql, String.valueOf(surveyInstanceId), questionId);
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

    public Observable<Boolean> deleteSurveyAndGroup(long surveyGroupId) {
        deleteSurveyGroup(surveyGroupId);
        deleteSurvey(surveyGroupId);
        return Observable.just(true);
    }

    private void deleteSurvey(long surveyGroupId) {
        briteDatabase.delete(Tables.SURVEY, SurveyColumns.SURVEY_GROUP_ID + " = ? ",
                String.valueOf(surveyGroupId));
    }

    private void deleteSurveyGroup(long surveyGroupId) {
        briteDatabase.delete(Tables.SURVEY_GROUP, SurveyGroupColumns.SURVEY_GROUP_ID + " = ? ",
                String.valueOf(surveyGroupId));
    }

    public Observable<Cursor> getSurveys() {
        String sqlQuery = "SELECT * FROM " + Tables.SURVEY_GROUP;
        return briteDatabase
                .createQuery(Tables.SURVEY_GROUP, sqlQuery)
                .concatMap(new Function<SqlBrite.Query, Observable<? extends Cursor>>() {
                    @Override
                    public Observable<? extends Cursor> apply(SqlBrite.Query query) {
                        return Observable.just(query.run());
                    }
                });
    }

    public Cursor getSurveys(long surveyGroupId) {
        String sqlQuery = "SELECT "
                + SurveyColumns._ID + ", "
                + SurveyColumns.SURVEY_ID + ", "
                + SurveyColumns.NAME + ", "
                + SurveyColumns.FILENAME + ", "
                + SurveyColumns.TYPE + ", "
                + SurveyColumns.LANGUAGE + ", "
                + SurveyColumns.HELP_DOWNLOADED + ", "
                + SurveyColumns.VERSION + ", "
                + SurveyColumns.LOCATION
                + " FROM " + Tables.SURVEY;
        String whereClause = SurveyColumns.DELETED + " <> 1";
        String[] whereParams = new String[0];
        if (surveyGroupId > 0) {
            whereClause += " AND " + SurveyColumns.SURVEY_GROUP_ID + " = ?";
            whereParams = new String[] {
                    String.valueOf(surveyGroupId)
            };
        }
        sqlQuery += " WHERE " + whereClause;
        return briteDatabase.query(sqlQuery, whereParams);
    }

    @Nullable
    public String[] getSurveyIds() {
        String sqlQuery = "SELECT "
                + SurveyColumns.SURVEY_ID
                + " FROM " + Tables.SURVEY
                + " WHERE " + SurveyColumns.DELETED + " <> 1";
        Cursor c =  briteDatabase.query(sqlQuery, "");
        if (c != null) {
            String[] ids = new String[c.getCount()];
            if (c.moveToFirst()) {
                do {
                    ids[c.getPosition()] = c
                            .getString(c.getColumnIndexOrThrow(SurveyColumns.SURVEY_ID));
                } while (c.moveToNext());
            }
            c.close();
            return ids;
        }
        return null;
    }

    public void addSurveyGroup(ContentValues values) {
        briteDatabase.insert(Tables.SURVEY_GROUP, values);
    }

    public Observable<Cursor> getUsers() {
        String sqlQuery =
                "SELECT * FROM " + Tables.USER + " WHERE " + UserColumns.DELETED + " <> 1";
        return briteDatabase
                .createQuery(Tables.USER, sqlQuery)
                .concatMap(new Function<SqlBrite.Query, ObservableSource<? extends Cursor>>() {
                    @Override
                    public Observable<? extends Cursor> apply(SqlBrite.Query query) {
                        return Observable.just(query.run());
                    }
                });
    }

    public void updateUser(long id, String name) {
        ContentValues values = new ContentValues();
        values.put(UserColumns.NAME, name);
        briteDatabase.update(Tables.USER, values, UserColumns._ID + "=?", String.valueOf(id));
    }

    public void deleteUser(long userId) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(UserColumns.DELETED, 1);
        briteDatabase.update(Tables.USER, updatedValues, UserColumns._ID + " = ?",
                String.valueOf(userId));
    }

    public long createUser(String userName) {
        ContentValues values = new ContentValues();
        values.put(UserColumns.NAME, userName);
        return briteDatabase.insert(Tables.USER, values);
    }

    /**
     * reinserts the test survey into the database. For debugging purposes only.
     * The survey xml must exist in the APK
     */
    public void reinstallTestSurvey() {
        ContentValues values = new ContentValues();
        values.put(SurveyColumns.SURVEY_ID, "999991");
        values.put(SurveyColumns.NAME, "Sample Survey");
        values.put(SurveyColumns.VERSION, 1.0);
        values.put(SurveyColumns.TYPE, "Survey");
        values.put(SurveyColumns.LOCATION, "res");
        values.put(SurveyColumns.FILENAME, "999991.xml");
        values.put(SurveyColumns.LANGUAGE, "en");
        briteDatabase.insert(Tables.SURVEY, values);
    }

    public void updateSurvey(ContentValues updatedValues, String surveyId) {
        int affectedRows = briteDatabase
                .update(Tables.SURVEY, updatedValues, SurveyColumns.SURVEY_ID + " = ?", surveyId);
        if (affectedRows <= 0) {
            briteDatabase.insert(Tables.SURVEY, updatedValues);
        }
    }

    /**
     * deletes all the surveys from the database
     */
    public void deleteAllSurveys() {
        briteDatabase.delete(Tables.SURVEY, null);
        briteDatabase.delete(Tables.SURVEY_GROUP, null);
    }

    public Cursor getSurveys(String surveyId, String surveyVersion) {
        String sql = "SELECT " + SurveyColumns.SURVEY_ID + " FROM " + Tables.SURVEY + " WHERE " +
                SurveyColumns.SURVEY_ID + " = ? and (" + SurveyColumns.VERSION + " >= ? or "
                + SurveyColumns.DELETED + " = ?)";
        String[] selectionArgs = {
                surveyId,
                surveyVersion,
                String.valueOf(1)
        };
        return briteDatabase.query(sql, selectionArgs);
    }

    /**
     * updates the survey table by recording the help download flag
     */
    public void markSurveyHelpDownloaded(String surveyId, boolean isDownloaded) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyColumns.HELP_DOWNLOADED, isDownloaded ? 1 : 0);

        int updatedRows = briteDatabase
                .update(Tables.SURVEY, updatedValues, SurveyColumns.SURVEY_ID + " = ?", surveyId);
        if (updatedRows < 1) {
            Timber.e("Could not update record for Survey %s", surveyId);
        }
    }

    /**
     * marks a survey record identified by the ID passed in as deleted.
     *
     * @param surveyId
     */
    public void deleteSurvey(String surveyId) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyColumns.DELETED, 1);
        briteDatabase
                .update(Tables.SURVEY, updatedValues, SurveyColumns.SURVEY_ID + " = ?", surveyId);
    }

    /**
     * if the ID is populated, this will update a user record. Otherwise, it
     * will be inserted
     */
    public long createOrUpdateUser(Long id, String name) {
        ContentValues initialValues = new ContentValues();
        Long userId = id;
        initialValues.put(UserColumns.NAME, name);
        initialValues.put(UserColumns.DELETED, 0);

        if (userId == null) {
            userId = briteDatabase.insert(Tables.USER, initialValues);
        } else {
            briteDatabase
                    .update(Tables.USER, initialValues, UserColumns._ID + "=?", userId.toString());
        }
        return userId;
    }
}
