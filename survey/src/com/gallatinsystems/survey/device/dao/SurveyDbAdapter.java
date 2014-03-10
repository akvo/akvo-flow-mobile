/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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

package com.gallatinsystems.survey.device.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.domain.FileTransmission;
import com.gallatinsystems.survey.device.domain.QuestionResponse;
import com.gallatinsystems.survey.device.domain.Survey;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.domain.SurveyInstance;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.util.Base32;
import com.gallatinsystems.survey.device.util.ConstantUtil;

/**
 * Database class for the survey db. It can create/upgrade the database as well
 * as select/insert/update survey responses. TODO: break this up into separate
 * DAOs
 * 
 * @author Christopher Fagiani
 */
public class SurveyDbAdapter {
    /*
    public static final String QUESTION_FK_COL = "question_id";
    public static final String ANSWER_COL = "answer_value";
    public static final String ANSWER_TYPE_COL = "answer_type";
    public static final String SURVEY_RESPONDENT_ID_COL = "survey_respondent_id";
    public static final String RESP_ID_COL = "survey_response_id";
    public static final String SURVEY_FK_COL = "survey_id";
    public static final String PK_ID_COL = "_id";
    public static final String USER_FK_COL = "user_id";
    public static final String DISP_NAME_COL = "display_name";
    public static final String EMAIL_COL = "email";
    public static final String SUBMITTED_FLAG_COL = "submitted_flag";
    public static final String SUBMITTED_DATE_COL = "submitted_date";
    public static final String SURVEY_START_COL = "survey_start";
    public static final String DELIVERED_DATE_COL = "delivered_date";
    public static final String CREATED_DATE_COL = "created_date";
    public static final String UPDATED_DATE_COL = "updated_date";
    public static final String PLOT_FK_COL = "plot_id";
    public static final String LAT_COL = "lat";
    public static final String LON_COL = "lon";
    public static final String ELEVATION_COL = "elevation";
    public static final String DESC_COL = "description";
    public static final String STATUS_COL = "status";
    public static final String VERSION_COL = "version";
    public static final String TYPE_COL = "type";
    public static final String LOCATION_COL = "location";
    public static final String FILENAME_COL = "filename";
    public static final String KEY_COL = "key";
    public static final String VALUE_COL = "value";
    public static final String DELETED_COL = "deleted_flag";
    public static final String MEDIA_SENT_COL = "media_sent_flag";
    public static final String HELP_DOWNLOADED_COL = "help_downloaded_flag";
    public static final String LANGUAGE_COL = "language";
    public static final String SURVEY_GROUP_ID_COL = "survey_group_id";
    public static final String SURVEYED_LOCALE_ID_COL = "surveyed_locale_id";
    public static final String SAVED_DATE_COL = "saved_date";
    public static final String INCLUDE_FLAG_COL = "include_flag";
    public static final String SCORED_VAL_COL = "scored_val";
    public static final String STRENGTH_COL = "strength";
    public static final String TRANS_START_COL = "trans_start_date";
    public static final String EXPORTED_FLAG_COL = "exported_flag";
    public static final String UUID_COL = "uuid";

    //private static final String USER_TABLE_CREATE = "create table user (_id integer primary key autoincrement, display_name text not null, email text not null, deleted_flag text);";

    //private static final String SURVEY_TABLE_CREATE = "create table survey (_id integer primary key, display_name text not null, "
    //+ "version real, type text, location text, filename text, language, help_downloaded_flag text, deleted_flag text, survey_group_id integer);";
    //private static final String SURVEY_GROUP_TABLE_CREATE = "create table survey_group (_id integer primary key on conflict replace, name text, register_survey_id text, monitored integer);";
    //private static final String SURVEY_RESPONDENT_CREATE = "create table survey_respondent (_id integer primary key autoincrement, "
            //+ "survey_id integer not null, submitted_flag text, submitted_date text, delivered_date text, user_id integer, media_sent_flag text, "
            //+ "status text, saved_date long, exported_flag text, uuid text, survey_start integer, surveyed_locale_id text);";
    //private static final String SURVEY_RESPONSE_CREATE = "create table survey_response (survey_response_id integer primary key autoincrement, "
            //+ " survey_respondent_id integer not null, question_id text not null, answer_value text not null, answer_type text not null, include_flag text not null, scored_val text, strength text);";
    //private static final String SURVEYED_LOCALE_TABLE_CREATE = "create table surveyed_locale (_id integer primary key autoincrement, surveyed_locale_id text, survey_group_id integer, name text, latitude real, longitude real, "
            //+ " UNIQUE(surveyed_locale_id) ON CONFLICT REPLACE);";
    //private static final String TRANSMISSION_HISTORY_TABLE_CREATE = "create table transmission_history (_id integer primary key, survey_respondent_id integer not null, status text, filename text, trans_start_date long, delivered_date long);";
    //private static final String PREFERENCES_TABLE_CREATE = "create table preferences (key text primary key, value text);";
    //private static final String SYNC_TIME_TABLE_CREATE = "CREATE TABLE sync_time (_id INTEGER PRIMARY KEY AUTOINCREMENT, survey_group_id INTEGER, time TEXT, UNIQUE (survey_group_id) ON CONFLICT REPLACE);";
    */
    
    public interface Tables {
        String SURVEY = "survey";
        String SURVEY_INSTANCE = "survey_instance";
        String RESPONSE = "survey_response";
        String USER = "user";
        String PREFERENCES = "preferences";
        String TRANSMISSION = "transmission";
        String SURVEY_GROUP = "survey_group";// Introduced in Point Updates
        String RECORD = "record";// Introduced in Point Updates
        String SYNC_TIME = "sync_time";// Introduced in Point Updates

        String SURVEY_INSTANCE_JOIN_RESPONSE_USER = "survey_instance "
                + "LEFT OUTER JOIN response ON survey_instance._id=response.survey_instance_id "
                + "LEFT OUTER JOIN user ON survey_instance.user_id=user._id";
    }

    public interface SurveyGroupColumns {
        String _ID                = "_id";
        String NAME               = "name";
        String REGISTER_SURVEY_ID = "register_survey_id";
        String MONITORED          = "monitored";
    }
    
    public interface RecordColumns {
        String _ID                = "_id";
        String RECORD_ID          = "record_id";
        String SURVEY_GROUP_ID    = "survey_group_id";
        String NAME               = "name";
        String LATITUDE           = "latitude";
        String LONGITUDE          = "longitude";
    }
    
    public interface SyncTimeColumns {
        String _ID                 = "_id";
        String SURVEY_GROUP_ID    = "survey_group_id";
        String TIME               = "time";
    }

    public interface SurveyInstanceColumns {
        String _ID = "_id";
        String UUID = "uuid";
        String SURVEY_ID = "survey_id";
        String USER_ID = "user_id";
        String START_DATE = "start_date";
        String SAVED_DATE = "saved_date";
        String SUBMITTED_DATE = "saved_date";
        String RECORD_ID = "surveyed_locale_id";
        String EXPORTED_DATE = "exported_date";
        String SENT_DATE = "sent_date";
        String STATUS = "status";// Denormalized value. See 'SurveyInstanceStatus'
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

    public interface PreferencesColumns {
        String KEY = "key";
        String VALUE = "value";
    }

