/*
 * Copyright (C) 2026 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.database.upgrade

import android.database.sqlite.SQLiteDatabase
import org.akvo.flow.database.Tables
import org.akvo.flow.database.TransmissionColumns
import org.akvo.flow.database.TransmissionStatus

/**
 * Puts transmissions marked FORM_DELETED back in the queue.
 *
 * Until now any 404 while uploading set that status, including a 404 from the file
 * store, which knows nothing about forms. The s3 proxy answered every instance with
 * one between 14 September and 6 October 2026, so surveys whose forms were never
 * deleted were written off on the tablets that collected them, in a state no query
 * retries, exports or publishes.
 *
 * Re-queueing cannot lose anything: a form that really has been deleted gets a 404
 * from the processor on the next attempt and is marked again.
 */
class RequeueFormDeletedUpgrader(private val db: SQLiteDatabase) : DatabaseUpgrader {

    override fun upgrade() {
        db.execSQL(
            "UPDATE ${Tables.TRANSMISSION}" +
                " SET ${TransmissionColumns.STATUS} = ${TransmissionStatus.FAILED}" +
                " WHERE ${TransmissionColumns.STATUS} = ${TransmissionStatus.FORM_DELETED}"
        )
    }
}
