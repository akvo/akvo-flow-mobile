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

package org.akvo.flow.data.database;

import android.database.sqlite.SQLiteDatabase;

public class LanguageTable {

    public static final String TABLE_NAME = "survey_languages_preferences";
    public static final String COLUMN_SURVEY_ID = "survey_instance_id";
    public static final String COLUMN_LANGUAGE_CODE = "language_code";
    public static final String _ID = "_id";

    public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_SURVEY_ID + " INTEGER NOT NULL,"
            + COLUMN_LANGUAGE_CODE + " TEXT NOT NULL, "
            + "UNIQUE(" + COLUMN_SURVEY_ID + ", " + COLUMN_LANGUAGE_CODE + ") ON CONFLICT REPLACE)";

    public LanguageTable() {
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    public void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
