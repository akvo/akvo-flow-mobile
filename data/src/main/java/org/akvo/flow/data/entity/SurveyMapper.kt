/*
 * Copyright (C) 2017,2020 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.data.entity

import android.database.Cursor
import org.akvo.flow.database.SurveyGroupColumns
import org.akvo.flow.domain.entity.DomainSurvey
import java.util.ArrayList
import javax.inject.Inject

class SurveyMapper @Inject constructor() {

    fun getSurveys(cursor: Cursor?): List<DomainSurvey> {
        val surveys: MutableList<DomainSurvey> = ArrayList()
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val survey = getSurvey(cursor)
                    surveys.add(survey)
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
        return surveys
    }

    private fun getSurvey(cursor: Cursor): DomainSurvey {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyGroupColumns.SURVEY_GROUP_ID))
        val name = getString(cursor, SurveyGroupColumns.NAME)
        val registerSurveyId = getString(cursor, SurveyGroupColumns.REGISTER_SURVEY_ID)
        val monitored = getBoolean(cursor, SurveyGroupColumns.MONITORED)
        val viewed = getBoolean(cursor, SurveyGroupColumns.VIEWED)
        return DomainSurvey(id, name, monitored, registerSurveyId, viewed)
    }

    private fun getBoolean(cursor: Cursor, columnName: String) =
        cursor.getInt(cursor.getColumnIndexOrThrow(columnName)) > 0

    private fun getString(cursor: Cursor, columnName: String) =
        cursor.getString(cursor.getColumnIndexOrThrow(columnName))
}
