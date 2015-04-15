/*
 *  Copyright (C) 2010-2015 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import org.akvo.flow.R;
import org.akvo.flow.domain.FileTransmission;
import org.akvo.flow.domain.Instance;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyInstance;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.domain.User;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.Prefs;

/**
 * Database class for the survey db. It can create/upgrade the database as well
 * as select/insert/update survey responses. TODO: break this up into separate
 * DAOs
 * 
 * @author Christopher Fagiani
 */
public class SurveyDbAdapter {

    public interface Tables {
        String SURVEY = "survey";
        String SURVEY_INSTANCE = "survey_instance";
        String RESPONSE = "response";
        String USER = "user";
        String TRANSMISSION = "transmission";
        String SURVEY_GROUP = "survey_group";
        String RECORD = "record";
        String SYNC_TIME = "sync_time";
        String INSTANCE = "instance";

        String SURVEY_INSTANCE_JOIN_RESPONSE_USER = "survey_instance "
                + "LEFT OUTER JOIN response ON survey_instance._id=response.survey_instance_id "
                + "LEFT OUTER JOIN user ON survey_instance.user_id=user._id";

        String SURVEY_INSTANCE_JOIN_SURVEY = "survey_instance "
                + "JOIN survey ON survey_instance.survey_id = survey.survey_id";

        String SURVEY_JOIN_SURVEY_INSTANCE = "survey LEFT OUTER JOIN survey_instance ON "
                + "survey.survey_id=survey_instance.survey_id";

        String TRANSMISSION_JOIN_SURVEY_INSTANCE = "transmission JOIN survey_instance ON "
                + "transmission.survey_instance_id=survey_instance._id";
    }

    public interface InstanceColumns {
        String _ID               = "_id";
        String APP_ID            = "app_id";// Unique ID
        String ALIAS             = "alias";// Friendly name
        String AWS_BUCKET        = "aws_bucket";
        String AWS_ACCESS_KEY_ID = "aws_access_key_id";
        String AWS_SECRET_KEY    = "aws_secret_key";
        String SERVER_BASE       = "server_base";
        String API_KEY           = "api_key";
    }

    public interface SurveyGroupColumns {
        String _ID                = "_id";
        String SURVEY_GROUP_ID    = "survey_group_id";
        String NAME               = "name";
        String REGISTER_SURVEY_ID = "register_survey_id";
        String MONITORED          = "monitored";
        String APP_ID             = "app_id";
    }
    
    public interface RecordColumns {
        String _ID                = "_id";
        String RECORD_ID          = "record_id";
        String SURVEY_GROUP_ID    = "survey_group_id";
        String NAME               = "name";
        String LATITUDE           = "latitude";
        String LONGITUDE          = "longitude";
        String LAST_MODIFIED      = "last_modified";
        String APP_ID             = "app_id";
    }
    
    public interface SyncTimeColumns {
        String _ID             = "_id";
        String SURVEY_GROUP_ID = "survey_group_id";
        String TIME            = "time";
        String APP_ID          = "app_id";
    }

    /**
     * Submitter is a denormalized value of the user_id.name in locally created surveys, whereas
     * on synced surveys, it just represents the name of the submitter (not matching a local user).
     * This is just a temporary implementation before a more robust login system is integrated.
     */
    public interface SurveyInstanceColumns {
        String _ID = "_id";
        String UUID = "uuid";
        String SURVEY_ID = "survey_id";
        String USER_ID = "user_id";
        String RECORD_ID = "surveyed_locale_id";
        String START_DATE = "start_date";
        String SAVED_DATE = "saved_date";
        String SUBMITTED_DATE = "submitted_date";
        String EXPORTED_DATE = "exported_date";
        String SYNC_DATE = "sync_date";
        String STATUS = "status";// Denormalized value. See 'SurveyInstanceStatus'
        String DURATION = "duration";
        String SUBMITTER = "submitter";// Submitter name. Added in DB version 79
        String APP_ID = "app_id";
    }

    public interface TransmissionColumns {
        String _ID = "_id";
        String SURVEY_INSTANCE_ID = "survey_instance_id";
        String FILENAME = "filename";
        String STATUS = "status";// separate table/constants?
        String START_DATE = "start_date";// do we really need this column?
        String END_DATE = "end_date";
    }

    public interface UserColumns {
        String _ID = "_id";
        String NAME = "name";
        String EMAIL = "email";
        String DELETED = "deleted";// 0 or 1
    }

    public interface SurveyColumns {
        String _ID = "_id";
        String SURVEY_ID = "survey_id";
        String SURVEY_GROUP_ID = "survey_group_id";
        String NAME = "display_name";
        String VERSION = "version";
        String TYPE = "type";
        String LOCATION = "location";
        String FILENAME = "filename";
        String LANGUAGE = "language";
        String HELP_DOWNLOADED = "help_downloaded_flag";
        String DELETED = "deleted";
        String APP_ID = "app_id";
    }

    public interface ResponseColumns {
        String _ID = "_id";
        String SURVEY_INSTANCE_ID = "survey_instance_id";
        String QUESTION_ID = "question_id";
        String ANSWER = "answer";
        String TYPE = "type";
        String INCLUDE = "include";
        String SCORED_VAL = "scored_val";
        String STRENGTH = "strength";
    }

    public interface SurveyInstanceStatus {
        int SAVED      = 0;
        int SUBMITTED  = 1;
        int EXPORTED   = 2;
        int SYNCED     = 3;
        int DOWNLOADED = 4;
    }

    public interface TransmissionStatus {
        int QUEUED      = 0;
        int IN_PROGRESS = 1;
        int SYNCED      = 2;
        int FAILED      = 3;
    }

    private static final String TAG = "SurveyDbAdapter";
    private DatabaseHelper databaseHelper;
    private SQLiteDatabase database;

    private static final String DATABASE_NAME = "surveydata";

    private static final int VER_LAUNCH = 78;// App refactor version. Start from scratch
    private static final int VER_FORM_SUBMITTER = 79;
    private static final int VER_SINGLE_APP = 80;
    private static final int DATABASE_VERSION = VER_SINGLE_APP;

    private final Context context;

    /**
     * Helper class for creating the database tables and loading reference data
     * It is declared with package scope for VM optimizations
     * 
     * @author Christopher Fagiani
     */
    static class DatabaseHelper extends SQLiteOpenHelper {
        private static SQLiteDatabase database;
        private static volatile Object LOCK_OBJ = new Object();
        private volatile static int instanceCount = 0;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + Tables.USER + " ("
                    + UserColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + UserColumns.NAME + " TEXT NOT NULL,"
                    + UserColumns.EMAIL + " TEXT,"
                    + UserColumns.DELETED + " INTEGER NOT NULL DEFAULT 0)");

            db.execSQL("CREATE TABLE " + Tables.SURVEY + " ("
                    + SurveyColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + SurveyColumns.SURVEY_ID + " TEXT NOT NULL,"
                    + SurveyColumns.SURVEY_GROUP_ID + " INTEGER,"// REFERENCES ...
                    + SurveyColumns.NAME + " TEXT NOT NULL,"
                    + SurveyColumns.VERSION + " REAL,"
                    + SurveyColumns.TYPE + " TEXT,"
                    + SurveyColumns.LOCATION + " TEXT,"
                    + SurveyColumns.FILENAME + " TEXT,"
                    + SurveyColumns.LANGUAGE + " TEXT,"
                    + SurveyColumns.HELP_DOWNLOADED + " INTEGER NOT NULL DEFAULT 0,"
                    + SurveyColumns.DELETED + " INTEGER NOT NULL DEFAULT 0,"
                    + SurveyColumns.APP_ID + " TEXT NOT NULL,"
                    + "UNIQUE (" + SurveyColumns.SURVEY_ID + ") ON CONFLICT REPLACE)");

