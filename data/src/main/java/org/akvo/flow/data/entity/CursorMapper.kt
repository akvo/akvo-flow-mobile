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

package org.akvo.flow.data.entity

import android.database.Cursor
import org.akvo.flow.database.tables.DataPointDownloadTable
import javax.inject.Inject

class CursorMapper @Inject constructor() {

    fun transform(cursor: Cursor?): String? {
        cursor?.let { c ->
            if (c.moveToFirst()) {
                return getCursor(c)
            }
            c.close()
        }
        return null
    }

    private fun getCursor(cursor: Cursor): String {
        return cursor.getString(cursor.getColumnIndexOrThrow(DataPointDownloadTable.COLUMN_CURSOR))
    }
}
