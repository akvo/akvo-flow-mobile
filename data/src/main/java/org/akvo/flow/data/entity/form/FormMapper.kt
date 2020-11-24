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

    fun mapForms(cursor: Cursor?): List<DataForm> {
        val forms = mutableListOf<DataForm>()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val id = getIntColumnValue(cursor, SurveyColumns._ID)
                val formId = getStringColumnValue(cursor, SurveyColumns.SURVEY_ID)
                val surveyId = getIntColumnValue(cursor, SurveyColumns.SURVEY_GROUP_ID)
                val formVersion = getDoubleColumnValue(cursor, SurveyColumns.VERSION)
                val name = getStringColumnValue(cursor, SurveyColumns.NAME)
                val type = getStringColumnValue(cursor, SurveyColumns.TYPE)
                val location = getStringColumnValue(cursor, SurveyColumns.LOCATION)
                val filename = getStringColumnValue(cursor, SurveyColumns.FILENAME)
                val language = getStringColumnValue(cursor, SurveyColumns.LANGUAGE)
                val resourcesDownloaded = getIntColumnValue(cursor, SurveyColumns.HELP_DOWNLOADED) == 1
                val deleted = getIntColumnValue(cursor, SurveyColumns.DELETED) == 1
                val dataForm = DataForm(
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
                forms.add(dataForm)
            } while (cursor.moveToNext())
        }
        cursor?.close()
        return forms
    }


    fun mapForm(cursor: Cursor?): DataForm {
        var resourcesDownloaded = false
        var formVersion = 0.0
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
            formVersion = cursor.getDouble(cursor.getColumnIndexOrThrow(SurveyColumns.VERSION))
            name = getStringColumnValue(cursor, SurveyColumns.NAME)
            type = getStringColumnValue(cursor, SurveyColumns.TYPE)
            location = getStringColumnValue(cursor, SurveyColumns.LOCATION)
            filename = getStringColumnValue(cursor, SurveyColumns.FILENAME)
            language = getStringColumnValue(cursor, SurveyColumns.LANGUAGE)
            resourcesDownloaded = getIntColumnValue(cursor, SurveyColumns.HELP_DOWNLOADED) == 1
            deleted = getIntColumnValue(cursor, SurveyColumns.DELETED) == 1
        }
        val dataForm = DataForm(
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
        cursor?.close()
        return dataForm
    }

    private fun getDoubleColumnValue(cursor: Cursor, columnName: String) =
        cursor.getDouble(cursor.getColumnIndexOrThrow(columnName))

    private fun getStringColumnValue(cursor: Cursor, columnName: String) =
        cursor.getString(cursor.getColumnIndexOrThrow(columnName))

    private fun getIntColumnValue(cursor: Cursor, columnName: String) =
        cursor.getInt(cursor.getColumnIndexOrThrow(columnName))
}
