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

package org.akvo.flow.service.bootstrap

import android.text.TextUtils
import java.io.File
import javax.inject.Inject

class SurveyMapper @Inject constructor() {

    /**
     * Get the survey id from the survey xml folder
     * The structure can be either surveyId/ or surveyId/folder/
     *
     * @param entryName all the folder path excluding the actual fileName
     */
    fun getSurveyIdFromFilePath(entryName: String): String {
        val fileSeparatorPosition = entryName.lastIndexOf("/")
        val folderPath =
            if (fileSeparatorPosition <= 0) "" else entryName.substring(0, fileSeparatorPosition)
        val folders = if (folderPath.isEmpty()) emptyArray() else folderPath.split("/".toRegex())
            .toTypedArray()
        return if (folders.isEmpty()) {
            //missing folder
            ""
        } else {
            val lastItemIndex = folders.size - 1
            for (i in lastItemIndex downTo 0) {
                val folderName = folders[i]
                if (TextUtils.isDigitsOnly(folderName)) {
                    return folderName
                }
            }
            //if not found just return the lowest subfolder name
            folders[lastItemIndex]
        }
    }

    fun generateFileName(entryName: String): String {
        if (entryName.isEmpty()) {
            return ""
        }
        val fileSeparatorPosition = entryName.lastIndexOf("/")
        return if (fileSeparatorPosition < 0) entryName else entryName.substring(
            fileSeparatorPosition + 1)
    }

    fun generateSurveyFolderName(entryName: String): String {
        val entryPaths: Array<String> = entryName.split(File.separator.toRegex())
            .toTypedArray()
        return if (entryPaths.size < 2) "" else entryPaths[entryPaths.size - 2]
    }
}