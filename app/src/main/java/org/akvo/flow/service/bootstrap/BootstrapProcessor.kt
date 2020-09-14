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
import androidx.annotation.VisibleForTesting
import org.akvo.flow.BuildConfig
import org.akvo.flow.data.database.SurveyDbDataSource
import org.akvo.flow.domain.Survey
import org.akvo.flow.domain.SurveyMetadata
import org.akvo.flow.serialization.form.SurveyMetadataParser
import org.akvo.flow.util.ConstantUtil
import org.akvo.flow.util.FileUtil
import org.akvo.flow.util.SurveyFileNameGenerator
import org.akvo.flow.util.SurveyIdGenerator
import org.akvo.flow.util.files.FormFileBrowser
import org.akvo.flow.util.files.FormResourcesFileBrowser
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.inject.Inject

class BootstrapProcessor @Inject constructor(
    private val resourcesFileUtil: FormResourcesFileBrowser,
    private val applicationContext: Context,
    private val surveyFileNameGenerator: SurveyFileNameGenerator,
    private val surveyIdGenerator: SurveyIdGenerator,
    private val databaseAdapter: SurveyDbDataSource,
    private val formFileBrowser: FormFileBrowser
) {

    fun processZipFile(zipFile: ZipFile): ProcessingResult {
        val entries: Enumeration<out ZipEntry> = zipFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryName = entry.name ?: ""
            when {
                entryName.endsWith(ConstantUtil.CASCADE_RES_SUFFIX) -> {
                    val result = processCascadeResource(zipFile, entry)
                    if (result is ProcessingResult.ProcessingError) {
                        return result
                    }
                }
                entryName.endsWith(ConstantUtil.XML_SUFFIX) -> {
                    val result = processSurveyFile(zipFile, entry, entryName)
                    if (result is ProcessingResult.ProcessingError ||
                        result is ProcessingResult.ProcessingErrorWrongDashboard) {
                        return result
                    }
                }
            }
        }
        return ProcessingResult.ProcessingSuccess
    }

    @VisibleForTesting
    fun processCascadeResource(zipFile: ZipFile, entry: ZipEntry): ProcessingResult {
        try {
            FileUtil.extract(
                ZipInputStream(zipFile.getInputStream(entry)),
                resourcesFileUtil.getExistingAppInternalFolder(applicationContext)
            )
            return ProcessingResult.ProcessingSuccess
        } catch (e: Exception) {
            Timber.e(e)
            return ProcessingResult.ProcessingError
        }
    }

    @VisibleForTesting
    fun processSurveyFile(zipFile: ZipFile, entry: ZipEntry, entryName: String): ProcessingResult {
        try {
            val filename = surveyFileNameGenerator.generateFileName(entryName)
            val idFromFolderName = surveyIdGenerator.getSurveyIdFromFilePath(entryName)
            var survey = databaseAdapter.getSurvey(idFromFolderName)
            val surveyFolderName: String = generateSurveyFolder(entry)

            // in both cases (new survey and existing), we need to update the xml
            val surveyFile = generateNewSurveyFile(filename, surveyFolderName)
            FileUtil.copy(zipFile.getInputStream(entry), FileOutputStream(surveyFile))
            // now read the survey XML back into memory to see if there is a version
            val surveyMetadata = readBasicSurveyData(surveyFile)
            if (surveyMetadata.app.isNullOrBlank() || !BuildConfig.SERVER_BASE.contains(surveyMetadata.app)) {
                return ProcessingResult.ProcessingErrorWrongDashboard
            }
            survey =
                updateSurvey(filename, idFromFolderName, survey, surveyFolderName, surveyMetadata)

            // Save the Survey, SurveyGroup, and languages.
            updateSurveyStorage(survey)
            return ProcessingResult.ProcessingSuccess
        } catch (e: Exception) {
            Timber.e(e)
            return ProcessingResult.ProcessingError
        }
    }

    @Throws(FileNotFoundException::class)
    private fun readBasicSurveyData(surveyFile: File): SurveyMetadata {
        val `in`: InputStream = FileInputStream(surveyFile)
        val parser = SurveyMetadataParser()
        return parser.parse(`in`)
    }

    private fun updateSurvey(
        filename: String, idFromFolderName: String,
        survey: Survey?, surveyFolderName: String,
        surveyMetadata: SurveyMetadata
    ): Survey {
        var survey = survey
        var surveyName = filename
        if (surveyName.contains(ConstantUtil.DOT_SEPARATOR)) {
            surveyName = surveyName.substring(0, surveyName.indexOf(ConstantUtil.DOT_SEPARATOR))
        }
        if (survey == null) {
            survey = createSurvey(idFromFolderName, surveyMetadata, surveyName)
        }
        survey.location = ConstantUtil.FILE_LOCATION
        val surveyFileName = generateSurveyFileName(filename, surveyFolderName)
        survey.fileName = surveyFileName
        if (!TextUtils.isEmpty(surveyMetadata.name)) {
            survey.name = surveyMetadata.name
        }
        survey.surveyGroup = surveyMetadata.surveyGroup
        if (surveyMetadata.version > 0) {
            survey.version = surveyMetadata.version
        } else {
            survey.version = 1.0
        }
        return survey
    }

    private fun createSurvey(id: String, surveyMetadata: SurveyMetadata, name: String): Survey {
        val survey = Survey()
        if (!TextUtils.isEmpty(surveyMetadata.id)) {
            survey.id = surveyMetadata.id
        } else {
            survey.id = id
        }
        survey.name = name
        /*
         * Resources are always attached to the zip file
         */
        survey.isHelpDownloaded = true
        survey.type = ConstantUtil.SURVEY_TYPE
        return survey
    }

    private fun updateSurveyStorage(survey: Survey) {
        databaseAdapter.addSurveyGroup(survey.surveyGroup)
        databaseAdapter.saveSurvey(survey)
    }

    private fun generateNewSurveyFile(filename: String, surveyFolderName: String): File {
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

    private fun generateSurveyFileName(filename: String, surveyFolderName: String?): String {
        val sb = StringBuilder(20)
        if (!TextUtils.isEmpty(surveyFolderName)) {
            sb.append(surveyFolderName)
            sb.append(File.separator)
        }
        sb.append(filename)
        return sb.toString()
    }

    private fun generateSurveyFolder(entry: ZipEntry): String {
        val entryName = entry.name
        val entryPaths: Array<String> = entryName?.split(File.separator.toRegex())
            ?.toTypedArray()
            ?: emptyArray()
        return if (entryPaths.size < 2) "" else entryPaths[entryPaths.size - 2]
    }

    fun openDb() {
        databaseAdapter.open()
    }

    fun closeDb() {
        databaseAdapter.close()
    }

}