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

import androidx.annotation.VisibleForTesting
import org.akvo.flow.BuildConfig
import org.akvo.flow.data.database.SurveyDbDataSource
import org.akvo.flow.domain.Survey
import org.akvo.flow.util.ConstantUtil
import timber.log.Timber
import java.io.File
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject

class BootstrapProcessor @Inject constructor(

    private val databaseAdapter: SurveyDbDataSource,
    private val surveyMapper: SurveyMapper,
    private val fileProcessor: FileProcessor
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
        return try {
            fileProcessor.extract(zipFile, entry)
            ProcessingResult.ProcessingSuccess
        } catch (e: Exception) {
            Timber.e(e)
            ProcessingResult.ProcessingError
        }
    }

    @VisibleForTesting
    fun processSurveyFile(zipFile: ZipFile, entry: ZipEntry, entryName: String): ProcessingResult {
        try {
            val filename = surveyMapper.generateFileName(entryName)
            val idFromFolderName = surveyMapper.getSurveyIdFromFilePath(entryName)
            val surveyFolderName = surveyMapper.generateSurveyFolderName(entryName)

            // in both cases (new survey and existing), we need to update the xml
            val surveyFile: File = fileProcessor.createAndCopyNewSurveyFile(filename, surveyFolderName, zipFile, entry)
            // now read the survey XML back into memory to see if there is a version
            val surveyMetadata = fileProcessor.readBasicSurveyData(surveyFile)

            if (surveyMetadata.alias.isEmpty() || !BuildConfig.INSTANCE_URL.contains(surveyMetadata.alias)) {
                    return ProcessingResult.ProcessingErrorWrongDashboard
            }
            val survey = surveyMapper.createOrUpdateSurvey(filename,
                idFromFolderName,
                databaseAdapter.getSurvey(idFromFolderName),
                surveyFolderName,
                surveyMetadata)

            // Save the Survey, SurveyGroup, and languages.
            updateSurveyStorage(survey)
            return ProcessingResult.ProcessingSuccess
        } catch (e: Exception) {
            Timber.e(e)
            return ProcessingResult.ProcessingError
        }
    }

    private fun updateSurveyStorage(survey: Survey) {
        databaseAdapter.addSurveyGroup(survey.surveyGroup)
        databaseAdapter.saveSurvey(survey)
    }

    fun openDb() {
        databaseAdapter.open()
    }

    fun closeDb() {
        databaseAdapter.close()
    }
}
