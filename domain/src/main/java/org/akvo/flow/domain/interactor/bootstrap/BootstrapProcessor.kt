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

package org.akvo.flow.domain.interactor.bootstrap

import kotlinx.coroutines.withContext
import org.akvo.flow.domain.exception.WrongDashboardError
import org.akvo.flow.domain.executor.CoroutineDispatcher
import org.akvo.flow.domain.repository.FormRepository
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class BootstrapProcessor @Inject constructor(
    private val formRepository: FormRepository,
    private val coroutineDispatcher: CoroutineDispatcher,
) {

    /**
     * Checks the bootstrap directory for unprocessed zip files. If they are
     * found, they're processed one at a time. If an error occurs, all
     * processing stops (subsequent zips won't be processed if there are
     * multiple zips in the directory) just in case data in a later zip depends
     * on the previous one being there.
     */
    suspend fun execute(
        zipFiles: List<File>,
        instanceUrl: String,
        awsBucket: String,
    ): ProcessingResult {
        return withContext(coroutineDispatcher.getDispatcher()) {
            var processingResult: ProcessingResult = ProcessingResult.ProcessingSuccess

            for (file in zipFiles) {
                try {
                    formRepository.processZipFile(file, instanceUrl, awsBucket)
                    file.renameTo(File(file.absolutePath + PROCESSED_OK_SUFFIX))
                } catch (e: Exception) {
                    Timber.e(e, "Bootstrap error")
                    file.renameTo(File(file.absolutePath + PROCESSED_ERROR_SUFFIX))

                    processingResult = if (e is WrongDashboardError) {
                        ProcessingResult.ProcessingErrorWrongDashboard
                    } else {
                        ProcessingResult.ProcessingError
                    }
                    break
                }
            }
            processingResult
        }
    }

    companion object {
        const val CASCADE_RES_SUFFIX = ".sqlite.zip"
        const val XML_SUFFIX = ".xml"
        const val PROCESSED_OK_SUFFIX = ".processed"
        const val PROCESSED_ERROR_SUFFIX = ".error"
    }
}
