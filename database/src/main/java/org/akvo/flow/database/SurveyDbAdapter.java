/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static org.akvo.flow.database.Constants.ORDER_BY_DATE;
import static org.akvo.flow.database.Constants.ORDER_BY_DISTANCE;
import static org.akvo.flow.database.Constants.ORDER_BY_NAME;
import static org.akvo.flow.database.Constants.ORDER_BY_STATUS;

/**
 * Database class for the survey db. It can create/upgrade the database as well
 * as select/insert/update survey responses. TODO: break this up into separate
 * DAOs
 *
 * @author Christopher Fagiani
 */
public class SurveyDbAdapter {

    private static final String SURVEY_INSTANCE_JOIN_RESPONSE_USER = "survey_instance "
            + "LEFT OUTER JOIN response ON survey_instance._id=response.survey_instance_id "
            + "LEFT OUTER JOIN user ON survey_instance.user_id=user._id";
    private static final String SURVEY_INSTANCE_JOIN_SURVEY = "survey_instance "
            + "JOIN survey ON survey_instance.survey_id = survey.survey_id "
            + "JOIN survey_group ON survey.survey_group_id=survey_group.survey_group_id";

    public static final String SURVEY_JOIN_SURVEY_INSTANCE =
            "survey LEFT OUTER JOIN survey_instance ON "
                    + "survey.survey_id=survey_instance.survey_id";

    private DatabaseHelper databaseHelper;
    private SQLiteDatabase database;

    private final Context context;
    private final MigrationListener migrationListener;

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx the Context within which to work
     */
    public SurveyDbAdapter(Context ctx, MigrationListener migrationListener) {
        this.context = ctx;
        this.migrationListener = migrationListener;
    }

    /**
     * Open or create the db
     *
     * @throws SQLException if the database could be neither opened or created
     */
    public SurveyDbAdapter open() throws SQLException {
        databaseHelper = new DatabaseHelper(context, new LanguageTable(), migrationListener);
        database = databaseHelper.getWritableDatabase();
        return this;
    }

    /**
     * close the db
     */
    public void close() {
        databaseHelper.close();
    }

    public Cursor getSurveyInstancesByStatus(int status) {
        return database.query(Tables.SURVEY_INSTANCE,
                new String[] { SurveyInstanceColumns._ID, SurveyInstanceColumns.UUID },
                SurveyInstanceColumns.STATUS + " = ?",
                new String[] { String.valueOf(status) },
                null, null, null);
    }

    public Cursor getResponsesData(long surveyInstanceId) {
        return database.query(SURVEY_INSTANCE_JOIN_RESPONSE_USER,
                new String[] {
                        SurveyInstanceColumns.SURVEY_ID, SurveyInstanceColumns.SUBMITTED_DATE,
                        SurveyInstanceColumns.UUID, SurveyInstanceColumns.START_DATE,
                        SurveyInstanceColumns.RECORD_ID, SurveyInstanceColumns.DURATION,
                        ResponseColumns.ANSWER, ResponseColumns.TYPE, ResponseColumns.QUESTION_ID,
                        ResponseColumns.FILENAME, UserColumns.NAME, UserColumns.EMAIL
                },
                ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND " + ResponseColumns.INCLUDE + " = 1",
                new String[] {
                        String.valueOf(surveyInstanceId)
                }, null, null, null);
    }

    /**
     * updates the status of a survey instance to the status passed in.
     * Status must be one of the 'SurveyInstanceStatus' one. The corresponding
     * Date column will be updated with the current timestamp.
     *
     * @param surveyInstanceId
     * @param status
     */
    public void updateSurveyStatus(long surveyInstanceId, int status) {
        String dateColumn;
        switch (status) {
            case SurveyInstanceStatus.DOWNLOADED:
            case SurveyInstanceStatus.SYNCED:
                dateColumn = SurveyInstanceColumns.SYNC_DATE;
                break;
            case SurveyInstanceStatus.EXPORTED:
                dateColumn = SurveyInstanceColumns.EXPORTED_DATE;
                break;
            case SurveyInstanceStatus.SUBMITTED:
                dateColumn = SurveyInstanceColumns.SUBMITTED_DATE;
                break;
            case SurveyInstanceStatus.SAVED:
                dateColumn = SurveyInstanceColumns.SAVED_DATE;
                break;
            default:
                return;// Nothing to see here, buddy
        }

        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyInstanceColumns.STATUS, status);
        updatedValues.put(dateColumn, System.currentTimeMillis());

