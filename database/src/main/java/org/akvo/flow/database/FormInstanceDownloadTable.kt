/*
 * Copyright (C) 2020 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.database

import android.database.sqlite.SQLiteDatabase

open class FormInstanceDownloadTable {

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    companion object {
        const val TABLE_NAME = "form_instance_download"
        const val COLUMN_CURSOR = "cursor"
        const val COLUMN_SURVEY_ID = "datapoint_id"

        private const val _ID = "_id"
        private const val CREATE_TABLE =
                ("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                        + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + COLUMN_SURVEY_ID + " INTEGER NOT NULL,"
                        + COLUMN_CURSOR + " TEXT NOT NULL, "
                        + "UNIQUE(" + COLUMN_SURVEY_ID + ") ON CONFLICT REPLACE)")
    }
}
