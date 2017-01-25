/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import org.akvo.flow.data.preference.InsertablePreferences;
import org.akvo.flow.data.preference.MigratablePreferences;
import org.akvo.flow.data.preference.PreferenceExtractor;
import org.akvo.flow.data.preference.PreferenceHandler;
import org.akvo.flow.data.preference.PreferenceMapper;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.util.ConstantUtil;

import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * Helper class for creating the database tables and loading reference data
 * It is declared with package scope for VM optimizations
 *
 * @author Christopher Fagiani
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "surveydata";
    private static final int VER_LAUNCH = 78;// App refactor version. Start from scratch
    private static final int VER_FORM_SUBMITTER = 79;
    private static final int VER_FORM_DEL_CHECK = 80;
    private static final int VER_FORM_VERSION = 81;
    private static final int VER_CADDISFLY_QN = 82;
    private static final int VER_PREFERENCES_MIGRATE = 83;
    private static final int DATABASE_VERSION = VER_PREFERENCES_MIGRATE;

    /**
     * Default values for languages
     */
    private static final String[] DEFAULT_INSERTS = new String[] {
            "INSERT INTO preferences VALUES('"+ ConstantUtil.SURVEY_LANG_SETTING_KEY+"','')",
            "INSERT INTO preferences VALUES('"+ ConstantUtil.SURVEY_LANG_PRESENT_KEY+"','')",
    };

    private static SQLiteDatabase database;
    private static final Object LOCK_OBJ = new Object();
    private volatile static int instanceCount = 0;
    private final PreferenceHandler preferenceHandler = new PreferenceHandler();
    private WeakReference<Context> contextWeakReference;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.contextWeakReference = new WeakReference<>(context);
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
                + "UNIQUE (" + SurveyColumns.SURVEY_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SURVEY_GROUP + " ("
                + SurveyGroupColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SurveyGroupColumns.SURVEY_GROUP_ID + " INTEGER,"
                + SurveyGroupColumns.NAME + " TEXT,"
                + SurveyGroupColumns.REGISTER_SURVEY_ID + " TEXT,"
                + SurveyGroupColumns.MONITORED + " INTEGER NOT NULL DEFAULT 0,"
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
                + SurveyInstanceColumns.VERSION + " REAL,"
                + "UNIQUE (" + SurveyInstanceColumns.UUID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.RESPONSE + " ("
                + ResponseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ResponseColumns.SURVEY_INSTANCE_ID + " INTEGER NOT NULL,"// REFERENCES...
                + ResponseColumns.QUESTION_ID + " TEXT NOT NULL,"
                + ResponseColumns.ANSWER + " TEXT NOT NULL,"
                + ResponseColumns.TYPE + " TEXT NOT NULL,"
                + ResponseColumns.INCLUDE + " INTEGER NOT NULL DEFAULT 1,"
                + ResponseColumns.FILENAME + " TEXT)");

        db.execSQL("CREATE TABLE " + Tables.RECORD + " ("
                + RecordColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + RecordColumns.RECORD_ID + " TEXT,"
                + RecordColumns.SURVEY_GROUP_ID + " INTEGER,"// REFERENCES ...
                + RecordColumns.NAME + " TEXT,"// REFERENCES ...
                + RecordColumns.LATITUDE + " REAL,"// REFERENCES ...
                + RecordColumns.LONGITUDE + " REAL,"// REFERENCES ...
                + RecordColumns.LAST_MODIFIED + " INTEGER NOT NULL DEFAULT 0,"
                + "UNIQUE (" + RecordColumns.RECORD_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.TRANSMISSION + " ("
                + TransmissionColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TransmissionColumns.SURVEY_INSTANCE_ID + " INTEGER NOT NULL,"
                + TransmissionColumns.SURVEY_ID + " TEXT,"
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
        Timber.d("Upgrading database from version " + oldVersion + " to " + newVersion);

        Context context = contextWeakReference.get();
        if (oldVersion < DATABASE_VERSION && context != null) {
            migratePreferences(context, db);
        }
        // Apply database updates sequentially. It starts in the current
        // version, hooking into the correspondent case block, and falls
        // through to any future upgrade. If no break statement is found,
        // the upgrade will end up in the current version.
        switch (oldVersion) {
            case VER_LAUNCH:
                db.execSQL("ALTER TABLE " + Tables.SURVEY_INSTANCE
                        + " ADD COLUMN " + SurveyInstanceColumns.SUBMITTER + " TEXT");
            case VER_FORM_SUBMITTER:
                db.execSQL("ALTER TABLE " + Tables.TRANSMISSION
                        + " ADD COLUMN " + TransmissionColumns.SURVEY_ID + " TEXT");
            case VER_FORM_DEL_CHECK:
                db.execSQL("ALTER TABLE " + Tables.SURVEY_INSTANCE
                        + " ADD COLUMN " + SurveyInstanceColumns.VERSION + " REAL");
            case VER_FORM_VERSION:
                db.execSQL("ALTER TABLE " + Tables.RESPONSE
                        + " ADD COLUMN " + ResponseColumns.FILENAME + " TEXT");
                oldVersion = VER_CADDISFLY_QN;
        }

        if (oldVersion < VER_CADDISFLY_QN) {
            Timber.d("onUpgrade() - Recreating the Database.");

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

    private void migratePreferences(Context context, SQLiteDatabase db) {
        PreferenceMapper mapper = new PreferenceMapper();
        Prefs prefs = new Prefs(context.getApplicationContext());
        PreferenceExtractor preferenceExtractor = new PreferenceExtractor();
        MigratablePreferences migratablePreferences = preferenceExtractor
                .create(preferenceHandler, db);
        InsertablePreferences insertablePreferences = mapper.transform(migratablePreferences);
        prefs.insertUserPreferences(insertablePreferences);
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
                + ResponseColumns.SURVEY_INSTANCE_ID + ", " + ResponseColumns.QUESTION_ID
                + ")");
        db.execSQL("CREATE INDEX record_name_idx ON " + Tables.RECORD
                + "(" + RecordColumns.NAME + ")");
        db.execSQL("CREATE INDEX response_status_idx ON " + Tables.SURVEY_INSTANCE
                + "(" + SurveyInstanceColumns.STATUS + ")");
        db.execSQL("CREATE INDEX response_modified_idx ON " + Tables.SURVEY_INSTANCE
                + "(" + SurveyInstanceColumns.SUBMITTED_DATE + ")");
    }

    /**
     * returns the value of a single setting identified by the key passed in
     */
    @Nullable
    public String findPreference(SQLiteDatabase db, String key) {
        return preferenceHandler.findPreference(db, key);
    }

    /**
     * persists setting to the db
     */
    public void savePreference(SQLiteDatabase db, String key, String value) {
        preferenceHandler.savePreference(db, key, value);
    }
}