            db.execSQL("CREATE TABLE " + Tables.SURVEY_GROUP + " ("
                    + SurveyGroupColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + SurveyGroupColumns.SURVEY_GROUP_ID + " INTEGER,"
                    + SurveyGroupColumns.NAME + " TEXT,"
                    + SurveyGroupColumns.REGISTER_SURVEY_ID + " TEXT,"
                    + SurveyGroupColumns.MONITORED + " INTEGER NOT NULL DEFAULT 0,"
                    + SurveyGroupColumns.APP_ID + " TEXT NOT NULL,"
                    + "UNIQUE (" + SurveyGroupColumns.SURVEY_GROUP_ID + ") ON CONFLICT REPLACE)");

            db.execSQL("CREATE TABLE " + Tables.SURVEY_INSTANCE + " ("
                    + SurveyInstanceColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + SurveyInstanceColumns.UUID + " TEXT,"
                    + SurveyInstanceColumns.SURVEY_ID + " TEXT NOT NULL,"// REFERENCES ...
                    + SurveyInstanceColumns.USER_ID + " INTEGER,"
                    + SurveyInstanceColumns.START_DATE + " INTEGER,"
                    + SurveyInstanceColumns.SAVED_DATE + " INTEGER,"
                    + SurveyInstanceColumns.SUBMITTED_DATE + " INTEGER,"
                    + SurveyInstanceColumns.RECORD_ID + " TEXT,"
                    + SurveyInstanceColumns.STATUS + " INTEGER,"
                    + SurveyInstanceColumns.EXPORTED_DATE + " INTEGER,"
                    + SurveyInstanceColumns.SYNC_DATE + " INTEGER,"
                    + SurveyInstanceColumns.DURATION + " INTEGER NOT NULL DEFAULT 0,"
                    + SurveyInstanceColumns.SUBMITTER + " TEXT,"
                    + SurveyInstanceColumns.APP_ID + " TEXT NOT NULL,"
                    + "UNIQUE (" + SurveyInstanceColumns.UUID + ") ON CONFLICT REPLACE)");

            db.execSQL("CREATE TABLE " + Tables.RESPONSE + " ("
                    + ResponseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ResponseColumns.SURVEY_INSTANCE_ID + " INTEGER NOT NULL,"// REFERENCES...
                    + ResponseColumns.QUESTION_ID + " TEXT NOT NULL,"
                    + ResponseColumns.ANSWER + " TEXT NOT NULL,"
                    + ResponseColumns.TYPE + " TEXT NOT NULL,"
                    + ResponseColumns.INCLUDE + " INTEGER NOT NULL DEFAULT 1,"
                    + ResponseColumns.SCORED_VAL + " TEXT,"
                    + ResponseColumns.STRENGTH + " TEXT)");

            db.execSQL("CREATE TABLE " + Tables.RECORD + " ("
                    + RecordColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + RecordColumns.RECORD_ID + " TEXT,"
                    + RecordColumns.SURVEY_GROUP_ID + " INTEGER,"// REFERENCES ...
                    + RecordColumns.NAME + " TEXT,"// REFERENCES ...
                    + RecordColumns.LATITUDE + " REAL,"// REFERENCES ...
                    + RecordColumns.LONGITUDE + " REAL,"// REFERENCES ...
                    + RecordColumns.LAST_MODIFIED + " INTEGER NOT NULL DEFAULT 0,"
                    + RecordColumns.APP_ID + " TEXT NOT NULL,"
                    + "UNIQUE (" + RecordColumns.RECORD_ID + ") ON CONFLICT REPLACE)");

            db.execSQL("CREATE TABLE " + Tables.TRANSMISSION + " ("
                    + TransmissionColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + TransmissionColumns.SURVEY_INSTANCE_ID + " INTEGER NOT NULL,"
                    + TransmissionColumns.FILENAME + " TEXT,"
                    + TransmissionColumns.STATUS + " INTEGER,"
                    + TransmissionColumns.START_DATE + " INTEGER,"
                    + TransmissionColumns.END_DATE + " INTEGER,"
                    + "UNIQUE (" + TransmissionColumns.FILENAME + ") ON CONFLICT REPLACE)");

            db.execSQL("CREATE TABLE " + Tables.SYNC_TIME + " ("
                    + SyncTimeColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + SyncTimeColumns.SURVEY_GROUP_ID + " INTEGER,"
                    + SyncTimeColumns.TIME + " TEXT,"
                    + SyncTimeColumns.APP_ID + " TEXT NOT NULL,"
                    + "UNIQUE (" + SyncTimeColumns.SURVEY_GROUP_ID + ") ON CONFLICT REPLACE)");

            db.execSQL("CREATE TABLE " + Tables.INSTANCE + " ("
                    + InstanceColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + InstanceColumns.APP_ID + " TEXT NOT NULL,"
                    + InstanceColumns.ALIAS + " TEXT,"
                    + InstanceColumns.SERVER_BASE + " TEXT NOT NULL,"
                    + InstanceColumns.AWS_BUCKET + " TEXT NOT NULL,"
                    + InstanceColumns.AWS_ACCESS_KEY_ID + " TEXT NOT NULL,"
                    + InstanceColumns.AWS_SECRET_KEY + " TEXT NOT NULL,"
                    + InstanceColumns.API_KEY + " TEXT NOT NULL,"
                    + "UNIQUE (" + InstanceColumns.APP_ID + ") ON CONFLICT REPLACE)");