        final int rows = database.update(Tables.SURVEY_INSTANCE,
                updatedValues,
                SurveyInstanceColumns._ID + " = ?",
                new String[] { String.valueOf(surveyInstanceId) });

        if (rows < 1) {
            Timber.e("Could not update status for Survey Instance: %d", surveyInstanceId);
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

    /**
     * returns a cursor listing all users
     *
     * @return
     */
    public Cursor getUsers() {
        Cursor cursor = database.query(Tables.USER,
                new String[] { UserColumns._ID, UserColumns.NAME, UserColumns.EMAIL },
                UserColumns.DELETED + " <> ?",
                new String[] { "1" },
                null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * retrieves a user by ID
     *
     * @param id
     * @return
     */
    public Cursor getUser(Long id) {
        Cursor cursor = database.query(Tables.USER,
                new String[] { UserColumns._ID, UserColumns.NAME, UserColumns.EMAIL },
                UserColumns._ID + "=?",
                new String[] { id.toString() },
                null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * if the ID is populated, this will update a user record. Otherwise, it
     * will be inserted
     */
    public long createOrUpdateUser(Long id, String name) {
        ContentValues initialValues = new ContentValues();
        Long idVal = id;
        initialValues.put(UserColumns.NAME, name);
        initialValues.put(UserColumns.DELETED, 0);

        if (idVal == null) {
            idVal = database.insert(Tables.USER, null, initialValues);
        } else {
            database.update(Tables.USER, initialValues, UserColumns._ID + "=?",
                    new String[] { idVal.toString() });
        }
        return idVal;
    }

    public Cursor getResponses(long surveyInstanceId) {
        return database.query(Tables.RESPONSE,
                new String[] {
                        ResponseColumns._ID, ResponseColumns.QUESTION_ID, ResponseColumns.ANSWER,
                        ResponseColumns.TYPE, ResponseColumns.SURVEY_INSTANCE_ID,
                        ResponseColumns.INCLUDE, ResponseColumns.FILENAME
                },
                ResponseColumns.SURVEY_INSTANCE_ID + " = ?",
                new String[] { String.valueOf(surveyInstanceId) },
                null, null, null);
    }

    /**
     * loads a single question response
     *
     * @param surveyInstanceId
     * @param questionId
     * @return
     */
    public Cursor getResponse(Long surveyInstanceId, String questionId) {
        return database.query(Tables.RESPONSE,
                new String[] {
                        ResponseColumns._ID, ResponseColumns.QUESTION_ID, ResponseColumns.ANSWER,
                        ResponseColumns.TYPE, ResponseColumns.SURVEY_INSTANCE_ID,
                        ResponseColumns.INCLUDE, ResponseColumns.FILENAME
                },
                ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND " + ResponseColumns.QUESTION_ID
                        + " =?",
                new String[] { String.valueOf(surveyInstanceId), questionId },
                null, null, null);
    }

    public long updateSurveyResponse(Long responseToSaveId, long id,
            ContentValues initialValues) {
        if (responseToSaveId == null) {
            id = database.insert(Tables.RESPONSE, null, initialValues);
        } else {
            if (database.update(Tables.RESPONSE, initialValues, ResponseColumns._ID
                    + "=?", new String[] {
                    responseToSaveId.toString()
            }) > 0) {
                id = responseToSaveId;
            }
        }
        return id;
    }

    /**
     * creates a new unsubmitted survey instance
     */
    public long createSurveyRespondent(ContentValues initialValues) {
        return database.insert(Tables.SURVEY_INSTANCE, null, initialValues);
    }

    //TODO: verify method naming
    public Cursor checkSurveyVersion(String surveyId, String surveyVersion) {
        return database.query(Tables.SURVEY,
                new String[] {
                        SurveyColumns.SURVEY_ID
                },
                SurveyColumns.SURVEY_ID + " = ? and (" + SurveyColumns.VERSION + " >= ? or "
                        + SurveyColumns.DELETED + " = ?)", new String[] {
                        surveyId,
                        surveyVersion,
                        String.valueOf(1)//ConstantUtil.IS_DELETED
                }, null, null, null);
    }

    /**
     * updates the survey table by recording the help download flag
     */
    public void markSurveyHelpDownloaded(String surveyId, boolean isDownloaded) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyColumns.HELP_DOWNLOADED, isDownloaded ? 1 : 0);

        if (database.update(Tables.SURVEY, updatedValues, SurveyColumns.SURVEY_ID + " = ?",
                new String[] {
                        surveyId
                }) < 1) {
            Timber.e("Could not update record for Survey %s", surveyId);
        }
    }

    @Nullable
    public Cursor updateSurvey(ContentValues updatedValues, String surveyId) {
        Cursor cursor = database.query(Tables.SURVEY,
                new String[] {
                        SurveyColumns._ID
                }, SurveyColumns.SURVEY_ID + " = ?",
                new String[] {
                        surveyId,
                }, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            // if we found an item, it's an update, otherwise, it's an insert
            database.update(Tables.SURVEY, updatedValues, SurveyColumns.SURVEY_ID + " = ?",
                    new String[] {
                            surveyId
                    });
        } else {
            database.insert(Tables.SURVEY, null, updatedValues);
        }
        return cursor;
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
     * deletes all the surveys from the database
     */
    public void deleteAllSurveys() {
        database.delete(Tables.SURVEY, null, null);
        database.delete(Tables.SURVEY_GROUP, null, null);
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
     *
     * @param surveyInstanceId
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
     *
     * @param surveyInstanceId
     * @param questionId
     */
    public void deleteResponse(long surveyInstanceId, String questionId) {
        database.delete(Tables.RESPONSE, ResponseColumns.SURVEY_INSTANCE_ID + "= ? AND "
                + ResponseColumns.QUESTION_ID + "= ?", new String[] {
                String.valueOf(surveyInstanceId),
                questionId
        });
    }

    public void createTransmission(long surveyInstanceId, String formID, String filename) {
        createTransmission(surveyInstanceId, formID, filename, TransmissionStatus.QUEUED);
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
        database.insert(Tables.TRANSMISSION, null, values);
    }

    /**
     * Updates the matching transmission history records with the status
     * passed in. If the status == Completed, the completion date is updated. If
     * the status == In Progress, the start date is updated.
     *
     * @param fileName
     * @param status
     * @return the number of rows affected
     */
    public int updateTransmissionHistory(String fileName, int status) {
        // TODO: Update Survey Instance STATUS as well
        ContentValues vals = new ContentValues();
        vals.put(TransmissionColumns.STATUS, status);
        if (TransmissionStatus.SYNCED == status) {
            vals.put(TransmissionColumns.END_DATE, System.currentTimeMillis() + "");
        } else if (TransmissionStatus.IN_PROGRESS == status) {
            vals.put(TransmissionColumns.START_DATE, System.currentTimeMillis() + "");
        }

        return database.update(Tables.TRANSMISSION, vals,
                TransmissionColumns.FILENAME + " = ?",
                new String[] { fileName });
    }

    public Cursor getFileTransmissions(long surveyInstanceId) {
        return database.query(Tables.TRANSMISSION,
                new String[] {
                        TransmissionColumns._ID, TransmissionColumns.SURVEY_INSTANCE_ID,
                        TransmissionColumns.SURVEY_ID, TransmissionColumns.STATUS,
                        TransmissionColumns.FILENAME, TransmissionColumns.START_DATE,
                        TransmissionColumns.END_DATE
                },
                TransmissionColumns.SURVEY_INSTANCE_ID + " = ?",
                new String[] { String.valueOf(surveyInstanceId) },
                null, null, null);
    }

    public Cursor getUnsyncedTransmissions() {
        return database.query(Tables.TRANSMISSION,
                new String[] {
                        TransmissionColumns._ID, TransmissionColumns.SURVEY_INSTANCE_ID,
                        TransmissionColumns.SURVEY_ID, TransmissionColumns.STATUS,
                        TransmissionColumns.FILENAME, TransmissionColumns.START_DATE,
                        TransmissionColumns.END_DATE
                },
                TransmissionColumns.STATUS + " IN (?, ?, ?)",
                new String[] {
                        String.valueOf(TransmissionStatus.FAILED),
                        String.valueOf(TransmissionStatus.IN_PROGRESS),// Stalled IN_PROGRESS files
                        String.valueOf(TransmissionStatus.QUEUED)
                }, null, null, null);
    }

    /**
     * executes a single insert/update/delete DML or any DDL statement without
     * any bind arguments.
     *
     * @param sql
     */
    private void executeSql(String sql) {
        database.execSQL(sql);
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
        database.insert(Tables.SURVEY, null, values);
    }

    /**
     * permanently deletes all surveys, responses, users and transmission
     * history from the database
     */
    public void clearAllData() {
        // User generated data
        clearCollectedData();

        // Surveys and preferences
        executeSql("DELETE FROM " + Tables.SURVEY);
        executeSql("DELETE FROM " + Tables.SURVEY_GROUP);
        executeSql("DELETE FROM " + Tables.USER);
    }

    /**
     * Permanently deletes user generated data from the database. It will clear
     * any response saved in the database, as well as the transmission history.
     */
    public void clearCollectedData() {
        executeSql("DELETE FROM " + Tables.SYNC_TIME);
        executeSql("DELETE FROM " + Tables.RESPONSE);
        executeSql("DELETE FROM " + Tables.SURVEY_INSTANCE);
        executeSql("DELETE FROM " + Tables.RECORD);
        executeSql("DELETE FROM " + Tables.TRANSMISSION);
    }

    /**
     * performs a soft-delete on a user
     *
     * @param id
     */
    public void deleteUser(Long id) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(UserColumns.DELETED, 1);
        database.update(Tables.USER, updatedValues, UserColumns._ID + " = ?",
                new String[] {
                        id.toString()
                });
    }

    public void addSurveyGroup(ContentValues values) {
        database.insert(Tables.SURVEY_GROUP, null, values);
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

    public Cursor getSurveyGroups() {
        return database.query(Tables.SURVEY_GROUP,
                new String[] {
                        SurveyGroupColumns._ID, SurveyGroupColumns.SURVEY_GROUP_ID,
                        SurveyGroupColumns.NAME,
                        SurveyGroupColumns.REGISTER_SURVEY_ID, SurveyGroupColumns.MONITORED
                },
                null, null, null, null, SurveyGroupColumns.NAME);
    }

    public String createSurveyedLocale(long surveyGroupId, String recordUid) {
        ContentValues values = new ContentValues();
        values.put(RecordColumns.RECORD_ID, recordUid);
        values.put(RecordColumns.SURVEY_GROUP_ID, surveyGroupId);
        database.insert(Tables.RECORD, null, values);

        return recordUid;
    }

    public Cursor getSurveyedLocales(long surveyGroupId) {
        return database.query(Tables.RECORD, RecordQuery.PROJECTION,
                RecordColumns.SURVEY_GROUP_ID + " = ?",
                new String[] { String.valueOf(surveyGroupId) },
                null, null, null);
    }

    public Cursor getSurveyedLocale(String surveyedLocaleId) {
        return database.query(Tables.RECORD, RecordQuery.PROJECTION,
                RecordColumns.RECORD_ID + " = ?",
                new String[] { String.valueOf(surveyedLocaleId) },
                null, null, null);
    }

    public String[] getSurveyIds() {
        Cursor c = getSurveys(-1);// All survey groups
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

    public Cursor getSurveys(long surveyGroupId) {
        String whereClause = SurveyColumns.DELETED + " <> 1";
        String[] whereParams = null;
        if (surveyGroupId > 0) {
            whereClause += " AND " + SurveyColumns.SURVEY_GROUP_ID + " = ?";
            whereParams = new String[] {
                    String.valueOf(surveyGroupId)
            };
        }

        return database.query(Tables.SURVEY, new String[] {
                        SurveyColumns._ID, SurveyColumns.SURVEY_ID, SurveyColumns.NAME,
                        SurveyColumns.FILENAME, SurveyColumns.TYPE, SurveyColumns.LANGUAGE,
                        SurveyColumns.HELP_DOWNLOADED, SurveyColumns.VERSION, SurveyColumns.LOCATION
                },
                whereClause, whereParams, null, null, null);
    }

    public void deleteSurveyGroup(long surveyGroupId) {
        // First the group
        database.delete(Tables.SURVEY_GROUP, SurveyGroupColumns.SURVEY_GROUP_ID + " = ? ",
                new String[] { String.valueOf(surveyGroupId) });
        // Now the surveys
        database.delete(Tables.SURVEY, SurveyColumns.SURVEY_GROUP_ID + " = ? ",
                new String[] { String.valueOf(surveyGroupId) });
    }

    /**
     * marks a survey record identified by the ID passed in as deleted.
     *
     * @param surveyId
     */
    public void deleteSurvey(String surveyId) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyColumns.DELETED, 1);
        database.update(Tables.SURVEY, updatedValues, SurveyColumns.SURVEY_ID + " = ?",
                new String[] { surveyId });
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
     * Get SurveyInstances with a particular status.
     * If the recordId is not null, results will be filtered by Record.
     */
    public long[] getFormInstances(String recordId, String surveyId, int status) {
        String where = Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SURVEY_ID + "= ?" +
                " AND " + SurveyInstanceColumns.STATUS + "= ?" +
                " AND " + SurveyInstanceColumns.RECORD_ID + "= ?";
        List<String> args = new ArrayList<>();
        args.add(surveyId);
        args.add(String.valueOf(status));
        args.add(recordId);

        Cursor c = database.query(Tables.SURVEY_INSTANCE,
                new String[] { SurveyInstanceColumns._ID },
                where, args.toArray(new String[args.size()]),
                null, null, SurveyInstanceColumns.START_DATE + " DESC");

        long[] instances = new long[0];// Avoid null array
        if (c != null) {
            instances = new long[c.getCount()];
            if (c.moveToFirst()) {
                do {
                    instances[c.getPosition()] = c.getLong(0);// Single column (ID)
                } while (c.moveToNext());
            }
            c.close();
        }
        return instances;
    }

    /**
     * Given a particular surveyedLocale and one of its surveys,
     * retrieves the ID of the last surveyInstance matching that criteria
     *
     * @param surveyedLocaleId
     * @param surveyId
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

    /**
     * Flag to indicate the type of locale update from a given response
     */
    public enum SurveyedLocaleMeta {
        NAME, GEOLOCATION
    }

    /**
     * Filters surveyd locales based on the parameters passed in.
     */
    //TODO: remove
    public Cursor getFilteredSurveyedLocales(long surveyGroupId, Double latitude, Double longitude,
            int orderBy) {
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
            case ORDER_BY_DATE:
                orderByStr = " ORDER BY " + RecordColumns.LAST_MODIFIED + " DESC";// By date
                break;
            case ORDER_BY_DISTANCE:
                if (latitude != null && longitude != null) {
                    // this is to correct the distance for the shortening at higher latitudes
                    Double fudge = Math.pow(Math.cos(Math.toRadians(latitude)), 2);

                    // this uses a simple planar approximation of distance. this should be good enough for our purpose.
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
            case ORDER_BY_STATUS:
                orderByStr = " ORDER BY " + " MIN(r." + SurveyInstanceColumns.STATUS + ")";
                break;
            case ORDER_BY_NAME:
                orderByStr = " ORDER BY " + RecordColumns.NAME + " COLLATE NOCASE ASC";// By name
                break;
        }

        String[] whereValues = new String[] { String.valueOf(surveyGroupId) };
        return database.rawQuery(queryString + whereClause + groupBy + orderByStr, whereValues);
    }

    /**
     * Get the synchronization time for a particular survey group.
     *
     * @param surveyGroupId id of the SurveyGroup
     * @return time if exists for this key, null otherwise
     */
    public String getSyncTime(long surveyGroupId) {
        Cursor cursor = database.query(Tables.SYNC_TIME,
                new String[] { SyncTimeColumns.SURVEY_GROUP_ID, SyncTimeColumns.TIME },
                SyncTimeColumns.SURVEY_GROUP_ID + "=?",
                new String[] { String.valueOf(surveyGroupId) },
                null, null, null);

        String time = null;
        if (cursor.moveToFirst()) {
            time = cursor.getString(cursor.getColumnIndexOrThrow(SyncTimeColumns.TIME));
        }
        cursor.close();
        return time;
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
