/*
 * Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.database.tables

import android.database.sqlite.SQLiteDatabase

open class FormUpdateNotifiedTable {

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    companion object {
        const val TABLE_NAME = "form_version_notified"
        const val COLUMN_FORM_ID = "form_id"
        const val COLUMN_NEW_FORM_VERSION = "new_form_version"

        private const val _ID = "_id"
        private const val CREATE_TABLE =
            ("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_FORM_ID + " TEXT NOT NULL,"
                    + COLUMN_NEW_FORM_VERSION + " REAL NOT NULL, "
                    + "UNIQUE(" + COLUMN_FORM_ID + ") ON CONFLICT REPLACE)")
    }
}
