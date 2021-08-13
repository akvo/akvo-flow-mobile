/*
 * Copyright (C) 2017-2021 Stichting Akvo (Akvo Foundation)
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

import static org.akvo.flow.database.Constants.ORDER_BY_DATE;
import static org.akvo.flow.database.Constants.ORDER_BY_DISTANCE;
import static org.akvo.flow.database.Constants.ORDER_BY_NAME;
import static org.akvo.flow.database.Constants.ORDER_BY_STATUS;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.sqlbrite2.BriteDatabase;
import com.squareup.sqlbrite2.SqlBrite;

import org.akvo.flow.database.RecordColumns;
import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.database.SurveyColumns;
import org.akvo.flow.database.SurveyGroupColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.database.TransmissionColumns;
import org.akvo.flow.database.TransmissionStatus;
import org.akvo.flow.database.UserColumns;
import org.akvo.flow.database.tables.DataPointDownloadTable;
import org.akvo.flow.database.tables.FormUpdateNotifiedTable;
import org.akvo.flow.database.tables.LanguageTable;
import org.akvo.flow.database.tables.QuestionGroupTable;
import org.akvo.flow.database.tables.Tables;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import timber.log.Timber;

public class BriteSurveyDbAdapter {

    private static final int DOES_NOT_EXIST = -1;

    private static final String SURVEY_INSTANCE_JOIN_RESPONSE_USER = "survey_instance "
            + "LEFT OUTER JOIN response ON survey_instance._id=response.survey_instance_id "
            + "LEFT OUTER JOIN user ON survey_instance.user_id=user._id";

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

        List<String> tables = new ArrayList<>(2);
        tables.add(Tables.RECORD);
        tables.add(Tables.SURVEY_INSTANCE);
        return briteDatabase
                .createQuery(tables, queryString + whereClause + groupBy + orderByStr,
                        String.valueOf(surveyGroupId))
                .concatMap(new Function<SqlBrite.Query, Observable<Cursor>>() {
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

    public Cursor getDataPoint(String dataPointId) {
        String sqlQuery =
                "SELECT  "
                        + RecordColumns._ID + ","
                        + RecordColumns.RECORD_ID + ","
                        + RecordColumns.SURVEY_GROUP_ID + ","
                        + RecordColumns.NAME + ","
                        + RecordColumns.LATITUDE + ","
                        + RecordColumns.LONGITUDE + ","
                        + RecordColumns.LAST_MODIFIED + ","
                        + RecordColumns.VIEWED
                        + " FROM " + Tables.RECORD
                        + " WHERE " + RecordColumns.RECORD_ID + " = ?";
        return briteDatabase.query(sqlQuery, dataPointId);
    }

    public Observable<Cursor> getDataPoints(long surveyGroupId) {
        String sqlQuery =
                "SELECT * FROM " + Tables.RECORD + " WHERE " + RecordColumns.SURVEY_GROUP_ID
                        + " = ?";
        return briteDatabase.createQuery(Tables.RECORD, sqlQuery,
                String.valueOf(surveyGroupId))
                .concatMap(new Function<SqlBrite.Query, Observable<? extends Cursor>>() {
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

    public boolean insertOrUpdateRecord(String dataPointId, ContentValues values) {
        String sql =
                "SELECT " + RecordColumns.RECORD_ID + ", " + RecordColumns.VIEWED + " FROM "
                        + Tables.RECORD + " WHERE " + RecordColumns.RECORD_ID + " = ?";
        Cursor cursor = briteDatabase.query(sql, dataPointId);

        long id = DOES_NOT_EXIST;
        int viewed = 1;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                id = cursor.getLong(0);
                viewed = cursor.getInt(1);
            }
            cursor.close();
        }
        if (id == DOES_NOT_EXIST) {
            values.put(RecordColumns.VIEWED, 0);
        } else {
            values.put(RecordColumns.VIEWED, viewed);
        }
        briteDatabase.insert(Tables.RECORD, values);
        return id == DOES_NOT_EXIST;
    }

    /**
     * updates the status of a survey instance to the status passed in.
     * Status must be one of the 'SurveyInstanceStatus' one. The corresponding
     * Date column will be updated with the current timestamp.
     */
    public void updateSurveyInstanceStatus(long surveyInstanceId, int status) {
        String dateColumn;
        switch (status) {
            case SurveyInstanceStatus.DOWNLOADED:
            case SurveyInstanceStatus.UPLOADED:
                dateColumn = SurveyInstanceColumns.SYNC_DATE;
                break;
            case SurveyInstanceStatus.SUBMITTED:
                dateColumn = SurveyInstanceColumns.EXPORTED_DATE;
                break;
            case SurveyInstanceStatus.SUBMIT_REQUESTED:
                dateColumn = SurveyInstanceColumns.SUBMITTED_DATE;
                break;
            case SurveyInstanceStatus.SAVED:
                dateColumn = SurveyInstanceColumns.SAVED_DATE;
                break;
            default:
                return;
        }

        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyInstanceColumns.STATUS, status);
        updatedValues.put(dateColumn, System.currentTimeMillis());

        final int rows = briteDatabase.update(Tables.SURVEY_INSTANCE,
                updatedValues,
                SurveyInstanceColumns._ID + " = ?",
                String.valueOf(surveyInstanceId));

        if (rows < 1) {
            Timber.e("Could not update status for Survey Instance: %d", surveyInstanceId);
        }
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

    public Cursor getSurveyInstancesByStatus(int status) {
        String sql = "SELECT " + SurveyInstanceColumns._ID + ", " + SurveyInstanceColumns.UUID
                + " FROM " + Tables.SURVEY_INSTANCE
                + " WHERE " + SurveyInstanceColumns.STATUS + " = ?";
        return briteDatabase.query(sql, String.valueOf(status));
    }

    public Cursor getResponses(long surveyInstanceId) {
        String sql = "SELECT " + SurveyInstanceColumns.SURVEY_ID + ", "
                + SurveyInstanceColumns.SUBMITTED_DATE + ", "
                + SurveyInstanceColumns.UUID + ", "
                + SurveyInstanceColumns.START_DATE + ", "
                + SurveyInstanceColumns.RECORD_ID + ", "
                + SurveyInstanceColumns.DURATION + ", "
                + SurveyInstanceColumns.VERSION + ", "
                + ResponseColumns.ANSWER + ", "
                + ResponseColumns.TYPE + ", "
                + ResponseColumns.QUESTION_ID + ", "
                + ResponseColumns.FILENAME + ", "
                + UserColumns.NAME + ", "
                + UserColumns.EMAIL + ", "
                + ResponseColumns.ITERATION
                + " FROM " + SURVEY_INSTANCE_JOIN_RESPONSE_USER
                + " WHERE " + ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND "
                + ResponseColumns.INCLUDE + " = 1";
        return briteDatabase.query(sql, String.valueOf(surveyInstanceId));
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

    public void createTransmissions(Long instanceId, String formId, Set<String> filenames) {
        BriteDatabase.Transaction transaction = beginTransaction();
        try {
            for (String filename : filenames) {
                createTransmission(instanceId, formId, filename, TransmissionStatus.QUEUED);
            }
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    private void createTransmission(long surveyInstanceId, String formID, String filename,
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

    public void updateFailedTransmissions(@NonNull Set<String> filenames) {
        BriteDatabase.Transaction transaction = beginTransaction();
        try {
            for (String filename : filenames) {
                int rows = updateFailedTransmission(filename);
                if (rows == 0) {
                    // Use a dummy "-1" as survey_instance_id, as the database needs that attribute
                    createTransmission(-1, null, filename, TransmissionStatus.FAILED);
                }
            }
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    /**
     * Updates the matching transmission history records with the status
     * passed in. If the status == Synced, the end date is updated. If
     * the status == In Progress, the start date is updated.
     */
    public void updateTransmissionStatus(long id, int status) {
        ContentValues values = new ContentValues();
        values.put(TransmissionColumns.STATUS, status);
        if (TransmissionStatus.SYNCED == status) {
            values.put(TransmissionColumns.END_DATE, System.currentTimeMillis() + "");
        } else if (TransmissionStatus.IN_PROGRESS == status) {
            values.put(TransmissionColumns.START_DATE, System.currentTimeMillis() + "");
        }
        briteDatabase.update(Tables.TRANSMISSION, values, TransmissionColumns._ID + " = ?",
                id + "");
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
        String sqlQuery =
                "SELECT * FROM " + Tables.SURVEY_GROUP + " ORDER BY " + SurveyGroupColumns.NAME;
        return briteDatabase
                .createQuery(Tables.SURVEY_GROUP, sqlQuery)
                .concatMap((Function<SqlBrite.Query, Observable<Cursor>>) query -> Observable.just(query.run()));
    }

    public Cursor getForms(long surveyId) {
        String sqlQuery = "SELECT * FROM " + Tables.SURVEY;
        String whereClause = SurveyColumns.DELETED + " <> 1";
        String[] whereParams = new String[0];
        if (surveyId > 0) {
            whereClause += " AND " + SurveyColumns.SURVEY_GROUP_ID + " = ?";
            whereParams = new String[]{
                    String.valueOf(surveyId)
            };
        }
        sqlQuery += " WHERE " + whereClause;
        return briteDatabase.query(sqlQuery, whereParams);
    }

    @Nullable
    public Cursor getFormIds() {
        String columns = SurveyColumns.SURVEY_ID;
        return queryForms(columns, "", null);
    }

    public Cursor getFormIds(@NonNull String surveyId) {
        String columns = SurveyColumns.SURVEY_ID;
        String whereClause = SurveyColumns.SURVEY_GROUP_ID + " = ?";
        List<String> surveyIds = new ArrayList<>(1);
        surveyIds.add(surveyId);
        return queryForms(columns, whereClause, surveyIds);
    }

    private Cursor queryForms(String columns, String whereClause, List<String> whereParams) {
        String defaultWhereClause = " WHERE " + SurveyColumns.DELETED + " <> ?";
        List<String> params = new ArrayList<>();
        params.add("1");
        if (!TextUtils.isEmpty(whereClause)) {
            defaultWhereClause += " AND " + whereClause;
        }
        String sqlQuery = "SELECT " + columns + " FROM " + Tables.SURVEY + defaultWhereClause;
        if (whereParams != null) {
            params.addAll(whereParams);
        }
        return briteDatabase.query(sqlQuery, params.toArray(new String[params.size()]));
    }

    public void addSurveyGroup(ContentValues values) {
        String where = SurveyGroupColumns.SURVEY_GROUP_ID + " = ? ";
        int updatedRows = briteDatabase.update(Tables.SURVEY_GROUP, values, where, values.get(SurveyGroupColumns.SURVEY_GROUP_ID) + "");
        if (updatedRows < 1) {
            briteDatabase.insert(Tables.SURVEY_GROUP, values);
        }
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

    public Cursor getUser(long userId) {
        String sqlQuery =
                "SELECT * FROM " + Tables.USER + " WHERE " + UserColumns.DELETED + " <> 1 AND "
                        + UserColumns._ID + "=?";
        return briteDatabase.query(sqlQuery, userId + "");
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
    public void installTestForm() {
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

    public boolean isSurveyUpToDate(String surveyId, String surveyVersion) {
        boolean isUpToDate = true;
        Cursor cursor = getSurveys(surveyId, surveyVersion);

        if (cursor == null || cursor.getCount() <= 0) {
            isUpToDate = false;
        }
        if (cursor != null) {
            cursor.close();
        }
        return isUpToDate;
    }

    /**
     * marks a survey record identified by the ID passed in as deleted.
     */
    public void setFormsDeleted(Set<String> formIds) {
        BriteDatabase.Transaction transaction = beginTransaction();
        try {
            for (String formId : formIds) {
                ContentValues updatedValues = new ContentValues();
                updatedValues.put(SurveyColumns.DELETED, 1);
                briteDatabase.update(Tables.SURVEY, updatedValues, SurveyColumns.SURVEY_ID + " = ?",
                        formId);
            }
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
    }

    public void setSurveyViewed(long surveyId) {
        updateSurveyStatus(surveyId, 1);
    }

    public void setSurveyUnViewed(double surveyId) {
        updateSurveyStatus(surveyId, 0);
    }

    private void updateSurveyStatus(double surveyId, int status) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(SurveyGroupColumns.VIEWED, status);
        String where = SurveyGroupColumns.SURVEY_GROUP_ID + " = ? ";
        briteDatabase.update(Tables.SURVEY_GROUP, contentValues, where, surveyId + "");
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

    /**
     * permanently deletes all surveys, responses, users and transmission
     * history from the database
     */
    public void clearAllData() {
        clearCollectedData();
        deleteAllSurveys();
        briteDatabase.delete(Tables.USER, null);
    }

    /**
     * Permanently deletes user generated data from the database. It will clear
     * any response saved in the database, as well as the transmission history.
     */
    public void clearCollectedData() {
        deleteAllResponses();
        briteDatabase.delete(Tables.SURVEY_INSTANCE, null);
        briteDatabase.delete(Tables.RECORD, null);
        briteDatabase.delete(Tables.TRANSMISSION, null);
        briteDatabase.delete(DataPointDownloadTable.TABLE_NAME, null);
    }

    private void deleteAllResponses() {
        briteDatabase.delete(Tables.RESPONSE, null);
    }

    public boolean unSyncedTransmissionsExist() {
        boolean transmissionsExist = false;
        String column = TransmissionColumns._ID;
        String whereClause = TransmissionColumns.STATUS + " IN (?, ?, ?) LIMIT 1";
        String[] selectionArgs = new String[]{
                String.valueOf(TransmissionStatus.FAILED),
                String.valueOf(TransmissionStatus.IN_PROGRESS),
                String.valueOf(TransmissionStatus.QUEUED)
        };
        Cursor cursor = queryTransmissions(column, whereClause, selectionArgs);
        if (cursor != null && cursor.getCount() > 0) {
            transmissionsExist = true;
        }
        if (cursor != null) {
            cursor.close();
        }
        return transmissionsExist;
    }

    public Cursor getAllTransmissions() {
        String column = TransmissionColumns.FILENAME;
        String whereClause = TransmissionColumns.STATUS + " IN (?, ?, ?, ?)";
        String[] selectionArgs = new String[]{
                String.valueOf(TransmissionStatus.QUEUED),
                String.valueOf(TransmissionStatus.IN_PROGRESS),
                String.valueOf(TransmissionStatus.SYNCED),
                String.valueOf(TransmissionStatus.FAILED),
        };
        return queryTransmissions(column, whereClause, selectionArgs);
    }

    public Cursor getTransmissionForFileName(String filename) {
        String column = TransmissionColumns.SURVEY_INSTANCE_ID;
        String whereClause = TransmissionColumns.FILENAME + " = ? ";
        whereClause += "GROUP BY " + column;
        return queryTransmissions(column, whereClause, new String[]{filename});
    }

    private Cursor queryTransmissions(String column, String whereClause, String[] selectionArgs) {
        String sql = "SELECT " + column + " FROM " + Tables.TRANSMISSION + " WHERE " + whereClause;
        return briteDatabase.query(sql, selectionArgs);
    }

    private int updateFailedTransmission(String filename) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(TransmissionColumns.STATUS, TransmissionStatus.FAILED);
        String where = TransmissionColumns.FILENAME + " = ? ";
        return briteDatabase.update(Tables.TRANSMISSION, contentValues, where, filename);
    }

    public Cursor getUnSyncedTransmissions() {
        String column =
                TransmissionColumns._ID + ", "
                        + TransmissionColumns.SURVEY_INSTANCE_ID + ", "
                        + TransmissionColumns.SURVEY_ID + ", "
                        + TransmissionColumns.FILENAME;
        String whereClause =
                TransmissionColumns.STATUS + " IN (?, ?, ?) AND " + TransmissionColumns.FILENAME
                        + " LIKE '%.%'";
        String[] selectionArgs = new String[]{
                String.valueOf(TransmissionStatus.QUEUED),
                String.valueOf(TransmissionStatus.IN_PROGRESS),
                String.valueOf(TransmissionStatus.FAILED),
        };
        return queryTransmissions(column, whereClause, selectionArgs);
    }

    public Cursor getUnSyncedTransmissions(@NonNull String formId) {
        String column =
                TransmissionColumns._ID + ", "
                        + TransmissionColumns.SURVEY_INSTANCE_ID + ", "
                        + TransmissionColumns.SURVEY_ID + ", "
                        + TransmissionColumns.FILENAME;
        String whereClause =
                TransmissionColumns.STATUS + " IN (?, ?, ?) AND " + TransmissionColumns.FILENAME
                        + " LIKE '%.%' AND " + TransmissionColumns.SURVEY_ID + " = ?";
        String[] selectionArgs = new String[]{
                String.valueOf(TransmissionStatus.QUEUED),
                String.valueOf(TransmissionStatus.IN_PROGRESS),
                String.valueOf(TransmissionStatus.FAILED),
                formId
        };
        return queryTransmissions(column, whereClause, selectionArgs);
    }

    public Cursor getForm(String formId) {
        String sql =
                "SELECT * FROM " + Tables.SURVEY + " WHERE " + SurveyColumns.SURVEY_ID + " = ?";
        return briteDatabase.query(sql, formId);
    }

    public Cursor getSavedFormInstance(String formId, String datapointId) {
        String sql = "SELECT " + SurveyInstanceColumns._ID + " FROM " + Tables.SURVEY_INSTANCE +
                " WHERE " + Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SURVEY_ID + "= ?" +
                " AND " + SurveyInstanceColumns.STATUS + "= ?" +
                " AND " + SurveyInstanceColumns.RECORD_ID + "= ?" +
                " ORDER BY " + SurveyInstanceColumns.START_DATE + " DESC LIMIT 1";

        return briteDatabase.query(sql, formId, String.valueOf(SurveyInstanceStatus.SAVED), datapointId);
    }

    public Cursor getFormInstance(String formId, String datapointId) {
        String sql = "SELECT " +
                SurveyInstanceColumns._ID + ", " +
                SurveyInstanceColumns.STATUS +
                " FROM " + Tables.SURVEY_INSTANCE +
                " WHERE " + Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SURVEY_ID + "= ?" +
                " AND " + SurveyInstanceColumns.RECORD_ID + "= ?" +
                " ORDER BY " + SurveyInstanceColumns.START_DATE + " DESC LIMIT 1";

        return briteDatabase.query(sql, formId, datapointId);
    }

    public Single<Long> createFormInstance(ContentValues initialValues) {
        return Single.just(briteDatabase.insert(Tables.SURVEY_INSTANCE, initialValues));
    }

    public Cursor getRecentSubmittedFormInstance(String formId, String dataPointId, long maxDate) {
        long dateParam = System.currentTimeMillis() - maxDate;
        String sql = "SELECT * FROM " + Tables.SURVEY_INSTANCE +
                " WHERE " + Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SURVEY_ID + "= ?" +
                " AND (" + SurveyInstanceColumns.STATUS + " = " + SurveyInstanceStatus.SUBMITTED +
                " OR " + SurveyInstanceColumns.STATUS + " = " + SurveyInstanceStatus.UPLOADED + ")" +
                " AND " + SurveyInstanceColumns.RECORD_ID + "= ?" +
                " AND " + SurveyInstanceColumns.SUBMITTED_DATE + " > ?" +
                " ORDER BY " + SurveyInstanceColumns.SUBMITTED_DATE + " DESC LIMIT 1";

        return briteDatabase.query(sql, formId, dataPointId, String.valueOf(dateParam));
    }

    public void markDataPointAsViewed(String dataPointId) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(RecordColumns.VIEWED, 1);
        String where = RecordColumns.RECORD_ID + " = ? AND " + RecordColumns.VIEWED + " = 0";
        briteDatabase.update(Tables.RECORD, contentValues, where, dataPointId);
    }

    public Cursor getCursor(long surveyId) {
        return briteDatabase.query("SELECT " + DataPointDownloadTable.COLUMN_CURSOR
                + " FROM " + DataPointDownloadTable.TABLE_NAME
                + " WHERE " + DataPointDownloadTable.COLUMN_SURVEY_ID + " = ? ", surveyId + "");
    }

    public void saveCursor(long surveyId, String cursor) {
        ContentValues contentValues = new ContentValues(2);
        contentValues.put(DataPointDownloadTable.COLUMN_SURVEY_ID, surveyId);
        contentValues.put(DataPointDownloadTable.COLUMN_CURSOR, cursor);
        briteDatabase.insert(DataPointDownloadTable.TABLE_NAME, contentValues);
    }

    public void clearCursor(long surveyId) {
        briteDatabase.delete(DataPointDownloadTable.TABLE_NAME, DataPointDownloadTable.COLUMN_SURVEY_ID + " = ?", surveyId + "");
    }


    public void cleanDataPoints(Long surveyGroupId) {
        String where1 = SurveyInstanceColumns._ID + " NOT IN "
                + "(SELECT DISTINCT " + ResponseColumns.SURVEY_INSTANCE_ID
                + " FROM " + Tables.RESPONSE + ")";
        briteDatabase.delete(Tables.SURVEY_INSTANCE, where1);
        String where =
                RecordColumns.SURVEY_GROUP_ID + " =? AND "
                        + RecordColumns.STATUS + " =? AND "
                        + RecordColumns.RECORD_ID + " NOT IN "
                        + "(SELECT DISTINCT " + SurveyInstanceColumns.RECORD_ID
                        + " FROM " + Tables.SURVEY_INSTANCE + ")";
        briteDatabase.delete(Tables.RECORD, where, surveyGroupId + "", "0");
    }

    public Cursor getSavedLanguages(long surveyId) {
        String sql = "SELECT " + LanguageTable.COLUMN_LANGUAGE_CODE
                + " FROM " + LanguageTable.TABLE_NAME
                + " WHERE " + LanguageTable.COLUMN_SURVEY_ID + " = ?";
        return briteDatabase.query(sql, surveyId + "");
    }

    public void saveLanguagePreferences(long surveyId, @NonNull Set<String> languageCodes) {
        ContentValues contentValues = new ContentValues(2);
        briteDatabase.delete(LanguageTable.TABLE_NAME, LanguageTable.COLUMN_SURVEY_ID + " = ?",
                surveyId + "");
        for (String languageCode : languageCodes) {
            contentValues.put(LanguageTable.COLUMN_SURVEY_ID, surveyId);
            contentValues.put(LanguageTable.COLUMN_LANGUAGE_CODE, languageCode);
            briteDatabase.insert(LanguageTable.TABLE_NAME, contentValues);
        }
    }

    public long updateFormVersion(long formInstanceId, double formVersion) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(SurveyInstanceColumns.VERSION, formVersion);
        String where = SurveyInstanceColumns._ID + " = ? AND "+ SurveyInstanceColumns.VERSION + " != ?";
        return briteDatabase.update(Tables.SURVEY_INSTANCE, contentValues, where, String.valueOf(formInstanceId), String.valueOf(formVersion));
    }

    public int formVersionUpdateNotified(String formId, double formVersion) {
        Cursor cursor = briteDatabase.query("SELECT " + FormUpdateNotifiedTable.COLUMN_NEW_FORM_VERSION
                + " FROM " + FormUpdateNotifiedTable.TABLE_NAME
                + " WHERE " + FormUpdateNotifiedTable.COLUMN_FORM_ID + " = ? AND " + FormUpdateNotifiedTable.COLUMN_NEW_FORM_VERSION + " = ? ", formId + "", formVersion + "");
        int count = 0;
        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        }
        return count;
    }

    public void saveFormVersionNotified(String formId, double formVersion) {
        ContentValues contentValues = new ContentValues(2);
        contentValues.put(FormUpdateNotifiedTable.COLUMN_FORM_ID, formId);
        contentValues.put(FormUpdateNotifiedTable.COLUMN_NEW_FORM_VERSION, formVersion);
        briteDatabase.insert(FormUpdateNotifiedTable.TABLE_NAME, contentValues);
    }

    public void saveGroup(List<ContentValues> groupValues, String formId) {
        String where = QuestionGroupTable.COLUMN_FORM_ID + " =? ";
        briteDatabase.delete(QuestionGroupTable.TABLE_NAME, where, formId);
        for (ContentValues values: groupValues) {
            briteDatabase.insert(QuestionGroupTable.TABLE_NAME, values);
        }
    }

    public Cursor getGroups(String formId) {
        return briteDatabase.query("SELECT * FROM " + QuestionGroupTable.TABLE_NAME + " WHERE "
                + QuestionGroupTable.COLUMN_FORM_ID + " =? ", formId);
    }

    public void deleteGroups(String formId) {
        briteDatabase.delete(QuestionGroupTable.TABLE_NAME, QuestionGroupTable.COLUMN_FORM_ID + " =? ", formId);
    }

    public Cursor getResponsesToDisplay(Long surveyInstanceId) {
        return briteDatabase.query("SELECT * FROM " + Tables.RESPONSE + " WHERE " +
                        ResponseColumns.SURVEY_INSTANCE_ID + " = ?",
                surveyInstanceId + "");
    }
}
