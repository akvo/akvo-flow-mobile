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

package org.akvo.flow.data.entity.form

import android.database.Cursor
import org.akvo.flow.database.tables.QuestionGroupTable
import javax.inject.Inject

class QuestionGroupMapper @Inject constructor() {

    fun mapGroups(groupCursor: Cursor?): MutableList<DataQuestionGroup> {
        val groups = mutableListOf<DataQuestionGroup>()
        if (groupCursor != null && groupCursor.moveToFirst()) {
            do {
                val groupId = groupCursor.getLong(groupCursor.getColumnIndexOrThrow(QuestionGroupTable.COLUMN_GROUP_ID))
                val heading = groupCursor.getString(groupCursor.getColumnIndexOrThrow(QuestionGroupTable.COLUMN_HEADING))
                val repeatable = groupCursor.getInt(groupCursor.getColumnIndexOrThrow(QuestionGroupTable.COLUMN_REPEATABLE)) == 1
                val formId = groupCursor.getString(groupCursor.getColumnIndexOrThrow(QuestionGroupTable.COLUMN_FORM_ID))
                val order = groupCursor.getInt(groupCursor.getColumnIndexOrThrow(QuestionGroupTable.COLUMN_ORDER))
                groups.add(DataQuestionGroup(groupId, heading, repeatable, formId, order))
            } while (groupCursor.moveToNext())
        }
        groupCursor?.close()
        return groups
    }
}