    public interface SurveyInstanceStatus {
        int CURRENT    = 0;
        int SAVED      = 1;
        int SUBMITTED  = 2;
        int EXPORTED   = 3;
        int SYNCED     = 4;
        int DOWNLOADED = 5;
    }

    private static final String TAG = "SurveyDbAdapter";
    private DatabaseHelper databaseHelper;
    private SQLiteDatabase database;

    private static final String[] DEFAULT_INSERTS = new String[] {
            "INSERT INTO preferences VALUES('survey.language','')",
            "INSERT INTO preferences VALUES('survey.languagespresent','')",
            "INSERT INTO preferences VALUES('user.storelast','false')",
            "INSERT INTO preferences VALUES('data.cellular.upload','0')",
            "INSERT INTO preferences VALUES('plot.default.mode','manual')",
            "INSERT INTO preferences VALUES('plot.interval','60000')",
            "INSERT INTO preferences VALUES('user.lastuser.id','')",
            "INSERT INTO preferences VALUES('location.sendbeacon','true')",
            "INSERT INTO preferences VALUES('survey.precachehelp','1')",
            "INSERT INTO preferences VALUES('backend.server','')",
            "INSERT INTO preferences VALUES('screen.keepon','true')",
            "INSERT INTO preferences VALUES('precache.points.countries','2')",
            "INSERT INTO preferences VALUES('precache.points.limit','200')",
            "INSERT INTO preferences VALUES('survey.textsize','LARGE')",
            "INSERT INTO preferences VALUES('survey.checkforupdates','0')",
            "INSERT INTO preferences VALUES('remoteexception.upload','0')",
            "INSERT INTO preferences VALUES('survey.media.photo.shrink','true')",
            "INSERT INTO preferences VALUES('survey.media.photo.sizereminder','true')"
    };

    private static final String DATABASE_NAME = "surveydata";

    private static final String RESPONDENT_JOIN = "survey_instance LEFT OUTER JOIN survey ON (survey_instance.survey_id = survey._id)";

    private static final int VER_LAUNCH = 78;// App refactor version. Start from scratch
    private static final int DATABASE_VERSION = VER_LAUNCH;

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
                    + SurveyColumns.SURVEY_ID + " INTEGER NOT NULL,"
                    + SurveyColumns.SURVEY_GROUP_ID + " INTEGER,"// REFERENCES ...
                    + SurveyColumns.NAME + " TEXT NOT NULL,"
                    + SurveyColumns.VERSION + " REAL,"
                    + SurveyColumns.TYPE + " TEXT,"
                    + SurveyColumns.LOCATION + " TEXT,"
                    + SurveyColumns.FILENAME + " TEXT,"
                    + SurveyColumns.LANGUAGE + " TEXT,"
                    + SurveyColumns.HELP_DOWNLOADED + " INTEGER NOT NULL DEFAULT 0,"
                    + SurveyColumns.DELETED + " INTEGER NOT NULL DEFAULT 0,"
                    + "UNIQUE (" + SurveyColumns.SURVEY_ID + ") ON CONFLICT REPLACE)");

            db.execSQL("CREATE TABLE " + Tables.SURVEY_GROUP + " ("
                    + SurveyGroupColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + SurveyGroupColumns.NAME + " TEXT,"
                    + SurveyGroupColumns.REGISTER_SURVEY_ID + " TEXT,"
                    + SurveyGroupColumns.MONITORED + " INTEGER NOT NULL DEFAULT 0)");

            db.execSQL("CREATE TABLE " + Tables.SURVEY_INSTANCE + " ("
                    + SurveyInstanceColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + SurveyInstanceColumns.UUID + " TEXT,"
                    + SurveyInstanceColumns.SURVEY_ID + " INTEGER NOT NULL,"// REFERENCES ...
                    + SurveyInstanceColumns.USER_ID + " INTEGER,"// REFERENCES? beware of the synced records
                    + SurveyInstanceColumns.START_DATE + " INTEGER,"
                    + SurveyInstanceColumns.SAVED_DATE + " INTEGER,"
                    + SurveyInstanceColumns.SUBMITTED_DATE + " INTEGER,"
                    + SurveyInstanceColumns.RECORD_ID + " TEXT,"
                    + SurveyInstanceColumns.STATUS + " INTEGER,"
                    + SurveyInstanceColumns.EXPORTED_DATE + " INTEGER,"
                    + SurveyInstanceColumns.SENT_DATE + " INTEGER,"
                    + "UNIQUE (" + SurveyInstanceColumns.UUID + ") ON CONFLICT REPLACE)");

            db.execSQL("CREATE TABLE " + Tables.RESPONSE + " ("
                    + ResponseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ResponseColumns.SURVEY_INSTANCE_ID + " INTEGER NOT NULL,"// REFERENCES...
                    + ResponseColumns.QUESTION_ID + " TEXT NOT NULL,"
                    + ResponseColumns.ANSWER + " TEXT NOT NULL,"
                    + ResponseColumns.TYPE + " TEXT NOT NULL,"
                    + ResponseColumns.INCLUDE + " INTEGER NOT NULL,"
                    + ResponseColumns.SCORED_VAL + " TEXT,"
                    + ResponseColumns.STRENGTH + " TEXT)");

            db.execSQL("CREATE TABLE " + Tables.RECORD + " ("
                    + RecordColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + RecordColumns.RECORD_ID + " TEXT,"
                    + RecordColumns.SURVEY_GROUP_ID + " INTEGER,"// REFERENCES ...
                    + RecordColumns.NAME + " TEXT,"// REFERENCES ...
                    + RecordColumns.LATITUDE + " REAL,"// REFERENCES ...
                    + RecordColumns.LONGITUDE + " REAL,"// REFERENCES ...
                    + "UNIQUE (" + RecordColumns.RECORD_ID + ") ON CONFLICT REPLACE)");

            db.execSQL("CREATE TABLE " + Tables.TRANSMISSION + " ("
                    + TransmissionColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + TransmissionColumns.SURVEY_INSTANCE_ID + " INTEGER NOT NULL,"
                    + TransmissionColumns.FILENAME + " TEXT,"
                    + TransmissionColumns.STATUS + " INTEGER,"
                    + TransmissionColumns.START_DATE + " INTEGER,"
                    + TransmissionColumns.END_DATE + " INTEGER,"
                    + "UNIQUE (" + TransmissionColumns.FILENAME + ") ON CONFLICT REPLACE)");

            db.execSQL("CREATE TABLE " + Tables.PREFERENCES + " ("
                    + PreferencesColumns.KEY + " TEXT PRIMARY KEY,"
                    + PreferencesColumns.VALUE + " TEXT)");

            db.execSQL("CREATE TABLE " + Tables.SYNC_TIME + " ("
                    + SyncTimeColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + SyncTimeColumns.SURVEY_GROUP_ID + " INTEGER,"
                    + SyncTimeColumns.TIME + " TEXT,"
                    + "UNIQUE (" + SyncTimeColumns.SURVEY_GROUP_ID + ") ON CONFLICT REPLACE)");

