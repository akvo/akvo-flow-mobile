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

package org.akvo.flow.database.migration

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.akvo.flow.database.SurveyColumns
import org.akvo.flow.database.tables.QuestionGroupTable
import org.akvo.flow.database.tables.Tables
import org.akvo.flow.utils.FileHelper
import org.akvo.flow.utils.XmlFormParser
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

class GroupsMigrationHelper {

    fun migrateGroups(db: SQLiteDatabase, context: Context) {
        val cursor =
            db.query(Tables.SURVEY, arrayOf(SurveyColumns._ID), null, null, null, null, null)
        val formIds = HashSet<String>()
        if (cursor != null && cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(QuestionGroupTable.COLUMN_FORM_ID)
            do {
                formIds.add(cursor.getString(columnIndex))
            } while (cursor.moveToNext())
        }
        cursor?.close()

        for (formId in formIds) {
            val formFolder: String = context.filesDir.absolutePath + File.separator + "forms"
            var fileInputStream: FileInputStream
            try {
                fileInputStream = FileInputStream(File(formFolder, "$formId.xml"))
                val form = XmlFormParser(FileHelper()).parseXmlForm(fileInputStream)
                //insert groups
                val groups = form.groups
                for (group in groups) {
                    val contentValues = ContentValues()
                    var groupId = group.groupId
                    if (groupId == null) {
                        groupId = -1L
                    }
                    contentValues.put(QuestionGroupTable.COLUMN_GROUP_ID, groupId)
                    contentValues.put(QuestionGroupTable.COLUMN_HEADING, group.heading)
                    contentValues.put(QuestionGroupTable.COLUMN_REPEATABLE, if (group.repeatable) 1 else 0)
                    contentValues.put(QuestionGroupTable.COLUMN_FORM_ID, group.formId)
                    contentValues.put(QuestionGroupTable.COLUMN_ORDER, group.order)
                    db.insert(QuestionGroupTable.TABLE_NAME, null, contentValues)
                    //TODO: when question table also add questions into table
                }
            } catch (e: FileNotFoundException) {
                Timber.e(e)
            }
        }
    }
}