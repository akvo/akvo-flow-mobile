/*
 * Copyright (C) 2010-2018,2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * Database class for the survey db. It can create/upgrade the database as well
 * as select/insert/update survey responses. TODO: break this up into separate
 * DAOs
 *
 * @author Christopher Fagiani
 */
public class SurveyDbAdapter {

    private static final String SURVEY_INSTANCE_JOIN_SURVEY = "survey_instance "
            + "JOIN survey ON survey_instance.survey_id = survey.survey_id "
            + "JOIN survey_group ON survey.survey_group_id=survey_group.survey_group_id";
    private static final String SURVEY_INSTANCE_JOIN_SURVEY_AND_RESPONSE = "survey_instance "
            + "JOIN survey ON survey_instance.survey_id = survey.survey_id "
            + "JOIN survey_group ON survey.survey_group_id=survey_group.survey_group_id "
            + "JOIN response ON survey_instance._id=response.survey_instance_id";

    private static final String SURVEY_JOIN_SURVEY_INSTANCE =
            "survey LEFT OUTER JOIN survey_instance ON "
                    + "survey.survey_id=survey_instance.survey_id";

    private static final String[] RESPONSE_COLUMNS = {
            ResponseColumns._ID, ResponseColumns.QUESTION_ID, ResponseColumns.ANSWER,
            ResponseColumns.TYPE, ResponseColumns.SURVEY_INSTANCE_ID,
            ResponseColumns.INCLUDE, ResponseColumns.FILENAME, ResponseColumns.ITERATION
    };

    private DatabaseHelper databaseHelper;
    private SQLiteDatabase database;

    private final Context context;

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx the Context within which to work
     */
    public SurveyDbAdapter(Context ctx) {
        this.context = ctx;
    }

    /**
     * Open or create the db
     *
     * @throws SQLException if the database could be neither opened or created
     */
    public SurveyDbAdapter open() throws SQLException {
        databaseHelper = new DatabaseHelper(context, new LanguageTable(), new DataPointDownloadTable());
        database = databaseHelper.getWritableDatabase();
        return this;
    }

    /**
     * close the db
     */
    public void close() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    /**
     * Increment the duration of a particular respondent.
     * The provided value will be added on top of the already stored one (default to 0).
     * This will allow users to pause and resume a survey without considering that
     * time as part of the survey duration.
     *
     * @param sessionDuration time spent in the current session
     */
    public void addSurveyDuration(long respondentId, long sessionDuration) {
        final String sql = "UPDATE " + Tables.SURVEY_INSTANCE
                + " SET " + SurveyInstanceColumns.DURATION + " = "
                + SurveyInstanceColumns.DURATION + " + " + sessionDuration
                + " WHERE " + SurveyInstanceColumns._ID + " = " + respondentId
                + " AND " + SurveyInstanceColumns.SUBMITTED_DATE + " IS NULL";
        database.execSQL(sql);
    }

    public Cursor getResponses(long surveyInstanceId) {
        return database.query(Tables.RESPONSE,
                RESPONSE_COLUMNS,
                ResponseColumns.SURVEY_INSTANCE_ID + " = ?",
                new String[] { String.valueOf(surveyInstanceId) },
                null, null, null);
    }

    /**
     * loads a single question response
     */
    public Cursor getResponse(Long surveyInstanceId, String questionId) {
        return database.query(Tables.RESPONSE,
                RESPONSE_COLUMNS,
                ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND " + ResponseColumns.QUESTION_ID
                        + " =?",
                new String[] { String.valueOf(surveyInstanceId), questionId },
                null, null, null);
    }

    public Cursor getResponse(Long surveyInstanceId, String questionId, int iteration) {
        return database.query(Tables.RESPONSE,
                RESPONSE_COLUMNS,
                ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND " + ResponseColumns.QUESTION_ID
                        + " =? AND " + "CAST(" + ResponseColumns.ITERATION + " as TEXT) = ? ",
                new String[] { String.valueOf(surveyInstanceId), questionId,
                        String.valueOf(iteration)
                },
                null, null, null);
    }

    public long updateSurveyResponse(Long responseToSaveId, ContentValues initialValues) {
        long insertedResponseId = -1;
        if (responseToSaveId == null) {
            insertedResponseId = insertResponse(initialValues);
        } else {
            if (updateResponse(responseToSaveId, initialValues) > 0) {
                insertedResponseId = responseToSaveId;
            }
        }
        return insertedResponseId;
    }

