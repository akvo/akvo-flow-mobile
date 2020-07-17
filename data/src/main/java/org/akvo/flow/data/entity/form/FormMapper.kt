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

package org.akvo.flow.data.entity.form

import android.database.Cursor
import org.akvo.flow.database.SurveyColumns
import javax.inject.Inject

class FormMapper @Inject constructor() {

    fun mapForm(cursor: Cursor?): DataForm {
        var resourcesDownloaded = false
        var formVersion = ""
        var id = -1 //invalid form
        var formId = ""
        var surveyId = 0
        var name = ""
        var type = ""
        var location = ""
        var filename = ""
        var language = ""
        var deleted = false
        if (cursor != null && cursor.moveToFirst()) {
            id = getIntColumnValue(cursor, SurveyColumns._ID)
            formId = getStringColumnValue(cursor, SurveyColumns.SURVEY_ID)
            surveyId = getIntColumnValue(cursor, SurveyColumns.SURVEY_GROUP_ID)
            formVersion = getStringColumnValue(cursor, SurveyColumns.VERSION)
            name = getStringColumnValue(cursor, SurveyColumns.NAME)
            type = getStringColumnValue(cursor, SurveyColumns.NAME)
            location = getStringColumnValue(cursor, SurveyColumns.NAME)
            filename = getStringColumnValue(cursor, SurveyColumns.NAME)
            language = getStringColumnValue(cursor, SurveyColumns.NAME)
            resourcesDownloaded = getIntColumnValue(cursor, SurveyColumns.HELP_DOWNLOADED) == 1
            deleted = getIntColumnValue(cursor, SurveyColumns.DELETED) == 1
        }
        cursor?.close()
        return DataForm(
            id,
            formId,
            surveyId,
            name,
            formVersion,
            type,
            location,
            filename,
            language,
            resourcesDownloaded,
            deleted
        )
    }

    private fun getStringColumnValue(cursor: Cursor, columnName: String) =
        cursor.getString(cursor.getColumnIndexOrThrow(columnName))

    private fun getIntColumnValue(cursor: Cursor, columnName: String) =
        cursor.getInt(cursor.getColumnIndexOrThrow(columnName))
}
