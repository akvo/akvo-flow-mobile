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
import android.util.Pair
import org.akvo.flow.database.SurveyColumns
import javax.inject.Inject

class FormMetadataMapper @Inject constructor() {

    fun mapForm(cursor: Cursor?): Pair<Boolean, String> {
        var resourcesDownloaded = false
        var formVersion = ""
        if (cursor != null && cursor.moveToFirst()) {
            resourcesDownloaded =
                cursor.getInt(cursor.getColumnIndexOrThrow(SurveyColumns.HELP_DOWNLOADED)) == 1
            formVersion = cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.VERSION))
        }
        cursor?.close()
        return Pair(resourcesDownloaded, formVersion)
    }
}