    /**
     * Inserts new response
     *
     * @return the id of the inserted row
     */
    private long insertResponse(ContentValues initialValues) {
        return database.insert(Tables.RESPONSE, null, initialValues);
    }

    private int updateResponse(long responseToSaveId, ContentValues initialValues) {
        return database.update(Tables.RESPONSE, initialValues, ResponseColumns._ID + "=?",
                new String[] { String.valueOf(responseToSaveId) });
    }

    /**
     * creates a new un-submitted survey instance
     */
    public long createSurveyRespondent(ContentValues initialValues) {
        return database.insert(Tables.SURVEY_INSTANCE, null, initialValues);
    }

    /**
     * Gets a single survey from the db using its survey id
     */
    public Cursor getSurvey(String surveyId) {
        return database.query(Tables.SURVEY, new String[] {
                        SurveyColumns.SURVEY_ID, SurveyColumns.NAME, SurveyColumns.LOCATION,
                        SurveyColumns.FILENAME, SurveyColumns.TYPE, SurveyColumns.LANGUAGE,
                        SurveyColumns.HELP_DOWNLOADED, SurveyColumns.VERSION
                }, SurveyColumns.SURVEY_ID + " = ?",
                new String[] {
                        surveyId
                }, null, null, null);
    }

    /**
     * deletes all survey responses from the database for a specific survey instance
     */
    public void deleteResponses(String surveyInstanceId) {
        database.delete(Tables.RESPONSE, ResponseColumns.SURVEY_INSTANCE_ID + "= ?",
                new String[] {
                        surveyInstanceId
                });
    }

    /**
     * deletes the surveyInstanceId record and any responses it contains
     */
    public void deleteSurveyInstance(String surveyInstanceId) {
        deleteResponses(surveyInstanceId);
        database.delete(Tables.SURVEY_INSTANCE, SurveyInstanceColumns._ID + "=?",
                new String[] {
                        surveyInstanceId
                });
    }

    /**
     * deletes a single response
     */
    public void deleteResponse(long surveyInstanceId, String questionId) {
        database.delete(Tables.RESPONSE, ResponseColumns.SURVEY_INSTANCE_ID + "= ? AND "
                + ResponseColumns.QUESTION_ID + "= ?", new String[] {
                String.valueOf(surveyInstanceId),
                questionId
        });
    }

    /**
     * Delete response for a repeated question
     */
    public void deleteResponse(long surveyInstanceId, String questionId, String iteration) {
        database.delete(Tables.RESPONSE,
                ResponseColumns.SURVEY_INSTANCE_ID
                        + "= ? AND "
                        + ResponseColumns.QUESTION_ID
                        + "= ? AND "
                        + ResponseColumns.ITERATION
                        + " = ? ",
                new String[] {
                        String.valueOf(surveyInstanceId), questionId, iteration
                });
    }

    public Cursor getSurveyInstanceTransmissions(long surveyInstanceId) {
        return getTransmissions(TransmissionColumns.SURVEY_INSTANCE_ID + " = ?",
                new String[] {
                        String.valueOf(surveyInstanceId)
                }
        );
    }

    private Cursor getTransmissions(String selection, String[] selectionArgs) {
        return database.query(Tables.TRANSMISSION,
                new String[] {
                        TransmissionColumns._ID, TransmissionColumns.SURVEY_INSTANCE_ID,
                        TransmissionColumns.SURVEY_ID, TransmissionColumns.STATUS,
                        TransmissionColumns.FILENAME, TransmissionColumns.START_DATE,
                        TransmissionColumns.END_DATE
                },
                selection + " AND " + TransmissionColumns.FILENAME + " LIKE '%.%'", selectionArgs,
                null, null, null);
    }

    /**
     * executes a single insert/update/delete DML or any DDL statement without
     * any bind arguments.
     */
    private void executeSql(String sql) {
        database.execSQL(sql);
    }

    /**
     * Permanently deletes user generated data from the database. It will clear
     * any response saved in the database, as well as the transmission history.
     */
    public void clearCollectedData() {
        deleteAllResponses();
        executeSql("DELETE FROM " + Tables.SURVEY_INSTANCE);
        executeSql("DELETE FROM " + Tables.RECORD);
        executeSql("DELETE FROM " + Tables.TRANSMISSION);
    }

    public void deleteAllResponses() {
        executeSql("DELETE FROM " + Tables.RESPONSE);
    }

