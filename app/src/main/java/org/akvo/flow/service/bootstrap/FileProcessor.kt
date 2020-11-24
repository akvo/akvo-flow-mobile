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

import android.content.Context
import android.text.TextUtils
import org.akvo.flow.domain.SurveyMetadata
import org.akvo.flow.serialization.form.SurveyMetadataParser
import org.akvo.flow.util.FileUtil
import org.akvo.flow.util.files.FormFileBrowser
import org.akvo.flow.util.files.FormResourcesFileBrowser
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.inject.Inject

class FileProcessor @Inject constructor(
    private val formFileBrowser: FormFileBrowser,
    private val resourcesFileUtil: FormResourcesFileBrowser,
    private val applicationContext: Context
) {

    @Throws(FileNotFoundException::class)
    fun readBasicSurveyData(surveyFile: File): SurveyMetadata {
        val `in`: InputStream = FileInputStream(surveyFile)
        val parser = SurveyMetadataParser()
        return parser.parse(`in`)
    }

    fun createAndCopyNewSurveyFile(
        filename: String,
        surveyFolderName: String,
        zipFile: ZipFile,
        entry: ZipEntry
    ): File {
        val surveyFile = createNewSurveyFile(filename, surveyFolderName)
        FileUtil.copy(zipFile.getInputStream(entry), FileOutputStream(surveyFile))
        return surveyFile
    }

    fun extract(zipFile: ZipFile, entry: ZipEntry) {
        FileUtil.extract(
            ZipInputStream(zipFile.getInputStream(entry)),
            resourcesFileUtil.getExistingAppInternalFolder(applicationContext)
        )
    }

    private fun createNewSurveyFile(filename: String, surveyFolderName: String): File {
        val formsFolder = formFileBrowser.getExistingAppInternalFolder(applicationContext)
        return when {
            TextUtils.isEmpty(surveyFolderName) -> {
                File(formsFolder, filename)
            }
            else -> {
                val surveyFolder = File(formsFolder, surveyFolderName)
                if (!surveyFolder.exists()) {
                    surveyFolder.mkdir()
                }
                File(surveyFolder, filename)
            }
        }
    }
}