            createIndexes(db);
            for (int i = 0; i < DEFAULT_INSERTS.length; i++) {
                db.execSQL(DEFAULT_INSERTS[i]);
            }
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
                case DATABASE_VERSION:
                    break;
            }

            if (version != DATABASE_VERSION) {
                Log.d(TAG, "onUpgrade() - Recreating the Database.");
                
                db.execSQL("DROP TABLE IF EXISTS " + Tables.RESPONSE);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.SYNC_TIME);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.SURVEY);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.PREFERENCES);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.USER);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.SURVEY_GROUP);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.SURVEY_INSTANCE);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.RECORD);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.TRANSMISSION);

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
        }

        /**
         * returns the value of a single setting identified by the key passed in
         */
        public String findPreference(SQLiteDatabase db, String key) {
            String value = null;
            Cursor cursor = db.query(Tables.PREFERENCES, new String[] {
                    PreferencesColumns.KEY,
                    PreferencesColumns.VALUE
            }, PreferencesColumns.KEY + " = ?", new String[] {
                key
            }, null,
                    null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    value = cursor.getString(cursor
                            .getColumnIndexOrThrow(PreferencesColumns.VALUE));
                }
                cursor.close();
            }
            return value;
        }

        /**
         * persists setting to the db
         * 
         * @param surveyId
         */
        public void savePreference(SQLiteDatabase db, String key, String value) {
            ContentValues updatedValues = new ContentValues();
            updatedValues.put(PreferencesColumns.VALUE, value);
            int updated = db.update(Tables.PREFERENCES, updatedValues, PreferencesColumns.KEY
                    + " = ?", new String[] {
                key
            });
            if (updated <= 0) {
                updatedValues.put(PreferencesColumns.KEY, key);
                db.insert(Tables.PREFERENCES, null, updatedValues);
            }
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

    // TODO: Use denormaized status? --> WHERE status IS SurveyInstanceStatus.SUBMITTED
    public Cursor getUnexportedSurveyInstances() {
        return database.query(Tables.SURVEY_INSTANCE,
                new String[] {
                        SurveyInstanceColumns.SURVEY_ID
                },
                SurveyInstanceColumns.SUBMITTED_DATE + " IS NOT NULL AND "
                        + SurveyInstanceColumns.EXPORTED_DATE + " IS NULL",
                null, null, null, null);
    }

    public Cursor getResponses(long surveyInstanceId) {
        return database.query(Tables.SURVEY_INSTANCE_JOIN_RESPONSE_USER,
                new String[] {
                        SurveyInstanceColumns.SURVEY_ID, SurveyInstanceColumns.SUBMITTED_DATE,
                        SurveyInstanceColumns.UUID, SurveyInstanceColumns.START_DATE,
                        SurveyInstanceColumns.RECORD_ID, ResponseColumns.ANSWER,
                        ResponseColumns.TYPE, ResponseColumns.QUESTION_ID, ResponseColumns.STRENGTH,
                        ResponseColumns.SCORED_VAL, UserColumns.NAME, UserColumns.EMAIL
                },
                ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND " + ResponseColumns.INCLUDE + " = 1",
                new String[] {
                        String.valueOf(surveyInstanceId)
                }, null, null, null);
    }

    /**
     * marks the data as submitted in the respondent table (submittedFlag =
     * true) thereby making it ready for transmission
     * 
     * @param respondentId
     */
    public void submitSurveyInstance(long surveyInstanceId) {
        // TODO: DRY!
        ContentValues vals = new ContentValues();
        vals.put(SurveyInstanceColumns.SUBMITTED_DATE, System.currentTimeMillis());
        vals.put(SurveyInstanceColumns.STATUS, SurveyInstanceStatus.SUBMITTED);
        database.update(Tables.SURVEY_INSTANCE, vals,
                SurveyInstanceColumns._ID + "= ? ",
                new String[] { String.valueOf(surveyInstanceId) });
    }

    /**
     * Mark a survey instance as saved, updating the saved time and the status.
     * This values will only be changed if the survey is not submitted yet.
     * @param surveyInstanceId
     */
    public void saveSurveyInstance(long surveyInstanceId) {
        Cursor cursor = database.query(Tables.SURVEY_INSTANCE,
                new String[] {SurveyInstanceColumns.STATUS},
                SurveyInstanceColumns._ID + " = ? AND " + SurveyInstanceColumns.SUBMITTED_DATE
                        + " IS NULL",
                new String[] {String.valueOf(surveyInstanceId)},
                null, null, null);

        boolean save = false;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                save = true;
            }
            cursor.close();
        }

        if (save) {
            ContentValues vals = new ContentValues();
            vals.put(SurveyInstanceColumns.SAVED_DATE, System.currentTimeMillis());
            vals.put(SurveyInstanceColumns.STATUS, SurveyInstanceStatus.SAVED);
            database.update(Tables.SURVEY_INSTANCE, vals,
                    SurveyInstanceColumns._ID + "= ?",
                    new String[] { String.valueOf(surveyInstanceId) });
        }
    }

    public void setSurveyInstanceExported(long surveyInstanceId) {
        updateSurveyStatus(surveyInstanceId, SurveyInstanceStatus.EXPORTED);
    }

    public void setSurveyInstanceSynced(long surveyInstanceId) {
        updateSurveyStatus(surveyInstanceId, SurveyInstanceStatus.SYNCED);
    }

    /**
     * updates the respondent table by recording the sent date stamp
     * 
     * @param idList
    public void markDataAsSent(Set<String> idList, String mediaSentFlag) {
        if (idList != null) {
            ContentValues updatedValues = new ContentValues();
            updatedValues.put(DELIVERED_DATE_COL, System.currentTimeMillis()
                    + "");
            updatedValues.put(MEDIA_SENT_COL, mediaSentFlag);
            // enhanced FOR ok here since we're dealing with an implicit
            // iterator anyway
            for (String id : idList) {
                if (database.update(Tables.RESPONDENT, updatedValues, PK_ID_COL
                        + " = ?", new String[] {
                    id
                }) < 1) {
                    Log.e(TAG,
                            "Could not update record for Survey_respondent_id "
                                    + id);
                }
            }
        }
    }
     */

    /**
     * updates the respondent table by recording the sent date stamp
     * 
     * @param idList
    public void markDataAsExported(Set<String> idList) {
        if (idList != null && idList.size() > 0) {
            ContentValues updatedValues = new ContentValues();
            updatedValues.put(EXPORTED_FLAG_COL, "true");
            // enhanced FOR ok here since we're dealing with an implicit
            // iterator anyway
            for (String id : idList) {
                if (database.update(Tables.RESPONDENT, updatedValues, PK_ID_COL
                        + " = ?", new String[] {
                    id
                }) < 1) {
                    Log.e(TAG, "Could not update record for Survey_respondent_id " + id);
                }
            }
        }
    }
     */

    /**
     * updates the status of a survey instance to the status passed in.
     * Status must be one of the 'SurveyInstanceStatus' one.
     * 
     * @param surveyInstanceId
     * @param status
     */
    private void updateSurveyStatus(long surveyInstanceId, int status) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyInstanceColumns.STATUS, status);
        updatedValues.put(SurveyInstanceColumns.SAVED_DATE, System.currentTimeMillis());

        final int rows = database.update(Tables.SURVEY_INSTANCE,
                updatedValues,
                SurveyInstanceColumns._ID + " = ?",
                new String[] { String.valueOf(surveyInstanceId) });

        if (rows < 1) {
            Log.e(TAG, "Could not update status for Survey Instance: " + surveyInstanceId);
        }
    }

    /**
     * returns a cursor listing all users
     * 
     * @return
     */
    public Cursor listUsers() {
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
    public Cursor findUser(Long id) {
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
            if (database.update(Tables.USER, initialValues, UserColumns._ID + "=?",
                    new String[] {
                        idVal.toString()
                    }) > 0) {
            }
        }
        return idVal;
    }

    /**
     * Return a Cursor over the list of all responses for a particular survey instance
     * 
     * @return Cursor over all responses
     */
    public Cursor fetchResponses(long surveyInstanceId) {
        return database.query(Tables.RESPONSE,
                new String[] {
                    ResponseColumns._ID, ResponseColumns.QUESTION_ID, ResponseColumns.ANSWER,
                    ResponseColumns.TYPE, ResponseColumns.SURVEY_INSTANCE_ID,
                    ResponseColumns.INCLUDE, ResponseColumns.SCORED_VAL, ResponseColumns.STRENGTH
                },
                ResponseColumns.SURVEY_INSTANCE_ID + " = ?",
                new String[] { String.valueOf(surveyInstanceId) },
                null, null, null);
    }

    /**
     * loads a single question response
     * 
     * @param respondentId
     * @param questionId
     * @return
     */
    public QuestionResponse findSingleResponse(Long respondentId, String questionId) {
        QuestionResponse resp = null;
        Cursor cursor = database.query(Tables.RESPONSE,
                new String[] {
                    ResponseColumns._ID, ResponseColumns.QUESTION_ID, ResponseColumns.ANSWER,
                    ResponseColumns.TYPE, ResponseColumns.SURVEY_INSTANCE_ID,
                    ResponseColumns.INCLUDE, ResponseColumns.SCORED_VAL, ResponseColumns.STRENGTH
                },
                ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND " + ResponseColumns.QUESTION_ID
                    + " =?",
                new String[] { String.valueOf(respondentId), questionId },
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            resp = new QuestionResponse();
            resp.setQuestionId(questionId);
            resp.setRespondentId(respondentId);
            resp.setType(cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.TYPE)));
            resp.setValue(cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.ANSWER)));
            resp.setId(cursor.getLong(cursor.getColumnIndexOrThrow(ResponseColumns._ID)));
            resp.setIncludeFlag(cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.INCLUDE)));
            resp.setScoredValue(cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.SCORED_VAL)));
            resp.setStrength(cursor.getString(cursor.getColumnIndexOrThrow(ResponseColumns.STRENGTH)));

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
        QuestionResponse responseToSave = findSingleResponse(
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
        initialValues.put(ResponseColumns.INCLUDE, resp.getIncludeFlag());
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
     * this method will get the max survey respondent ID that has an unsubmitted
     * survey or, if none exists, will create a new respondent
     * 
     * @param surveyId
     * @return
     */
    public long createOrLoadSurveyRespondent(String surveyId, String userId, long surveyGroupId, String surveyedLocaleId) {
        String where = SurveyInstanceColumns.SUBMITTED_DATE + "IS NULL AND "
                + SurveyInstanceColumns.SURVEY_ID + "= ?  AND " + SurveyInstanceColumns.STATUS + " = ? ";
        List<String> argList =  new ArrayList<String>();
        argList.add(surveyId);
        argList.add(String.valueOf(SurveyInstanceStatus.CURRENT));
        
        if (surveyedLocaleId != null) {
            where += " AND " + SurveyInstanceColumns.RECORD_ID + " =  ?";
            argList.add(surveyedLocaleId);
        }
        
        Cursor results = database.query(Tables.SURVEY_INSTANCE,
                new String[] {
                    "max(" + SurveyInstanceColumns._ID + ")"
                },
                where,
                argList.toArray(new String[argList.size()]),
                null, null, null);
        
        long id = -1;
        if (results != null && results.getCount() > 0) {
            results.moveToFirst();
            id = results.getLong(0);
        }
        if (results != null) {
            results.close();
        }
        if (id <= 0) {
            if (surveyedLocaleId == null) {
                surveyedLocaleId = createSurveyedLocale(surveyGroupId);
            }
            id = createSurveyRespondent(surveyId, userId, surveyedLocaleId);
        }
        return id;
    }

    /**
     * creates a new unsubmitted survey respondent record
     * 
     * @param surveyId
     * @return
     */
    public long createSurveyRespondent(String surveyId, String userId, String surveyedLocaleId) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(SurveyInstanceColumns.SURVEY_ID, surveyId);
        initialValues.put(SurveyInstanceColumns.USER_ID, userId);
        initialValues.put(SurveyInstanceColumns.STATUS, SurveyInstanceStatus.CURRENT);
        initialValues.put(SurveyInstanceColumns.UUID, UUID.randomUUID().toString());
        initialValues.put(SurveyInstanceColumns.START_DATE, System.currentTimeMillis());
        initialValues.put(SurveyInstanceColumns.RECORD_ID, surveyedLocaleId);
        return database.insert(Tables.SURVEY_INSTANCE, null, initialValues);
    }

    /**
     * returns a list of survey objects that are out of date (missing from the
     * db or with a lower version number). If a survey is present but marked as
     * deleted, it will not be listed as out of date (and thus won't be updated)
     * 
     * @param surveys
     * @return
     */
    public List<Survey> checkSurveyVersions(List<Survey> surveys) {
        List<Survey> outOfDateSurveys = new ArrayList<Survey>();
        for (int i = 0; i < surveys.size(); i++) {
            Cursor cursor = database.query(Tables.SURVEY,
                    new String[] {
                        SurveyColumns._ID
                    },
                    SurveyColumns._ID + " = ? and (" + SurveyColumns.VERSION + " >= ? or "
                            + SurveyColumns.DELETED + " = ?)", new String[] {
                            surveys.get(i).getId(),
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
     * 
     * @param idList
     */
    public void markSurveyHelpDownloaded(String surveyId, boolean isDownloaded) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyColumns.HELP_DOWNLOADED, isDownloaded ? 1 : 0);

        if (database.update(Tables.SURVEY, updatedValues, SurveyColumns._ID + " = ?",
                new String[] {
                    surveyId
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
        updatedValues.put(SurveyColumns.HELP_DOWNLOADED, survey.isHelpDownloaded() ? "Y"
                : "N");
        //updatedValues.put(SurveyColumns., ConstantUtil.NOT_DELETED);

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
     * Gets a single survey from the db using its primary key
     */
    public Survey findSurvey(String surveyId) {
        Survey survey = null;
        Cursor cursor = database.query(Tables.SURVEY, new String[] {
                SurveyColumns.SURVEY_ID, SurveyColumns.NAME, SurveyColumns.LOCATION,
                SurveyColumns.FILENAME, SurveyColumns.TYPE, SurveyColumns.LANGUAGE,
                SurveyColumns.HELP_DOWNLOADED
        }, SurveyColumns.SURVEY_ID + " = ?",
                new String[] {
                    surveyId
                }, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                survey = new Survey();
                survey.setId(surveyId);
                survey.setName(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.NAME)));
                survey.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LOCATION)));
                survey.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.FILENAME)));
                survey.setType(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.TYPE)));
                survey.setHelpDownloaded(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.HELP_DOWNLOADED)));// TODO: Type conversion!
                survey.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LANGUAGE)));
            }
            cursor.close();
        }

        return survey;
    }

    /**
     * Lists all non-deleted surveys from the database
     */
    public ArrayList<Survey> listSurveys(long surveyGroupId) {
        ArrayList<Survey> surveys = new ArrayList<Survey>();
        String whereClause = SurveyColumns.DELETED + " <> 1";
        String[] whereParams = null;
        if (surveyGroupId > 0) {
            whereClause += " and " + SurveyColumns.SURVEY_GROUP_ID + " = ?";
            whereParams = new String[] {
                    String.valueOf(surveyGroupId)
            };
        }
        Cursor cursor = database.query(Tables.SURVEY, new String[] {
                SurveyColumns.SURVEY_ID, SurveyColumns.NAME, SurveyColumns.LOCATION, SurveyColumns.FILENAME,
                SurveyColumns.TYPE, SurveyColumns.LANGUAGE, SurveyColumns.HELP_DOWNLOADED, SurveyColumns.VERSION
        }, whereClause,
                whereParams, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    Survey survey = new Survey();
                    survey.setId(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.SURVEY_ID)));
                    survey.setName(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.NAME)));
                    survey.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LOCATION)));
                    survey.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.FILENAME)));
                    survey.setType(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.TYPE)));
                    survey.setHelpDownloaded(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.HELP_DOWNLOADED)));// TODO: Type conversion!
                    survey.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LANGUAGE)));
                    surveys.add(survey);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return surveys;
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
                    new String[] {
                        surveyId
                    });
        } else {
            database.delete(Tables.SURVEY, SurveyColumns.SURVEY_ID + " = ? ",
                    new String[] {
                        surveyId
                    });
        }
    }

    /**
     * returns the value of a single setting identified by the key passed in
     */
    public String findPreference(String key) {
        return databaseHelper.findPreference(database, key);
    }

    /**
     * Lists all settings from the database
     */
    public HashMap<String, String> listPreferences() {
        HashMap<String, String> settings = new HashMap<String, String>();
        Cursor cursor = database.query(Tables.PREFERENCES, new String[] {
                PreferencesColumns.KEY, PreferencesColumns.VALUE
        }, null, null, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    settings.put(cursor.getString(cursor
                            .getColumnIndexOrThrow(PreferencesColumns.KEY)), cursor
                            .getString(cursor.getColumnIndexOrThrow(PreferencesColumns.VALUE)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return settings;
    }

    /**
     * persists setting to the db
     * 
     * @param surveyId
     */
    public void savePreference(String key, String value) {
        databaseHelper.savePreference(database, key, value);
    }

    /**
     * deletes all the surveys from the database
     */
    public void deleteAllSurveys() {
        database.delete(Tables.SURVEY, null, null);
    }

    /**
     * deletes all survey responses from the database for a specific respondent
     */
    public void deleteResponses(String respondentId) {
        database.delete(Tables.RESPONSE, ResponseColumns.SURVEY_INSTANCE_ID + "= ?",
                new String[] {
                    respondentId
                });
    }

    /**
     * deletes the respondent record and any responses it contains
     * 
     * @param respondentId
     */
    public void deleteRespondent(String respondentId) {
        deleteResponses(respondentId);
        database.delete(Tables.SURVEY_INSTANCE, SurveyInstanceColumns._ID + "=?",
                new String[] {
                    respondentId
                });
    }

    /**
     * deletes a single response
     * 
     * @param respondentId
     * @param questionId
     */
    public void deleteResponse(String respondentId, String questionId) {
        database.delete(Tables.RESPONSE, ResponseColumns.SURVEY_INSTANCE_ID + "= ? AND "
                + ResponseColumns.QUESTION_ID + "= ?", new String[] {
                respondentId,
                questionId
        });
    }

    public void createTransmission (long surveyInstanceId, String filename) {
        createTransmission(surveyInstanceId, filename, ConstantUtil.QUEUED_STATUS);
    }


    public void createTransmission (long surveyInstanceId, String filename, String status) {
        ContentValues values = new ContentValues();
        values.put(TransmissionColumns.SURVEY_INSTANCE_ID, surveyInstanceId);
        values.put(TransmissionColumns.FILENAME, filename);
        values.put(TransmissionColumns.STATUS, status);
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
    public int updateTransmissionHistory(String fileName, String status) {
        // TODO: Update Survey Instance STATUS as well
        ContentValues vals = new ContentValues();
        vals.put(TransmissionColumns.STATUS, status);
        if (ConstantUtil.COMPLETE_STATUS.equals(status)) {
            vals.put(TransmissionColumns.END_DATE, System.currentTimeMillis() + "");
        } else if (ConstantUtil.IN_PROGRESS_STATUS.equals(status)) {
            vals.put(TransmissionColumns.START_DATE, System.currentTimeMillis() + "");
        }

        return database.update(Tables.TRANSMISSION, vals,
                TransmissionColumns.FILENAME + " = ?",
                new String[] {fileName});
    }

    /**
     * Get the list of queued and failed transmissions
     */
    public List<FileTransmission> getUnsyncedTransmissions() {
        List<FileTransmission> transmissions = new ArrayList<FileTransmission>();
        Cursor cursor = database.query(Tables.TRANSMISSION,
                new String[] {
                        TransmissionColumns._ID, TransmissionColumns.SURVEY_INSTANCE_ID,
                        TransmissionColumns.STATUS, TransmissionColumns.SURVEY_INSTANCE_ID
                },
                TransmissionColumns.STATUS + " IN (?, ?)",
                new String[] {ConstantUtil.FAILED_STATUS, ConstantUtil.QUEUED_STATUS},
                null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                transmissions = new ArrayList<FileTransmission>();
                do {
                    FileTransmission trans = new FileTransmission();
                    trans.setId(cursor.getLong(
                            cursor.getColumnIndexOrThrow(TransmissionColumns._ID)));
                    trans.setRespondentId(cursor.getLong(
                            cursor.getColumnIndexOrThrow(TransmissionColumns.SURVEY_INSTANCE_ID)));
                    trans.setFileName(cursor.getString(cursor
                            .getColumnIndexOrThrow(TransmissionColumns.FILENAME)));
                    trans.setStatus(cursor.getString(cursor
                            .getColumnIndexOrThrow(TransmissionColumns.STATUS)));
                    transmissions.add(trans);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return transmissions;
    }

    /**
     * inserts a transmissionHistory row into the db
     * 
     * @param respId
     * @param fileName
     * @param status
     * @return uid of created record
    public Long createTransmissionHistory(Long respId, String fileName,
            String status) {
        ContentValues initialValues = new ContentValues();
        Long idVal = null;
        initialValues.put(SURVEY_RESPONDENT_ID_COL, respId);
        initialValues.put(FILENAME_COL, fileName);

        if (status != null) {
            final long time = System.currentTimeMillis();
            initialValues.put(STATUS_COL, status);
            if (ConstantUtil.IN_PROGRESS_STATUS.equals(status)) {
                initialValues.put(TRANS_START_COL, time);
            } else if (ConstantUtil.DOWNLOADED_STATUS.equals(status)) {
                // Mark both columns with the same timestamp.
                initialValues.put(TRANS_START_COL, time);
                initialValues.put(DELIVERED_DATE_COL, time);
            }
        } else {
            initialValues.put(TRANS_START_COL, (Long) null);
            initialValues.put(STATUS_COL, ConstantUtil.QUEUED_STATUS);
        }
        idVal = database
                .insert(Tables.TRANSMISSION_HISTORY, null, initialValues);
        return idVal;
    }
     */

    /**
     * updates the first matching transmission history record with the status
     * passed in. If the status == Completed, the completion date is updated. If
     * the status == In Progress, the start date is updated.
     * 
     * @param respondId
     * @param fileName
     * @param status
    public void updateTransmissionHistory(Long respondId, String fileName,
            String status) {
        List<FileTransmission> transList = listFileTransmission(respondId,
                fileName, true);
        Long idVal = null;
        if (transList != null && transList.size() > 0) {
            idVal = transList.get(0).getId();
            if (idVal != null) {
                ContentValues vals = new ContentValues();
                vals.put(STATUS_COL, status);
                if (ConstantUtil.COMPLETE_STATUS.equals(status)) {
                    vals.put(DELIVERED_DATE_COL, System.currentTimeMillis()
                            + "");
                } else if (ConstantUtil.IN_PROGRESS_STATUS.equals(status)) {
                    vals.put(TRANS_START_COL, System.currentTimeMillis() + "");
                }
                database.update(Tables.TRANSMISSION_HISTORY, vals, PK_ID_COL
                        + " = ?", new String[] {
                    idVal.toString()
                });
            }
            else
                // it should have been found
                Log.e(TAG,
                        "Could not update transmission history record for respondent_id "
                                + respondId
                                + " filename "
                                + fileName);
        }
    }

    public void updateTransmissionHistory(Set<String> respondentIDs, String fileName,
            String status) {
        for (String id : respondentIDs) {
            updateTransmissionHistory(Long.valueOf(id), fileName, status);
        }
    }
     */

    /**
     * lists all the file transmissions for the values passed in.
     * 
     * @param respondentId - MANDATORY id of the survey respondent
     * @param fileName - OPTIONAL file name
     * @param incompleteOnly - if true, only rows without a complete status will
     *            be returned
     * @return
    public List<FileTransmission> listFileTransmission(Long respondentId,
            String fileName, boolean incompleteOnly) {
        List<FileTransmission> transList = null;

        String whereClause = SURVEY_RESPONDENT_ID_COL + "=?";
        if (incompleteOnly) {
            whereClause = whereClause + " AND " + STATUS_COL + " <> '"
                    + ConstantUtil.COMPLETE_STATUS + "'";
        }
        String[] whereValues = null;

        if (fileName != null && fileName.trim().length() > 0) {
            whereClause = whereClause + " AND " + FILENAME_COL + " = ?";
            whereValues = new String[2];
            whereValues[0] = respondentId.toString();
            whereValues[1] = fileName;

        } else {
            whereValues = new String[] {
                respondentId.toString()
            };
        }

        Cursor cursor = database.query(Tables.TRANSMISSION_HISTORY,
                new String[] {
                        PK_ID_COL, FILENAME_COL, STATUS_COL,
                        TRANS_START_COL, DELIVERED_DATE_COL,
                        SURVEY_RESPONDENT_ID_COL
                }, whereClause, whereValues,
                null, null, TRANS_START_COL + " desc");
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                transList = new ArrayList<FileTransmission>();
                do {
                    FileTransmission trans = new FileTransmission();
                    trans.setId(cursor.getLong(cursor
                            .getColumnIndexOrThrow(PK_ID_COL)));
                    trans.setRespondentId(respondentId);
                    trans.setFileName(cursor.getString(cursor
                            .getColumnIndexOrThrow(FILENAME_COL)));
                    Long startDateMillis = cursor.getLong(cursor
                            .getColumnIndexOrThrow(TRANS_START_COL));
                    if (startDateMillis != null && startDateMillis > 0) {
                        trans.setStartDate(new Date(startDateMillis));
                    }
                    Long delivDateMillis = cursor.getLong(cursor
                            .getColumnIndexOrThrow(DELIVERED_DATE_COL));
                    if (delivDateMillis != null && delivDateMillis > 0) {
                        trans.setEndDate(new Date(delivDateMillis));
                    }
                    trans.setStatus(cursor.getString(cursor
                            .getColumnIndexOrThrow(STATUS_COL)));
                    transList.add(trans);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return transList;
    }
     */

    /**
     * marks submitted data as unsent. If an ID is passed in, only that
     * submission will be updated. If id is null, ALL data will be marked as
     * unsent.
    public void markDataUnsent(Long respondentId) {
        executeSql("update survey_respondent set media_sent_flag = 'false', delivered_date = null where _id = "
                + respondentId);
    }
    
    public void markRecordUnsent(String recordId) {
        executeSql("update survey_respondent set media_sent_flag = 'false', delivered_date = null where surveyed_locale_id = '"
                + recordId + "'");
    }
    
    public void markSurveyGroupUnsent(long surveyGroupId) {
        executeSql("update survey_respondent set media_sent_flag = 'false', delivered_date = null where survey_id in "
                + "(select _id from survey where survey_group_id = " + surveyGroupId + ")");
    }
     */

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
        executeSql("delete from survey");
        executeSql("delete from user");
        executeSql("update preferences set value = '' where key = 'user.lastuser.id'");
    }

    /**
     * Permanently deletes user generated data from the database. It will clear
     * any response saved in the database, as well as the transmission history.
     */
    public void clearCollectedData() {
        executeSql("delete from survey_instance");
        executeSql("delete from survey_response");
        executeSql("delete from transmission");
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
        StringBuffer buffer = new StringBuffer();
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

        String langsSelection = findPreference(ConstantUtil.SURVEY_LANG_SETTING_KEY);
        String langsPresentIndexes = findPreference(ConstantUtil.SURVEY_LANG_PRESENT_KEY);

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

        savePreference(ConstantUtil.SURVEY_LANG_SETTING_KEY, newLangsSelection);
        savePreference(ConstantUtil.SURVEY_LANG_PRESENT_KEY,
                newLangsPresentIndexes);
    }
    
    public void addSurveyGroup(SurveyGroup surveyGroup) {
        ContentValues values = new ContentValues();
        values.put(SurveyGroupColumns._ID, surveyGroup.getId());
        values.put(SurveyGroupColumns.NAME, surveyGroup.getName());
        values.put(SurveyGroupColumns.REGISTER_SURVEY_ID, surveyGroup.getRegisterSurveyId());
        values.put(SurveyGroupColumns.MONITORED, surveyGroup.isMonitored() ? 1 : 0);
        database.insert(Tables.SURVEY_GROUP, null, values);
    }
    
    public static SurveyGroup getSurveyGroup(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(SurveyGroupColumns._ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(SurveyGroupColumns.NAME));
        String registerSurveyId = cursor.getString(cursor.getColumnIndexOrThrow(SurveyGroupColumns.REGISTER_SURVEY_ID));
        boolean monitored = cursor.getInt(cursor.getColumnIndexOrThrow(SurveyGroupColumns.MONITORED)) > 0;
        return new SurveyGroup(id, name, registerSurveyId, monitored);
    }
    
    public Cursor getSurveyGroups() {
        Cursor cursor = database.query(Tables.SURVEY_GROUP, 
                new String[] {SurveyGroupColumns._ID, SurveyGroupColumns.NAME, SurveyGroupColumns.REGISTER_SURVEY_ID, SurveyGroupColumns.MONITORED},
                null, null, null, null, null);
        
        return cursor;
    }
    
    public Cursor getSurveyGroup(long id) {
        String where = null;
        String[] selectionArgs = null;
        
        if (id != SurveyGroup.ID_NONE) {
            where = SurveyGroupColumns._ID + "= ?";
            selectionArgs = new String[] {String.valueOf(id)};
        }
        
        Cursor cursor = database.query(Tables.SURVEY_GROUP, 
                new String[] {SurveyGroupColumns._ID, SurveyGroupColumns.NAME, SurveyGroupColumns.REGISTER_SURVEY_ID, SurveyGroupColumns.MONITORED},
                where, selectionArgs,
                null, null, null);
        
        return cursor;
    }
    
    public String createSurveyedLocale(long surveyGroupId) {
        String base32Id = Base32.base32Uuid();
        // Put dashes between the 4-5 and 8-9 positions to increase readability
        String id = base32Id.substring(0, 4) + "-" + base32Id.substring(4, 8) + "-" + base32Id.substring(8);
        String name = "Unknown";// TODO
        double lat = 0.0d;// TODO
        double lon = 0.0d;// TODO
        ContentValues values = new ContentValues();
        values.put(RecordColumns.RECORD_ID, id);
        values.put(RecordColumns.SURVEY_GROUP_ID, surveyGroupId);
        values.put(RecordColumns.NAME, name);
        values.put(RecordColumns.LATITUDE, lat);
        values.put(RecordColumns.LONGITUDE, lon);
        database.insert(Tables.RECORD, null, values);
        
        return id;
    }
    
    public static SurveyedLocale getSurveyedLocale(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(RecordColumns.RECORD_ID));
        long surveyGroupId = cursor.getLong(cursor.getColumnIndexOrThrow(RecordColumns.SURVEY_GROUP_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(RecordColumns.NAME));
        double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(RecordColumns.LATITUDE));
        double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(RecordColumns.LONGITUDE));
        return new SurveyedLocale(id, name, surveyGroupId, latitude, longitude);
    }
    
    public Cursor getSurveyedLocales(long surveyGroupId) {
        Cursor cursor = database.query(Tables.RECORD,
                new String[] {RecordColumns._ID, RecordColumns.RECORD_ID, RecordColumns.SURVEY_GROUP_ID,
                        RecordColumns.NAME, RecordColumns.LATITUDE, RecordColumns.LONGITUDE},
                RecordColumns.SURVEY_GROUP_ID + " = ?",
                new String[] {String.valueOf(surveyGroupId)},
                null, null, null);
        
        return cursor;
    }
    
    public SurveyedLocale getSurveyedLocale(String surveyedLocaleId) {
        Cursor cursor = database.query(Tables.RECORD,
                new String[] {RecordColumns._ID, RecordColumns.RECORD_ID, RecordColumns.SURVEY_GROUP_ID,
                        RecordColumns.NAME, RecordColumns.LATITUDE, RecordColumns.LONGITUDE},
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
        survey.setHelpDownloaded(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.HELP_DOWNLOADED)));
        survey.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LANGUAGE)));
        survey.setVersion(cursor.getDouble(cursor.getColumnIndexOrThrow(SurveyColumns.VERSION)));
        return survey;
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
                    SurveyColumns._ID, SurveyColumns.NAME, SurveyColumns.LOCATION,
                    SurveyColumns.FILENAME, SurveyColumns.TYPE, SurveyColumns.LANGUAGE,
                    SurveyColumns.HELP_DOWNLOADED, SurveyColumns.VERSION
                },
                whereClause, whereParams, null, null, null);
    }
    
    public Cursor getSurveyInstances(long surveyGroupId) {
        Cursor cursor = database.query(RESPONDENT_JOIN,
                new String[] {
                    Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns._ID, SurveyColumns.NAME,
                    SurveyInstanceColumns.SAVED_DATE, SurveyInstanceColumns.SURVEY_ID,
                    SurveyInstanceColumns.USER_ID, SurveyInstanceColumns.SUBMITTED_DATE,
                    SurveyInstanceColumns.UUID, SurveyInstanceColumns.STATUS
                },
                Tables.SURVEY + "." + SurveyColumns.SURVEY_GROUP_ID + "= ?",
                new String[]{String.valueOf(surveyGroupId)},
                null, null, SurveyInstanceColumns.SUBMITTED_DATE + " DESC");
        return cursor;
    }
    
    public Cursor getSurveyInstances(String surveyedLocaleId) {
        Cursor cursor = database.query(RESPONDENT_JOIN,
                new String[] {
                        Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns._ID, SurveyColumns.NAME,
                        SurveyInstanceColumns.SAVED_DATE, SurveyInstanceColumns.SURVEY_ID,
                        SurveyInstanceColumns.USER_ID, SurveyInstanceColumns.SUBMITTED_DATE,
                        SurveyInstanceColumns.UUID, SurveyInstanceColumns.RECORD_ID,
                        SurveyInstanceColumns.STATUS
                },
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.RECORD_ID + "= ?",
                new String[]{String.valueOf(surveyedLocaleId)},
                null, null, SurveyInstanceColumns.SUBMITTED_DATE + " DESC");
        return cursor;
    }
    
    /**
     * Given a particular surveyedLocale and one of its surveys,
     * retrieves the ID of the last surveyInstance matching that criteria
     * @param surveyedLocaleId
     * @param surveyId
     * @return last surveyInstance with those attributes
     */
    public Long getLastSurveyInstance(String surveyedLocaleId, long surveyId) {
        Cursor cursor = database.query(Tables.SURVEY_INSTANCE,
                new String[] {
                    SurveyInstanceColumns._ID, SurveyInstanceColumns.RECORD_ID,
                    SurveyInstanceColumns.SURVEY_ID, SurveyInstanceColumns.SUBMITTED_DATE
                },
                SurveyInstanceColumns.RECORD_ID + "= ? AND " + SurveyInstanceColumns.SURVEY_ID
                        + "= ? AND " + SurveyInstanceColumns.SUBMITTED_DATE + " IS NOT NULL",
                new String[]{surveyedLocaleId, String.valueOf(surveyId)},
                null, null,
                SurveyInstanceColumns.SUBMITTED_DATE + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getLong(cursor.getColumnIndexOrThrow(SurveyInstanceColumns._ID));
        }
        
        return null;
    }
    
    public String getSurveyedLocaleId(long surveyInstanceId) {
        Cursor cursor = database.query(RESPONDENT_JOIN,
                new String[] {
                    Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns._ID,
                    SurveyInstanceColumns.RECORD_ID
                },
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns._ID + "= ?",
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
    public enum SurveyedLocaleMeta {NAME, GEOLOCATION};
    
    public void updateSurveyedLocale(long surveyInstanceId, String response, SurveyedLocaleMeta type) {
        if (!TextUtils.isEmpty(response)) {
            String surveyedLocaleId = getSurveyedLocaleId(surveyInstanceId);
            ContentValues surveyedLocaleValues = new ContentValues();
            
            QuestionResponse metaResponse = new QuestionResponse();
            metaResponse.setRespondentId(surveyInstanceId);
            metaResponse.setValue(response);
            metaResponse.setIncludeFlag("true");
            
            switch (type) {
                case NAME:
                    surveyedLocaleValues.put(RecordColumns.NAME, response);
                    metaResponse.setType("META_NAME");
                    metaResponse.setQuestionId(ConstantUtil.QUESTION_LOCALE_NAME);
                    break;
                case GEOLOCATION:
                    String[] parts = response != null ? response.split("\\|") : new String[]{};
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
    * Filters surveyd locales based on the parameters passed in.
    * @param projectId
    * @param latitude
    * @param longitude
    * @param filterString
    * @param nearbyRadius
    * @return
    */
    public Cursor getFilteredSurveyedLocales(long surveyGroupId, Double latitude, Double longitude,
                Double nearbyRadius, int orderBy) {
        String queryString = "SELECT sl.*, MAX(r." + SurveyInstanceColumns.SUBMITTED_DATE + ") as "
                + SurveyInstanceColumns.SUBMITTED_DATE + " FROM "
                + Tables.RECORD + " AS sl LEFT JOIN " + Tables.SURVEY_INSTANCE + " AS r ON "
                + "sl." + RecordColumns.RECORD_ID + "=" + "r." + SurveyInstanceColumns.RECORD_ID;
        String whereClause = " WHERE sl." + RecordColumns.SURVEY_GROUP_ID + " =?";
        String groupBy = " GROUP BY sl." + RecordColumns.RECORD_ID;
        String orderByStr = " ORDER BY " + SurveyInstanceColumns.SUBMITTED_DATE + " DESC";// By date
        
        // location part
        if (orderBy == ConstantUtil.ORDER_BY_DISTANCE && latitude != null && longitude != null){
            // this is to correct the distance for the shortening at higher latitudes
            Double fudge = Math.pow(Math.cos(Math.toRadians(latitude)),2);
            
            // this uses a simple planar approximation of distance. this should be good enough for our purpose.
            String orderByTempl = " ORDER BY ((%s - " + RecordColumns.LATITUDE + ") * (%s - " + RecordColumns.LATITUDE + ") + (%s - " + RecordColumns.LONGITUDE + ") * (%s - " + RecordColumns.LONGITUDE + ") * %s)";
            orderByStr = String.format(orderByTempl, latitude, latitude, longitude, longitude, fudge);
        } 
        
        String[] whereValues = new String[] {String.valueOf(surveyGroupId)};
        Cursor cursor = database.rawQuery(queryString + whereClause + groupBy + orderByStr, whereValues);
        
        return cursor;
    }
    
    public int getSurveyedLocalesCount(long surveyGroupId) {
        Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + Tables.RECORD
                + " WHERE " + RecordColumns.SURVEY_GROUP_ID + " = ?",
                new String[]{String.valueOf(surveyGroupId)});
        
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        
        return 0;
    }
    
    // ======================================================= //
    // =========== SurveyedLocales synchronization =========== //
    // ======================================================= //
    
    public void syncResponses(List<QuestionResponse> responses, long surveyInstanceId) {
        for (QuestionResponse response : responses) {
            Cursor cursor = database.query(Tables.RESPONSE, new String[] {
                    "survey_respondent_id, question_id"},
                    "survey_respondent_id = ? AND question_id = ?",
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
                        "survey_respondent_id = ? AND question_id = ?",
                        new String[] { String.valueOf(surveyInstanceId), response.getQuestionId()});
            } else {
                database.insert(Tables.RESPONSE, null, values);
            }
        }
    }
    
    public void syncSurveyInstances(List<SurveyInstance> surveyInstances, String surveyedLocaleId) {
        for (SurveyInstance surveyInstance : surveyInstances) {
            Cursor cursor = database.query(Tables.SURVEY_INSTANCE, new String[] {
                    SurveyInstanceColumns._ID, SurveyInstanceColumns.UUID},
                    SurveyInstanceColumns.UUID + " = ?",
                    new String[] { surveyInstance.getUuid()},
                    null, null, null);
                
            long id = -1;
            if (cursor.moveToFirst()) {
                id = cursor.getInt(0);
            }
            cursor.close();
                
            ContentValues values = new ContentValues();
            values.put(SurveyInstanceColumns.SURVEY_ID, surveyInstance.getSurveyId());
            values.put(SurveyInstanceColumns.SUBMITTED_DATE, surveyInstance.getDate());
            values.put(SurveyInstanceColumns.RECORD_ID, surveyedLocaleId);
            values.put(SurveyInstanceColumns.STATUS, SurveyInstanceStatus.DOWNLOADED);
                
            if (id != -1) {
                database.update(Tables.SURVEY_INSTANCE, values, SurveyInstanceColumns.UUID
                        + " = ?", new String[] { surveyInstance.getUuid()});
            } else {
                values.put(SurveyInstanceColumns.UUID, surveyInstance.getUuid());
                id = database.insert(Tables.SURVEY_INSTANCE, null, values);
            }
            
            //createTransmissionHistory(id, null, ConstantUtil.DOWNLOADED_STATUS);
                
            // Now the responses...
            syncResponses(surveyInstance.getResponses(), id);
        }
    }
    
    public void syncSurveyedLocales(List<SurveyedLocale> surveyedLocales) {
        for (SurveyedLocale surveyedLocale : surveyedLocales) {
            try {
                database.beginTransaction();
                
                ContentValues values = new ContentValues();
                values.put(RecordColumns.RECORD_ID, surveyedLocale.getId());
                values.put(RecordColumns.SURVEY_GROUP_ID, surveyedLocale.getSurveyGroupId());
                values.put(RecordColumns.NAME, surveyedLocale.getName());
                values.put(RecordColumns.LATITUDE, surveyedLocale.getLatitude());
                values.put(RecordColumns.LONGITUDE, surveyedLocale.getLongitude());
                database.insert(Tables.RECORD, null, values);
                
                syncSurveyInstances(surveyedLocale.getSurveyInstances(), surveyedLocale.getId());
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
    }
    
    /**
     * Get the synchronization time for a particular survey group.
     * @param surveyGroupId id of the SurveyGroup
     * @return time if exists for this key, null otherwise
     */
    public String getSyncTime(long surveyGroupId) {
        Cursor cursor = database.query(Tables.SYNC_TIME, 
                new String[] {SyncTimeColumns.SURVEY_GROUP_ID, SyncTimeColumns.TIME},
                SyncTimeColumns.SURVEY_GROUP_ID + "=?",
                new String[] {String.valueOf(surveyGroupId)},
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
    public void setSyncTime(long surveyGroupId, String time) {
        ContentValues values = new ContentValues();
        values.put(SyncTimeColumns.SURVEY_GROUP_ID, surveyGroupId);
        values.put(SyncTimeColumns.TIME, time);
        database.insert(Tables.SYNC_TIME, null, values);
    }

}