    public Cursor getSurveyGroup(long id) {
        return database.query(Tables.SURVEY_GROUP,
                new String[] {
                        SurveyGroupColumns._ID, SurveyGroupColumns.SURVEY_GROUP_ID,
                        SurveyGroupColumns.NAME,
                        SurveyGroupColumns.REGISTER_SURVEY_ID, SurveyGroupColumns.MONITORED
                },
                SurveyGroupColumns.SURVEY_GROUP_ID + "= ?",
                new String[] { String.valueOf(id) },
                null, null, null);
    }

    public String createSurveyedLocale(long surveyGroupId, String recordUid) {
        ContentValues values = new ContentValues();
        values.put(RecordColumns.RECORD_ID, recordUid);
        values.put(RecordColumns.SURVEY_GROUP_ID, surveyGroupId);
        database.insert(Tables.RECORD, null, values);

        return recordUid;
    }

    public Cursor getFormInstance(long formInstanceId) {
        return database.query(SURVEY_INSTANCE_JOIN_SURVEY,
                FormInstanceQuery.PROJECTION,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns._ID + "= ?",
                new String[] { String.valueOf(formInstanceId) },
                null, null, null);
    }

    /**
     * Get all the SurveyInstances for a particular data point. Registration form will be at the top
     * of the list, all other forms will be ordered by submission date (desc).
     */
    public Cursor getFormInstances(String recordId) {
        return database.query(SURVEY_INSTANCE_JOIN_SURVEY,
                FormInstanceQuery.PROJECTION,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.RECORD_ID + "= ?",
                new String[] { recordId },
                null, null,
                "CASE WHEN survey.survey_id = survey_group.register_survey_id THEN 0 ELSE 1 END, "
                        + SurveyInstanceColumns.START_DATE + " DESC");
    }

    /**
     * Get all the SurveyInstances for a particular data point which actually have non empty
     * responses. Registration form will be at the top of the list, all other forms will be ordered
     * by submission date (desc).
     */
    public Cursor getFormInstancesWithResponses(String recordId) {
        return database.query(SURVEY_INSTANCE_JOIN_SURVEY_AND_RESPONSE,
                FormInstanceQuery.PROJECTION,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.RECORD_ID + "= ?",
                new String[] { recordId },
                ResponseColumns.SURVEY_INSTANCE_ID, null,
                "CASE WHEN survey.survey_id = survey_group.register_survey_id THEN 0 ELSE 1 END, "
                        + SurveyInstanceColumns.START_DATE + " DESC");
    }

    /**
     * Given a particular surveyedLocale and one of its surveys,
     * retrieves the ID of the last surveyInstance matching that criteria
     *
     * @return last surveyInstance with those attributes
     */
    public Long getLastSurveyInstance(String surveyedLocaleId, String surveyId) {
        Cursor cursor = database.query(Tables.SURVEY_INSTANCE,
                new String[] {
                        SurveyInstanceColumns._ID, SurveyInstanceColumns.RECORD_ID,
                        SurveyInstanceColumns.SURVEY_ID, SurveyInstanceColumns.SUBMITTED_DATE
                },
                SurveyInstanceColumns.RECORD_ID + "= ? AND " + SurveyInstanceColumns.SURVEY_ID
                        + "= ? AND " + SurveyInstanceColumns.SUBMITTED_DATE + " IS NOT NULL",
                new String[] { surveyedLocaleId, surveyId },
                null, null,
                SurveyInstanceColumns.SUBMITTED_DATE + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            long surveyInstanceId = cursor
                    .getLong(cursor.getColumnIndexOrThrow(SurveyInstanceColumns._ID));
            cursor.close();
            return surveyInstanceId;
        }

        return null;
    }

    public String getSurveyedLocaleId(long surveyInstanceId) {
        Cursor cursor = database.query(Tables.SURVEY_INSTANCE,
                new String[] {
                        SurveyInstanceColumns._ID, SurveyInstanceColumns.RECORD_ID
                },
                SurveyInstanceColumns._ID + "= ?",
                new String[] { String.valueOf(surveyInstanceId) },
                null, null, null);

        String id = null;
        if (cursor.moveToFirst()) {
            id = cursor.getString(cursor.getColumnIndexOrThrow(SurveyInstanceColumns.RECORD_ID));
        }
        cursor.close();
        return id;
    }

    public void clearSurveyedLocaleName(long surveyInstanceId) {
        String surveyedLocaleId = getSurveyedLocaleId(surveyInstanceId);
        ContentValues surveyedLocaleValues = new ContentValues();
        surveyedLocaleValues.put(RecordColumns.NAME, "");
        database.update(Tables.RECORD, surveyedLocaleValues,
                RecordColumns.RECORD_ID + " = ?",
                new String[] { surveyedLocaleId });
    }

