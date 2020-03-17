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
import org.akvo.flow.database.Constants
import org.akvo.flow.database.LanguageTable
import javax.inject.Inject

class FormLanguagesMapper @Inject constructor() {

    fun transform(cursor: Cursor?): Set<String> {
        val languages = mutableSetOf<String>()
        cursor?.let { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    getLanguage(cursor)?.let { language ->
                        if (language.isNotEmpty()) {
                            languages.add(language)
                        }
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
        if (languages.isEmpty()) {
            //if nothing there, we add english
            languages.add("en")
        }
        return languages
    }

    private fun getLanguage(cursor: Cursor): String? {
        return cursor.getString(cursor.getColumnIndexOrThrow(LanguageTable.COLUMN_LANGUAGE_CODE))
    }
}