            createIndexes(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
            
            int version = oldVersion;
            
            // Apply database updates sequentially. It starts in the current 
            // version, hooking into the correspondent case block, and falls 
            // through to any future upgrade. If no break statement is found,
            // the upgrade will end up in the current version.
            switch (version) {
                case VER_LAUNCH:
                    // Add form submitter
                    db.execSQL("ALTER TABLE " + Tables.SURVEY_INSTANCE
                            + " ADD COLUMN " + SurveyInstanceColumns.SUBMITTER + " TEXT");
                    version = VER_FORM_SUBMITTER;
                case VER_FORM_SUBMITTER:
                    db.execSQL("ALTER TABLE " + Tables.SURVEY
                            + " ADD COLUMN " + SurveyColumns.APP_ID + " TEXT");
                    db.execSQL("ALTER TABLE " + Tables.SURVEY_INSTANCE
                            + " ADD COLUMN " + SurveyInstanceColumns.APP_ID + " TEXT");
                    db.execSQL("ALTER TABLE " + Tables.RECORD
                            + " ADD COLUMN " + RecordColumns.APP_ID + " TEXT");
                    db.execSQL("ALTER TABLE " + Tables.SURVEY_GROUP
                            + " ADD COLUMN " + SurveyGroupColumns.APP_ID + " TEXT");
                    db.execSQL("ALTER TABLE " + Tables.SYNC_TIME
                            + " ADD COLUMN " + SyncTimeColumns.APP_ID + " TEXT");

                    db.execSQL("CREATE TABLE " + Tables.INSTANCE + " ("
                            + InstanceColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + InstanceColumns.APP_ID + " TEXT NOT NULL,"
                            + InstanceColumns.ALIAS + " TEXT,"
                            + InstanceColumns.SERVER_BASE + " TEXT NOT NULL,"
                            + InstanceColumns.AWS_BUCKET + " TEXT NOT NULL,"
                            + InstanceColumns.AWS_ACCESS_KEY_ID + " TEXT NOT NULL,"
                            + InstanceColumns.AWS_SECRET_KEY + " TEXT NOT NULL,"
                            + InstanceColumns.API_KEY + " TEXT NOT NULL,"
                            + "UNIQUE (" + InstanceColumns.APP_ID + ") ON CONFLICT REPLACE)");
                    version = VER_SINGLE_APP;
            }

            if (version != DATABASE_VERSION) {
                Log.d(TAG, "onUpgrade() - Recreating the Database.");
                
                db.execSQL("DROP TABLE IF EXISTS " + Tables.RESPONSE);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.SYNC_TIME);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.SURVEY);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.USER);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.SURVEY_GROUP);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.SURVEY_INSTANCE);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.RECORD);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.TRANSMISSION);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.INSTANCE);

                onCreate(db);
            }
        }

        @Override
        public SQLiteDatabase getWritableDatabase() {
            synchronized (LOCK_OBJ) {

                if (database == null || !database.isOpen()) {
                    database = super.getWritableDatabase();
                    instanceCount = 0;
                }
                instanceCount++;
                return database;
            }
        }

        @Override
        public void close() {
            synchronized (LOCK_OBJ) {
                instanceCount--;
                if (instanceCount <= 0) {
                    // close the database held by the helper (if any)
                    super.close();
                    if (database != null && database.isOpen()) {
                        // we may be holding a different database than the
                        // helper so
                        // close that too if it's still open.
                        database.close();
                    }
                    database = null;
                }
            }
        }
        
        private void createIndexes(SQLiteDatabase db) {
            // Included in point updates
            db.execSQL("CREATE INDEX response_idx ON " + Tables.RESPONSE + "("
                    + ResponseColumns.SURVEY_INSTANCE_ID + ", " + ResponseColumns.QUESTION_ID + ")");
            db.execSQL("CREATE INDEX record_name_idx ON " + Tables.RECORD
                    + "(" + RecordColumns.NAME +")");
            db.execSQL("CREATE INDEX response_status_idx ON " + Tables.SURVEY_INSTANCE
                    + "(" + SurveyInstanceColumns.STATUS +")");
            db.execSQL("CREATE INDEX response_modified_idx ON " + Tables.SURVEY_INSTANCE
                    + "(" + SurveyInstanceColumns.SUBMITTED_DATE +")");
        }

    }

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
        databaseHelper = new DatabaseHelper(context);
        database = databaseHelper.getWritableDatabase();
        return this;
    }

    /**
     * close the db
     */
    public void close() {
        databaseHelper.close();
    }

    public Cursor getSurveyInstancesByStatus(String appId, int status) {
        return database.query(Tables.SURVEY_INSTANCE,
                new String[] { SurveyInstanceColumns._ID, SurveyInstanceColumns.UUID },
                SurveyInstanceColumns.APP_ID + "=? AND " + SurveyInstanceColumns.STATUS + "=?",
                new String[] { appId, String.valueOf(status) },
                null, null, null);
    }

    public Cursor getResponsesData(long surveyInstanceId) {
        return database.query(Tables.SURVEY_INSTANCE_JOIN_RESPONSE_USER,
                new String[] {
                        SurveyInstanceColumns.SURVEY_ID, SurveyInstanceColumns.SUBMITTED_DATE,
                        SurveyInstanceColumns.UUID, SurveyInstanceColumns.START_DATE,
                        SurveyInstanceColumns.RECORD_ID, SurveyInstanceColumns.DURATION,
                        ResponseColumns.ANSWER, ResponseColumns.TYPE, ResponseColumns.QUESTION_ID,
                        ResponseColumns.STRENGTH, ResponseColumns.SCORED_VAL, UserColumns.NAME, UserColumns.EMAIL
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
            Log.e(TAG, "Could not update status for Survey Instance: " + surveyInstanceId);
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
     * 
     * @param id
     * @param name
     * @param email
     * @return
     */
    public long createOrUpdateUser(Long id, String name, String email) {
        ContentValues initialValues = new ContentValues();
        Long idVal = id;
        initialValues.put(UserColumns.NAME, name);
        initialValues.put(UserColumns.EMAIL, email);
        initialValues.put(UserColumns.DELETED, 0);

        if (idVal == null) {
            idVal = database.insert(Tables.USER, null, initialValues);
        } else {
            database.update(Tables.USER, initialValues, UserColumns._ID + "=?",
                    new String[]{idVal.toString()});
        }
        return idVal;
    }

    public Map<String, QuestionResponse> getResponses(long surveyInstanceId) {
        Map<String, QuestionResponse> responses = new HashMap<String, QuestionResponse>();

        Cursor cursor = database.query(Tables.RESPONSE,
                new String[] {
                        ResponseColumns._ID, ResponseColumns.QUESTION_ID, ResponseColumns.ANSWER,
                        ResponseColumns.TYPE, ResponseColumns.SURVEY_INSTANCE_ID,
                        ResponseColumns.INCLUDE, ResponseColumns.SCORED_VAL, ResponseColumns.STRENGTH
                },
                ResponseColumns.SURVEY_INSTANCE_ID + " = ?",
                new String[] { String.valueOf(surveyInstanceId) },
                null, null, null);

        if (cursor != null) {
            int idCol = cursor.getColumnIndexOrThrow(ResponseColumns._ID);
            int answerCol = cursor.getColumnIndexOrThrow(ResponseColumns.ANSWER);
            int typeCol = cursor.getColumnIndexOrThrow(ResponseColumns.TYPE);
            int qidCol = cursor.getColumnIndexOrThrow(ResponseColumns.QUESTION_ID);
            int includeCol = cursor.getColumnIndexOrThrow(ResponseColumns.INCLUDE);
            int scoreCol = cursor.getColumnIndexOrThrow(ResponseColumns.SCORED_VAL);
            int strengthCol = cursor.getColumnIndexOrThrow(ResponseColumns.STRENGTH);

            if (cursor.moveToFirst()) {
                do {
                    QuestionResponse response = new QuestionResponse();
                    response.setId(cursor.getLong(idCol));
                    response.setRespondentId(surveyInstanceId);// No need to read the cursor
                    response.setValue(cursor.getString(answerCol));
                    response.setType(cursor.getString(typeCol));
                    response.setQuestionId(cursor.getString(qidCol));
                    response.setIncludeFlag(cursor.getInt(includeCol) == 1);
                    response.setScoredValue(cursor.getString(scoreCol));
                    response.setStrength(cursor.getString(strengthCol));

                    responses.put(response.getQuestionId(), response);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return responses;
    }

    /**
     * loads a single question response
     * 
     * @param surveyInstanceId
     * @param questionId
     * @return
     */
    public QuestionResponse getResponse(Long surveyInstanceId, String questionId) {
        QuestionResponse resp = null;
        Cursor cursor = database.query(Tables.RESPONSE,
                new String[] {
                    ResponseColumns._ID, ResponseColumns.QUESTION_ID, ResponseColumns.ANSWER,
                    ResponseColumns.TYPE, ResponseColumns.SURVEY_INSTANCE_ID,
                    ResponseColumns.INCLUDE, ResponseColumns.SCORED_VAL, ResponseColumns.STRENGTH
                },
                ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND " + ResponseColumns.QUESTION_ID
                    + " =?",
                new String[] { String.valueOf(surveyInstanceId), questionId },
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            resp = new QuestionResponse();
            resp.setQuestionId(questionId);
            resp.setRespondentId(surveyInstanceId);
            resp.setType(cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.TYPE)));
            resp.setValue(cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.ANSWER)));
            resp.setId(cursor.getLong(cursor.getColumnIndexOrThrow(ResponseColumns._ID)));
            resp.setScoredValue(cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.SCORED_VAL)));
            resp.setStrength(cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.STRENGTH)));

            boolean include = cursor.getInt(cursor.getColumnIndexOrThrow(ResponseColumns.INCLUDE)) == 1;
            resp.setIncludeFlag(include);
        }

        if (cursor != null) {
            cursor.close();
        }

        return resp;
    }

    /**
     * inserts or updates a question response after first looking to see if it
     * already exists in the database.
     * 
     * @param resp
     * @return
     */
    public QuestionResponse createOrUpdateSurveyResponse(QuestionResponse resp) {
        QuestionResponse responseToSave = getResponse(
                resp.getRespondentId(), resp.getQuestionId());
        if (responseToSave != null) {
            responseToSave.setValue(resp.getValue());
            responseToSave.setStrength(resp.getStrength());
            responseToSave.setScoredValue(resp.getScoredValue());
            if (resp.getType() != null) {
                responseToSave.setType(resp.getType());
            }
        } else {
            responseToSave = resp;
        }
        long id = -1;
        ContentValues initialValues = new ContentValues();
        initialValues.put(ResponseColumns.ANSWER, responseToSave.getValue());
        initialValues.put(ResponseColumns.TYPE, responseToSave.getType());
        initialValues.put(ResponseColumns.QUESTION_ID, responseToSave.getQuestionId());
        initialValues.put(ResponseColumns.SURVEY_INSTANCE_ID, responseToSave.getRespondentId());
        initialValues.put(ResponseColumns.SCORED_VAL, responseToSave.getScoredValue());
        initialValues.put(ResponseColumns.INCLUDE, resp.getIncludeFlag() ? 1: 0);
        initialValues.put(ResponseColumns.STRENGTH, responseToSave.getStrength());
        if (responseToSave.getId() == null) {
            id = database.insert(Tables.RESPONSE, null, initialValues);
        } else {
            if (database.update(Tables.RESPONSE, initialValues, ResponseColumns._ID
                    + "=?", new String[] {
                    responseToSave.getId().toString()
                    }) > 0) {
                id = responseToSave.getId();
            }
        }
        responseToSave.setId(id);
        resp.setId(id);
        return responseToSave;
    }

    /**
     * creates a new unsubmitted survey instance
     */
    public long createSurveyRespondent(String surveyId, User user, String surveyedLocaleId, String appId) {
        final long time = System.currentTimeMillis();

        ContentValues initialValues = new ContentValues();
        initialValues.put(SurveyInstanceColumns.SURVEY_ID, surveyId);
        initialValues.put(SurveyInstanceColumns.USER_ID, user.getId());
        initialValues.put(SurveyInstanceColumns.STATUS, SurveyInstanceStatus.SAVED);
        initialValues.put(SurveyInstanceColumns.UUID, PlatformUtil.uuid());
        initialValues.put(SurveyInstanceColumns.START_DATE, time);
        initialValues.put(SurveyInstanceColumns.SAVED_DATE, time);// Default to START_TIME
        initialValues.put(SurveyInstanceColumns.RECORD_ID, surveyedLocaleId);
        initialValues.put(SurveyInstanceColumns.APP_ID, appId);
        // Make submitter field available before submission
        initialValues.put(SurveyInstanceColumns.SUBMITTER, user.getName());
        return database.insert(Tables.SURVEY_INSTANCE, null, initialValues);
    }

    /**
     * Returns a list of survey objects that are out of date (missing from the
     * db or with a lower version number). If a survey is present but marked as
     * deleted, it will not be listed as out of date (and thus won't be updated)
     */
    public List<Survey> checkSurveyVersions(List<Survey> surveys, String appId) {
        List<Survey> outOfDateSurveys = new ArrayList<Survey>();
        for (int i = 0; i < surveys.size(); i++) {
            Cursor cursor = database.query(Tables.SURVEY,
                    new String[] {
                        SurveyColumns.SURVEY_ID
                    },
                    SurveyColumns.SURVEY_ID + "=? AND " + SurveyColumns.APP_ID + "=? AND (" +
                            SurveyColumns.VERSION + " >= ? or " + SurveyColumns.DELETED + " = ?)",
                    new String[] {
                            surveys.get(i).getId(),
                            appId,
                            surveys.get(i).getVersion() + "",
                            String.valueOf(1)//ConstantUtil.IS_DELETED
                    }, null, null, null);

            if (cursor == null || cursor.getCount() <= 0) {
                outOfDateSurveys.add(surveys.get(i));
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        return outOfDateSurveys;
    }

    /**
     * updates the survey table by recording the help download flag
     */
    public void markSurveyHelpDownloaded(String surveyId, String appId, boolean isDownloaded) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyColumns.HELP_DOWNLOADED, isDownloaded ? 1 : 0);

        if (database.update(Tables.SURVEY, updatedValues,
                SurveyColumns.SURVEY_ID + "=? AND " + SurveyColumns.APP_ID + "=?",
                new String[] {
                    surveyId, appId
                }) < 1) {
            Log.e(TAG, "Could not update record for Survey " + surveyId);
        }
    }

    /**
     * updates a survey in the db and resets the deleted flag to "N"
     * 
     * @param survey
     * @return
     */
    public void saveSurvey(Survey survey) {
        Cursor cursor = database.query(Tables.SURVEY,
                new String[] {
                    SurveyColumns._ID
                }, SurveyColumns.SURVEY_ID + " = ?",
                new String[] {
                    survey.getId(),
                }, null, null, null);
        final long surveyGroupId = survey.getSurveyGroup() != null ? 
                survey.getSurveyGroup().getId() 
                : SurveyGroup.ID_NONE;
        
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyColumns.SURVEY_ID, survey.getId());
        updatedValues.put(SurveyColumns.VERSION, survey.getVersion());
        updatedValues.put(SurveyColumns.TYPE, survey.getType());
        updatedValues.put(SurveyColumns.LOCATION, survey.getLocation());
        updatedValues.put(SurveyColumns.FILENAME, survey.getFileName());
        updatedValues.put(SurveyColumns.NAME, survey.getName());
        updatedValues.put(SurveyColumns.LANGUAGE, survey.getLanguage() != null ? survey
                .getLanguage().toLowerCase() : ConstantUtil.ENGLISH_CODE);
        updatedValues.put(SurveyColumns.SURVEY_GROUP_ID, surveyGroupId);
        updatedValues.put(SurveyColumns.HELP_DOWNLOADED, survey.isHelpDownloaded() ? 1 : 0);
        updatedValues.put(SurveyColumns.APP_ID, survey.getAppId());

        if (cursor != null && cursor.getCount() > 0) {
            // if we found an item, it's an update, otherwise, it's an insert
            database.update(Tables.SURVEY, updatedValues, SurveyColumns.SURVEY_ID + " = ?",
                    new String[] {
                        survey.getId()
                    });
        } else {
            database.insert(Tables.SURVEY, null, updatedValues);
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * Gets a single survey from the db using its survey id
     */
    public Survey getSurvey(String surveyId, String appId) {
        Survey survey = null;
        Cursor cursor = database.query(Tables.SURVEY, new String[] {
                SurveyColumns.SURVEY_ID, SurveyColumns.NAME, SurveyColumns.LOCATION,
                SurveyColumns.FILENAME, SurveyColumns.TYPE, SurveyColumns.LANGUAGE,
                SurveyColumns.HELP_DOWNLOADED, SurveyColumns.VERSION, SurveyColumns.APP_ID
        }, SurveyColumns.SURVEY_ID + "=? AND " + SurveyColumns.APP_ID + "=?",
                new String[] {
                    surveyId, appId
                }, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                survey = getSurvey(cursor);
            }
            cursor.close();
        }

        return survey;
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
    public void deleteResponses(long surveyInstanceId) {
        database.delete(Tables.RESPONSE, ResponseColumns.SURVEY_INSTANCE_ID + "=?",
                new String[] {
                    String.valueOf(surveyInstanceId)
                });
    }

    /**
     * deletes the surveyInstanceId record and any responses it contains
     * 
     * @param surveyInstanceId
     */
    public void deleteSurveyInstance(long surveyInstanceId) {
        deleteResponses(surveyInstanceId);
        database.delete(Tables.SURVEY_INSTANCE, SurveyInstanceColumns._ID + "=?",
                new String[]{
                        String.valueOf(surveyInstanceId)
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

    public void createTransmission(long surveyInstanceId, String filename) {
        createTransmission(surveyInstanceId, filename, TransmissionStatus.QUEUED);
    }


    public void createTransmission(long surveyInstanceId, String filename, int status) {
        ContentValues values = new ContentValues();
        values.put(TransmissionColumns.SURVEY_INSTANCE_ID, surveyInstanceId);
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
    public int updateTransmissionHistory(String fileName, String appId, int status) {
        // TODO: Update Survey Instance STATUS as well
        ContentValues vals = new ContentValues();
        vals.put(TransmissionColumns.STATUS, status);
        if (TransmissionStatus.SYNCED == status) {
            vals.put(TransmissionColumns.END_DATE, System.currentTimeMillis() + "");
        } else if (TransmissionStatus.IN_PROGRESS == status) {
            vals.put(TransmissionColumns.START_DATE, System.currentTimeMillis() + "");
        }

        return database.update(Tables.TRANSMISSION,
                vals,
                TransmissionColumns.FILENAME + "=? AND " + TransmissionColumns.SURVEY_INSTANCE_ID
                    + " IN (SELECT _id FROM survey_instance WHERE app_id=?)",
                new String[] {fileName, appId});
    }

    public List<FileTransmission> getFileTransmissions(Cursor cursor) {
        List<FileTransmission> transmissions = new ArrayList<FileTransmission>();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                final int startCol = cursor.getColumnIndexOrThrow(TransmissionColumns.START_DATE);
                final int endCol = cursor.getColumnIndexOrThrow(TransmissionColumns.END_DATE);
                final int idCol = cursor.getColumnIndexOrThrow(TransmissionColumns._ID);
                final int surveyInstanceCol = cursor.getColumnIndexOrThrow(TransmissionColumns.SURVEY_INSTANCE_ID);
                final int fileCol = cursor.getColumnIndexOrThrow(TransmissionColumns.FILENAME);
                final int statusCol = cursor.getColumnIndexOrThrow(TransmissionColumns.STATUS);

                transmissions = new ArrayList<FileTransmission>();
                do {
                    FileTransmission trans = new FileTransmission();
                    trans.setId(cursor.getLong(idCol));
                    trans.setRespondentId(cursor.getLong(surveyInstanceCol));
                    trans.setFileName(cursor.getString(fileCol));
                    trans.setStatus(cursor.getInt(statusCol));

                    // Start and End date. Handle null cases
                    if (!cursor.isNull(startCol)) {
                        trans.setStartDate(new Date(cursor.getLong(startCol)));
                    }
                    if (!cursor.isNull(endCol)) {
                        trans.setEndDate(new Date(cursor.getLong(endCol)));
                    }

                    transmissions.add(trans);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return transmissions;
    }

    public List<FileTransmission> getFileTransmissions(long surveyInstanceId) {
        Cursor cursor = database.query(Tables.TRANSMISSION,
                new String[] {
                        TransmissionColumns._ID, TransmissionColumns.SURVEY_INSTANCE_ID,
                        TransmissionColumns.STATUS, TransmissionColumns.FILENAME,
                        TransmissionColumns.START_DATE, TransmissionColumns.END_DATE
                },
                TransmissionColumns.SURVEY_INSTANCE_ID + " = ?",
                new String[] { String.valueOf(surveyInstanceId) },
                null, null, null);

        return getFileTransmissions(cursor);
    }

    /**
     * Get the list of queued and failed transmissions
     */
    public List<FileTransmission> getUnsyncedTransmissions(String appId) {
        Cursor cursor = database.query(Tables.TRANSMISSION_JOIN_SURVEY_INSTANCE,
                new String[] {
                        Tables.TRANSMISSION + "." + TransmissionColumns._ID,
                        Tables.TRANSMISSION + "." + TransmissionColumns.SURVEY_INSTANCE_ID,
                        Tables.TRANSMISSION + "." + TransmissionColumns.STATUS,
                        Tables.TRANSMISSION + "." + TransmissionColumns.START_DATE,
                        Tables.TRANSMISSION + "." + TransmissionColumns.END_DATE,
                        Tables.TRANSMISSION + "." + TransmissionColumns.FILENAME
                },


                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.APP_ID + "=? AND "
                    + Tables.TRANSMISSION + "." + TransmissionColumns.STATUS + " IN (?, ?, ?)",
                new String[] {
                        appId,
                        String.valueOf(TransmissionStatus.FAILED),
                        String.valueOf(TransmissionStatus.IN_PROGRESS),// Stalled IN_PROGRESS files
                        String.valueOf(TransmissionStatus.QUEUED)
                }, null, null, null);

        return getFileTransmissions(cursor);
    }

    /**
     * executes a single insert/update/delete DML or any DDL statement without
     * any bind arguments.
     * 
     * @param sql
     */
    public void executeSql(String sql) {
        database.execSQL(sql);
    }

    /**
     * reinserts the test survey into the database. For debugging purposes only.
     * The survey xml must exist in the APK
     */
    public void reinstallTestSurvey() {
        executeSql("insert into survey values(999991,'Sample Survey', 1.0,'Survey','res','testsurvey','english','N','N')");
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
        Prefs.setLong(context, Prefs.KEY_USER_ID, -1);
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
                new String[]{
                        id.toString()
                });
    }

    public HashSet<String> stringToSet(String item) {
        HashSet<String> set = new HashSet<String>();
        StringTokenizer strTok = new StringTokenizer(item, ",");
        while (strTok.hasMoreTokens()) {
            set.add(strTok.nextToken());
        }
        return set;
    }

    public String setToString(HashSet<String> set) {
        boolean isFirst = true;
        StringBuilder buffer = new StringBuilder();
        Iterator<String> itr = set.iterator();

        for (int i = 0; i < set.size(); i++) {
            if (!isFirst) {
                buffer.append(",");
            } else {
                isFirst = false;
            }
            buffer.append(itr.next());
        }
        return buffer.toString();
    }

    public void addLanguages(String[] values) {
        // values holds the 2-letter codes of the languages. We first have to
        // find out what the indexes are

        String[] langCodesArray = context.getResources().getStringArray(
                R.array.alllanguagecodes);
        int[] valuesIndex = new int[values.length];
        List<String> langCodesList = Arrays.asList(langCodesArray);
        int index;
        for (int i = 0; i < values.length; i++) {
            index = langCodesList.indexOf(values[i]);
            if (index != -1) {
                valuesIndex[i] = index;
            }
        }

        String langsSelection = Prefs.getString(context, Prefs.KEY_LANGUAGE, "");
        String langsPresentIndexes = Prefs.getString(context, Prefs.KEY_LANGUAGES_PRESENT, "");

        HashSet<String> langsSelectionSet = stringToSet(langsSelection);
        HashSet<String> langsPresentIndexesSet = stringToSet(langsPresentIndexes);

        for (int i = 0; i < values.length; i++) {
            // values[0] holds the default language. That is the one that will
            // be turned 'on'.
            if (i == 0) {
                langsSelectionSet.add(valuesIndex[i] + "");
            }
            langsPresentIndexesSet.add(valuesIndex[i] + "");
        }

        String newLangsSelection = setToString(langsSelectionSet);
        String newLangsPresentIndexes = setToString(langsPresentIndexesSet);

        Prefs.setString(context, Prefs.KEY_LANGUAGE, newLangsSelection);
        Prefs.setString(context, Prefs.KEY_LANGUAGES_PRESENT, newLangsPresentIndexes);
    }
    
    public void addSurveyGroup(SurveyGroup surveyGroup) {
        ContentValues values = new ContentValues();
        values.put(SurveyGroupColumns.SURVEY_GROUP_ID, surveyGroup.getId());
        values.put(SurveyGroupColumns.NAME, surveyGroup.getName());
        values.put(SurveyGroupColumns.REGISTER_SURVEY_ID, surveyGroup.getRegisterSurveyId());
        values.put(SurveyGroupColumns.MONITORED, surveyGroup.isMonitored() ? 1 : 0);
        values.put(SurveyGroupColumns.APP_ID, surveyGroup.getAppId());
        database.insert(Tables.SURVEY_GROUP, null, values);
    }

    public void addInstance(Instance instance) {
        ContentValues values = new ContentValues();
        values.put(InstanceColumns.APP_ID, instance.getAppId());
        values.put(InstanceColumns.ALIAS, instance.getAlias());
        values.put(InstanceColumns.SERVER_BASE, instance.getServerBase());
        values.put(InstanceColumns.AWS_BUCKET, instance.getAWSBucket());
        values.put(InstanceColumns.AWS_ACCESS_KEY_ID, instance.getAWSAccessKeyId());
        values.put(InstanceColumns.AWS_SECRET_KEY, instance.getAWSSecretKey());
        values.put(InstanceColumns.API_KEY, instance.getApiKey());

        database.insert(Tables.INSTANCE, null, values);
    }

    public static Instance getInstance(Cursor cursor) {
        return new Instance(
                cursor.getString(cursor.getColumnIndexOrThrow(InstanceColumns.APP_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(InstanceColumns.ALIAS)),
                cursor.getString(cursor.getColumnIndexOrThrow(InstanceColumns.SERVER_BASE)),
                cursor.getString(cursor.getColumnIndexOrThrow(InstanceColumns.AWS_BUCKET)),
                cursor.getString(cursor.getColumnIndexOrThrow(InstanceColumns.AWS_ACCESS_KEY_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(InstanceColumns.AWS_SECRET_KEY)),
                cursor.getString(cursor.getColumnIndexOrThrow(InstanceColumns.API_KEY))
        );
    }

    public Cursor getInstance(String name) {
        String where = null;
        String[] selectionArgs = null;
        if (!TextUtils.isEmpty(name)) {
            where = InstanceColumns.APP_ID + "= ?";
            selectionArgs = new String[] {name};
        }

        return database.query(Tables.INSTANCE, null, where, selectionArgs, null, null, InstanceColumns.APP_ID);
    }

    public Cursor getInstances() {
        return getInstance("");
    }

    public static SurveyGroup getSurveyGroup(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyGroupColumns.SURVEY_GROUP_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(SurveyGroupColumns.NAME));
        String registerSurveyId = cursor.getString(cursor.getColumnIndexOrThrow(SurveyGroupColumns.REGISTER_SURVEY_ID));
        String appId = cursor.getString(cursor.getColumnIndexOrThrow(SurveyGroupColumns.APP_ID));
        boolean monitored = cursor.getInt(cursor.getColumnIndexOrThrow(SurveyGroupColumns.MONITORED)) > 0;
        return new SurveyGroup(id, name, registerSurveyId, monitored, appId);
    }
    
    public Cursor getSurveyGroup(String appId) {
        return database.query(Tables.SURVEY_GROUP, null,
                SurveyGroupColumns.APP_ID + "= ?",
                new String[] {appId},
                null, null, SurveyGroupColumns.NAME);
    }
    
    public String createSurveyedLocale(long surveyGroupId, String appId) {
        String id = PlatformUtil.recordUuid();
        ContentValues values = new ContentValues();
        values.put(RecordColumns.RECORD_ID, id);
        values.put(RecordColumns.SURVEY_GROUP_ID, surveyGroupId);
        values.put(RecordColumns.APP_ID, appId);
        database.insert(Tables.RECORD, null, values);
        
        return id;
    }
    
    public static SurveyedLocale getSurveyedLocale(Cursor cursor) {
        String id = cursor.getString(RecordQuery.RECORD_ID);
        long surveyGroupId = cursor.getLong(RecordQuery.SURVEY_GROUP_ID);
        long lastModified = cursor.getLong(RecordQuery.LAST_MODIFIED);
        String name = cursor.getString(RecordQuery.NAME);

        // Location. Check for null values first
        Double latitude = null;
        Double longitude = null;
        if (!cursor.isNull(RecordQuery.LATITUDE) && !cursor.isNull(RecordQuery.LONGITUDE)) {
            latitude = cursor.getDouble(RecordQuery.LATITUDE);
            longitude = cursor.getDouble(RecordQuery.LONGITUDE);
        }
        return new SurveyedLocale(id, name, lastModified, surveyGroupId, latitude, longitude);
    }

    public Cursor getSurveyedLocales(long surveyGroupId, String appId) {
        return database.query(Tables.RECORD, RecordQuery.PROJECTION,
                RecordColumns.SURVEY_GROUP_ID + "=? AND " + RecordColumns.APP_ID + "=?",
                new String[] {String.valueOf(surveyGroupId), appId},
                null, null, null);
    }
    
    public SurveyedLocale getSurveyedLocale(String surveyedLocaleId) {
        Cursor cursor = database.query(Tables.RECORD, RecordQuery.PROJECTION,
                RecordColumns.RECORD_ID + " = ?",
                new String[] {String.valueOf(surveyedLocaleId)},
                null, null, null);
        
        SurveyedLocale locale = null;
        if (cursor.moveToFirst()) {
            locale = getSurveyedLocale(cursor);
        }
        cursor.close();
        
        return locale;
    }
    
    public static Survey getSurvey(Cursor cursor) {
        Survey survey = new Survey();
        survey.setId(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.SURVEY_ID)));
        survey.setName(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.NAME)));
        survey.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LOCATION)));
        survey.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.FILENAME)));
        survey.setType(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.TYPE)));
        survey.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LANGUAGE)));
        survey.setVersion(cursor.getDouble(cursor.getColumnIndexOrThrow(SurveyColumns.VERSION)));
        survey.setAppId(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.APP_ID)));

        int helpDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(SurveyColumns.HELP_DOWNLOADED));
        survey.setHelpDownloaded(helpDownloaded == 1);
        return survey;
    }

    public String[] getSurveyIds(String appId) {
        Cursor c = getSurveys(-1, appId);// All survey groups
        if (c != null) {
            String[] ids = new String[c.getCount()];
            if (c.moveToFirst()) {
                do {
                    ids[c.getPosition()] = c.getString(c.getColumnIndexOrThrow(SurveyColumns.SURVEY_ID));
                } while (c.moveToNext());
            }
            c.close();
            return ids;
        }
        return null;
    }

    public Cursor getSurveys(long surveyGroupId, String appId) {
        String whereClause = SurveyColumns.APP_ID + "=? AND " + SurveyColumns.DELETED + " <> 1";
        String[] whereParams = new String[]{appId};
        if (surveyGroupId > 0) {
            whereClause += " AND " + SurveyColumns.SURVEY_GROUP_ID + " = ?";
            whereParams = new String[]{appId, String.valueOf(surveyGroupId)};
        }
        
        return database.query(Tables.SURVEY, null, whereClause, whereParams, null, null, null);
    }

    /**
     * Convenience method to retrieve all non-deleted surveys, without the hassle of
     * parsing the Cursor columns.
     * To get the Cursor result, use getSurveys(surveyGroupId)
     */
    public List<Survey> getSurveyList (long surveyGroupId, String appId) {
        // Reuse getSurveys() method
        Cursor cursor = getSurveys(surveyGroupId, appId);

        ArrayList<Survey> surveys = new ArrayList<Survey>();

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    surveys.add(getSurvey(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return surveys;
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
    public void deleteSurvey(String surveyId, boolean physicalDelete) {
        if (!physicalDelete) {
            ContentValues updatedValues = new ContentValues();
            updatedValues.put(SurveyColumns.DELETED, 1);
            database.update(Tables.SURVEY, updatedValues, SurveyColumns.SURVEY_ID + " = ?",
                    new String[] { surveyId });
        } else {
            database.delete(Tables.SURVEY, SurveyColumns.SURVEY_ID + " = ? ",
                    new String[] { surveyId });
        }
    }

    /**
     * Get all the SurveyInstances for a particular Record
     */
    public Cursor getSurveyInstances(String recordId, String appId) {
        return database.query(Tables.SURVEY_INSTANCE_JOIN_SURVEY,
                new String[] {
                        Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns._ID,
                        Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SURVEY_ID,
                        SurveyColumns.NAME, SurveyInstanceColumns.SAVED_DATE,
                        SurveyInstanceColumns.USER_ID, SurveyInstanceColumns.SUBMITTED_DATE,
                        SurveyInstanceColumns.UUID, SurveyInstanceColumns.STATUS,
                        SurveyInstanceColumns.SYNC_DATE, SurveyInstanceColumns.EXPORTED_DATE,
                        SurveyInstanceColumns.RECORD_ID, SurveyInstanceColumns.SUBMITTER
                },
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.RECORD_ID + "=? AND "
                        + Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.APP_ID + "=?",
                new String[] { recordId, appId },
                null, null, SurveyInstanceColumns.START_DATE + " DESC");
    }

    /**
     * Get SurveyInstances with a particular status.
     * If the recordId is not null, results will be filtered by Record.
     */
    public long[] getSurveyInstances(String recordId, String surveyId, String appId, int status) {
        String where = Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SURVEY_ID + "= ?" +
                " AND " + SurveyInstanceColumns.STATUS + "= ?" +
                " AND " + SurveyInstanceColumns.APP_ID + "= ?" +
                " AND "  + SurveyInstanceColumns.RECORD_ID + "= ?";
        List<String> args = new ArrayList<String>();
        args.add(surveyId);
        args.add(String.valueOf(status));
        args.add(appId);
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
     * @param surveyedLocaleId
     * @param surveyId
     * @return last surveyInstance with those attributes
     */
    public Long getLastSurveyInstance(String surveyedLocaleId, String surveyId, String appId) {
        Cursor cursor = database.query(Tables.SURVEY_INSTANCE,
                new String[] {
                    SurveyInstanceColumns._ID, SurveyInstanceColumns.RECORD_ID,
                    SurveyInstanceColumns.SURVEY_ID, SurveyInstanceColumns.SUBMITTED_DATE
                },
                SurveyInstanceColumns.RECORD_ID + "=? AND "
                        + SurveyInstanceColumns.SURVEY_ID + "= ? AND "
                        + SurveyInstanceColumns.APP_ID + "=? AND "
                        + SurveyInstanceColumns.SUBMITTED_DATE + " IS NOT NULL",
                new String[]{surveyedLocaleId, surveyId, appId},
                null, null,
                SurveyInstanceColumns.SUBMITTED_DATE + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getLong(cursor.getColumnIndexOrThrow(SurveyInstanceColumns._ID));
        }
        
        return null;
    }
    
    public String getSurveyedLocaleId(long surveyInstanceId) {
        Cursor cursor = database.query(Tables.SURVEY_INSTANCE,
                new String[] {
                    SurveyInstanceColumns._ID, SurveyInstanceColumns.RECORD_ID
                },
                SurveyInstanceColumns._ID + "= ?",
                new String[]{String.valueOf(surveyInstanceId)},
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
    public enum SurveyedLocaleMeta {NAME, GEOLOCATION}
    
    public void updateSurveyedLocale(long surveyInstanceId, String response, SurveyedLocaleMeta type) {
        if (!TextUtils.isEmpty(response)) {
            String surveyedLocaleId = getSurveyedLocaleId(surveyInstanceId);
            ContentValues surveyedLocaleValues = new ContentValues();

            QuestionResponse metaResponse = new QuestionResponse();
            metaResponse.setRespondentId(surveyInstanceId);
            metaResponse.setValue(response);
            metaResponse.setIncludeFlag(true);
            
            switch (type) {
                case NAME:
                    surveyedLocaleValues.put(RecordColumns.NAME, response);
                    metaResponse.setType("META_NAME");
                    metaResponse.setQuestionId(ConstantUtil.QUESTION_LOCALE_NAME);
                    break;
                case GEOLOCATION:
                    String[] parts = response.split("\\|");
                    if (parts.length < 2) {
                        return;// Wrong format
                    }
                    surveyedLocaleValues.put(RecordColumns.LATITUDE, Double.parseDouble(parts[0]));
                    surveyedLocaleValues.put(RecordColumns.LONGITUDE, Double.parseDouble(parts[1]));
                    metaResponse.setType("META_GEO");
                    metaResponse.setQuestionId(ConstantUtil.QUESTION_LOCALE_GEO);
                    break;
            }
            
            // Update the surveyed locale info
            database.update(Tables.RECORD, surveyedLocaleValues,
                    RecordColumns.RECORD_ID + " = ?",
                    new String[] {surveyedLocaleId});
            
            // Store the META_NAME/META_GEO as a response
            createOrUpdateSurveyResponse(metaResponse);
        }
    }

    /**
     * Update the last modification date, if necessary
     */
    public void updateRecordModifiedDate(String recordId, String appId, long timestamp) {
        ContentValues values = new ContentValues();
        values.put(RecordColumns.LAST_MODIFIED, timestamp);
        database.update(Tables.RECORD, values,
                RecordColumns.RECORD_ID + "=? AND " + RecordColumns.APP_ID + "=? AND "
                        + RecordColumns.LAST_MODIFIED + "<?",
                new String[]{recordId, appId, String.valueOf(timestamp)});
    }
    
    /**
    * Filters surveyd locales based on the parameters passed in.
    */
    public Cursor getFilteredSurveyedLocales(long surveyGroupId, String appId, Double latitude, Double longitude, int orderBy) {
        // Note: This PROJECTION column indexes have to match the default RecordQuery PROJECTION ones,
        // as this one will only APPEND new columns to the resultset, making the generic getSurveyedLocale(Cursor)
        // fully compatible. TODO: This should be refactored and replaced with a less complex approach.
        String queryString = "SELECT sl.*,"
                + " MIN(r." + SurveyInstanceColumns.STATUS + ") as " + SurveyInstanceColumns.STATUS
                + " FROM "
                + Tables.RECORD + " AS sl LEFT JOIN " + Tables.SURVEY_INSTANCE + " AS r ON "
                + "sl." + RecordColumns.RECORD_ID + "=" + "r." + SurveyInstanceColumns.RECORD_ID;
        String whereClause = " WHERE sl." + RecordColumns.SURVEY_GROUP_ID + " =? AND sl." + RecordColumns.APP_ID + "=?";
        String groupBy = " GROUP BY sl." + RecordColumns.RECORD_ID;

        String orderByStr = "";
        switch (orderBy) {
            case ConstantUtil.ORDER_BY_DATE:
                orderByStr = " ORDER BY " + RecordColumns.LAST_MODIFIED + " DESC";// By date
                break;
            case ConstantUtil.ORDER_BY_DISTANCE:
                if (latitude != null && longitude != null){
                    // this is to correct the distance for the shortening at higher latitudes
                    Double fudge = Math.pow(Math.cos(Math.toRadians(latitude)),2);

                    // this uses a simple planar approximation of distance. this should be good enough for our purpose.
                    String orderByTempl = " ORDER BY CASE WHEN " + RecordColumns.LATITUDE + " IS NULL THEN 1 ELSE 0 END,"
                            + " ((%s - " + RecordColumns.LATITUDE + ") * (%s - " + RecordColumns.LATITUDE
                            + ") + (%s - " + RecordColumns.LONGITUDE + ") * (%s - " + RecordColumns.LONGITUDE + ") * %s)";
                    orderByStr = String.format(orderByTempl, latitude, latitude, longitude, longitude, fudge);
                }
                break;
            case ConstantUtil.ORDER_BY_STATUS:
                orderByStr = " ORDER BY " + " MIN(r." + SurveyInstanceColumns.STATUS + ")";
                break;
            case ConstantUtil.ORDER_BY_NAME:
                orderByStr = " ORDER BY " + RecordColumns.NAME + " ASC";// By name
                break;
        }

        String[] whereValues = new String[] {String.valueOf(surveyGroupId), appId};
        return database.rawQuery(queryString + whereClause + groupBy + orderByStr, whereValues);
    }

    // ======================================================= //
    // =========== SurveyedLocales synchronization =========== //
    // ======================================================= //
    
    public void syncResponses(List<QuestionResponse> responses, long surveyInstanceId) {
        for (QuestionResponse response : responses) {
            Cursor cursor = database.query(Tables.RESPONSE,
                    new String[] { ResponseColumns.SURVEY_INSTANCE_ID, ResponseColumns.QUESTION_ID },
                    ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND "
                        + ResponseColumns.QUESTION_ID + " = ?",
                    new String[] { String.valueOf(surveyInstanceId), response.getQuestionId()},
                    null, null, null);
                
            boolean exists = cursor.getCount() > 0;
            cursor.close();
                
            ContentValues values = new ContentValues();
            values.put(ResponseColumns.ANSWER, response.getValue());
            values.put(ResponseColumns.TYPE, response.getType());
            values.put(ResponseColumns.QUESTION_ID, response.getQuestionId());
            values.put(ResponseColumns.INCLUDE, response.getIncludeFlag());
            values.put(ResponseColumns.SURVEY_INSTANCE_ID, surveyInstanceId);
                
            if (exists) {
                database.update(Tables.RESPONSE, values,
                        ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND "
                                + ResponseColumns.QUESTION_ID + " = ?",
                        new String[] { String.valueOf(surveyInstanceId), response.getQuestionId()});
            } else {
                database.insert(Tables.RESPONSE, null, values);
            }
        }
    }
    
    public void syncSurveyInstances(List<SurveyInstance> surveyInstances, String surveyedLocaleId, String appId) {
        for (SurveyInstance surveyInstance : surveyInstances) {
            Cursor cursor = database.query(Tables.SURVEY_INSTANCE, new String[] {
                    SurveyInstanceColumns._ID, SurveyInstanceColumns.UUID},
                    SurveyInstanceColumns.UUID + "=? AND " + SurveyInstanceColumns.APP_ID + "=?",
                    new String[] { surveyInstance.getUuid(), appId},
                    null, null, null);
                
            long id = -1;
            if (cursor.moveToFirst()) {
                id = cursor.getLong(0);
            }
            cursor.close();
                
            ContentValues values = new ContentValues();
            values.put(SurveyInstanceColumns.SURVEY_ID, surveyInstance.getSurveyId());
            values.put(SurveyInstanceColumns.SUBMITTED_DATE, surveyInstance.getDate());
            values.put(SurveyInstanceColumns.RECORD_ID, surveyedLocaleId);
            values.put(SurveyInstanceColumns.STATUS, SurveyInstanceStatus.DOWNLOADED);
            values.put(SurveyInstanceColumns.SYNC_DATE, System.currentTimeMillis());
            values.put(SurveyInstanceColumns.SUBMITTER, surveyInstance.getSubmitter());
            values.put(SurveyInstanceColumns.APP_ID, appId);

            if (id != -1) {
                database.update(Tables.SURVEY_INSTANCE, values, SurveyInstanceColumns.UUID
                        + " = ?", new String[] { surveyInstance.getUuid()});
            } else {
                values.put(SurveyInstanceColumns.UUID, surveyInstance.getUuid());
                id = database.insert(Tables.SURVEY_INSTANCE, null, values);
            }

            // Now the responses...
            syncResponses(surveyInstance.getResponses(), id);

            // The filename is a unique column in the transmission table, and as we do not have
            // a file to hold this data, we set the value to the instance UUID
            createTransmission(id, surveyInstance.getUuid(), TransmissionStatus.SYNCED);
        }
    }
    
    public void syncSurveyedLocale(SurveyedLocale surveyedLocale, String appId) {
        final String id = surveyedLocale.getId();
        try {
            database.beginTransaction();

            ContentValues values = new ContentValues();
            values.put(RecordColumns.RECORD_ID, id);
            values.put(RecordColumns.SURVEY_GROUP_ID, surveyedLocale.getSurveyGroupId());
            values.put(RecordColumns.NAME, surveyedLocale.getName());
            values.put(RecordColumns.LATITUDE, surveyedLocale.getLatitude());
            values.put(RecordColumns.LONGITUDE, surveyedLocale.getLongitude());
            values.put(RecordColumns.APP_ID, appId);
            database.insert(Tables.RECORD, null, values);

            syncSurveyInstances(surveyedLocale.getSurveyInstances(), id, appId);

            // Update the record last modification date, if necessary
            updateRecordModifiedDate(id, appId, surveyedLocale.getLastModified());

            String syncTime = String.valueOf(surveyedLocale.getLastModified());
            setSyncTime(surveyedLocale.getSurveyGroupId(), appId, syncTime);

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }
    
    /**
     * Get the synchronization time for a particular survey group.
     * @param surveyGroupId id of the SurveyGroup
     * @return time if exists for this key, null otherwise
     */
    public String getSyncTime(long surveyGroupId, String appId) {
        Cursor cursor = database.query(Tables.SYNC_TIME,
                new String[]{SyncTimeColumns.SURVEY_GROUP_ID, SyncTimeColumns.TIME},
                SyncTimeColumns.SURVEY_GROUP_ID + "=? AND " + SyncTimeColumns.APP_ID + "=?",
                new String[]{String.valueOf(surveyGroupId), appId},
                null, null, null);
        
        String time = null;
        if (cursor.moveToFirst()) {
            time = cursor.getString(cursor.getColumnIndexOrThrow(SyncTimeColumns.TIME));
        }
        cursor.close();
        return time;
    }
    
    /**
     * Save the time of synchronization time for a particular SurveyGroup
     * @param surveyGroupId id of the SurveyGroup
     * @param time String containing the timestamp
     */
    public void setSyncTime(long surveyGroupId, String appId, String time) {
        ContentValues values = new ContentValues();
        values.put(SyncTimeColumns.SURVEY_GROUP_ID, surveyGroupId);
        values.put(SyncTimeColumns.APP_ID, appId);
        values.put(SyncTimeColumns.TIME, time);
        database.insert(Tables.SYNC_TIME, null, values);
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
                + " WHERE "  + RecordColumns.RECORD_ID + " NOT IN "
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

    /**
     * Upgrade the database data to enable app-specific support. This implies the addition of "app_id"
     * columns to all relevant tables, as well as physical file reference updates.
     * @param appId The instance being set up.
     */
    public void onSingleAppUpgrade(String appId) {
        ContentValues values = new ContentValues();
        values.put(InstanceColumns.APP_ID, appId);
        database.update(Tables.SURVEY, values, null, null);
        database.update(Tables.SURVEY_INSTANCE, values, null, null);
        database.update(Tables.SURVEY_GROUP, values, null, null);
        database.update(Tables.RECORD, values, null, null);
        database.update(Tables.SYNC_TIME, values, null, null);
        // TODO: Add DB "NOT NULL" constraints at this point?

        // Update file paths
        final String replace = "replace(%s,'akvoflow/data','akvoflow/%s/data')";

        String query = "UPDATE " + Tables.RESPONSE + " SET "
                + ResponseColumns.ANSWER + " = " + String.format(replace, ResponseColumns.ANSWER, appId)
                + " WHERE " + ResponseColumns.TYPE + " IN (?,?)";
        database.execSQL(query, new String[]{ConstantUtil.IMAGE_RESPONSE_TYPE, ConstantUtil.VIDEO_RESPONSE_TYPE});

        query = "UPDATE " + Tables.TRANSMISSION + " SET "
                + TransmissionColumns.FILENAME + " = " + String.format(replace, TransmissionColumns.FILENAME, appId);
        database.execSQL(query);
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

}