    /**
     * Flag to indicate the type of locale update from a given response
     */
    public enum SurveyedLocaleMeta {
        NAME, GEOLOCATION
    }

    /**
     * Delete any SurveyInstance that contains no response.
     */
    public void deleteEmptySurveyInstances() {
        executeSql("DELETE FROM " + Tables.SURVEY_INSTANCE
                + " WHERE " + SurveyInstanceColumns._ID + " NOT IN "
                + "(SELECT DISTINCT " + ResponseColumns.SURVEY_INSTANCE_ID
                + " FROM " + Tables.RESPONSE + ")");
    }

    /**
     * Delete any Record that contains no SurveyInstance
     */
    public void deleteEmptyRecords() {
        executeSql("DELETE FROM " + Tables.RECORD
                + " WHERE " + RecordColumns.RECORD_ID + " NOT IN "
                + "(SELECT DISTINCT " + SurveyInstanceColumns.RECORD_ID
                + " FROM " + Tables.SURVEY_INSTANCE + ")");
    }

    /**
     * Query the given table, returning a Cursor over the result set.
     */
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having, String orderBy) {
        return database.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
    }

    public Cursor getDatapointStatus(String recordId) {
        return database.query(Tables.SURVEY_INSTANCE,
                new String[] {
                        SurveyInstanceColumns.STATUS
                },
                SurveyInstanceColumns.RECORD_ID + "= ?",
                new String[] { String.valueOf(recordId) },
                null, null, null);
    }

    public Cursor getDataPointForms(long surveyGroupId, String recordId) {
        String table = SurveyDbAdapter.SURVEY_JOIN_SURVEY_INSTANCE;
        if (recordId != null) {
            // Add record id to the join condition. If put in the where, the left join won't work
            table += " AND " + Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.RECORD_ID
                    + "='" + recordId + "'";
        }
        return database.query(table,
                SurveyQuery.PROJECTION,
                SurveyColumns.SURVEY_GROUP_ID + " = ?",
                new String[] { String.valueOf(surveyGroupId) },
                Tables.SURVEY + "." + SurveyColumns.SURVEY_ID,
                null,
                SurveyColumns.NAME);
    }

    public interface SurveyQuery {
        String[] PROJECTION = {
                Tables.SURVEY + "." + SurveyColumns.SURVEY_ID,
                Tables.SURVEY + "." + SurveyColumns.NAME,
                Tables.SURVEY + "." + SurveyColumns.VERSION,
                Tables.SURVEY + "." + SurveyColumns.DELETED,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SUBMITTED_DATE,
        };

        int SURVEY_ID = 0;
        int NAME = 1;
        int VERSION = 2;
        int DELETED = 3;
        int SUBMITTED = 4;
    }

    // Wrap DB projections and column indexes.
    public interface RecordQuery {
        String[] PROJECTION = {
                RecordColumns._ID,
                RecordColumns.RECORD_ID,
                RecordColumns.SURVEY_GROUP_ID,
                RecordColumns.NAME,
                RecordColumns.LATITUDE,
                RecordColumns.LONGITUDE,
                RecordColumns.LAST_MODIFIED,
        };

        int _ID = 0;
        int RECORD_ID = 1;
        int SURVEY_GROUP_ID = 2;
        int NAME = 3;
        int LATITUDE = 4;
        int LONGITUDE = 5;
        int LAST_MODIFIED = 6;
        int VIEWED = 7;
    }

    public interface FormInstanceQuery {
        String[] PROJECTION = {
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns._ID,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SURVEY_ID,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.VERSION,
                SurveyColumns.NAME,
                SurveyInstanceColumns.SAVED_DATE,
                SurveyInstanceColumns.USER_ID,
                SurveyInstanceColumns.SUBMITTED_DATE,
                SurveyInstanceColumns.UUID,
                SurveyInstanceColumns.STATUS,
                SurveyInstanceColumns.SYNC_DATE,
                SurveyInstanceColumns.EXPORTED_DATE,
                SurveyInstanceColumns.RECORD_ID,
                SurveyInstanceColumns.SUBMITTER,
        };

        int _ID = 0;
        int SURVEY_ID = 1;
        int VERSION = 2;
        int NAME = 3;
        int SAVED_DATE = 4;
        int USER_ID = 5;
        int SUBMITTED_DATE = 6;
        int UUID = 7;
        int STATUS = 8;
        int SYNC_DATE = 9;
        int EXPORTED_DATE = 10;
        int RECORD_ID = 11;
        int SUBMITTER = 12;
    }

}
