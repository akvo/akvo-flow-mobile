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

open class QuestionGroupTable {

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    companion object {
        const val TABLE_NAME = "question_group"
        private const val COLUMN_ID = "_id"
        const val COLUMN_GROUP_ID = "group_id"
        const val COLUMN_HEADING = "heading"
        const val COLUMN_REPEATABLE = "repeatable"
        const val COLUMN_FORM_ID = "form_id"
        const val COLUMN_ORDER = "group_order"

        private const val CREATE_TABLE: String =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_GROUP_ID INTEGER DEFAULT -1, $COLUMN_HEADING TEXT NOT NULL, $COLUMN_REPEATABLE INTEGER DEFAULT 0, $COLUMN_FORM_ID TEXT NOT NULL, $COLUMN_ORDER INTEGER NOT NULL DEFAULT 0)"
    }
}
