/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.domain.interactor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.akvo.flow.domain.entity.DownloadResult
import org.akvo.flow.domain.exception.AssignmentRequiredException
import org.akvo.flow.domain.repository.DataPointRepository
import org.akvo.flow.domain.util.ConnectivityStateManager
import timber.log.Timber
import javax.inject.Inject

class DownloadDataPoints @Inject constructor(
    private val dataPointRepository: DataPointRepository,
    private val connectivityStateManager: ConnectivityStateManager
) {
    suspend fun execute(parameters: Map<String, Any>): DownloadResult {
        if (parameters[KEY_SURVEY_ID] == null) {
           throw IllegalArgumentException("Missing surveyId")
        }
        return if (!connectivityStateManager.isConnectionAvailable) {
            DownloadResult(
                DownloadResult.ResultCode.ERROR_NO_NETWORK,
                0
            )
        } else {
            syncDataPoints(parameters)
        }
    }

    private suspend fun syncDataPoints(parameters: Map<String, Any>): DownloadResult {
        return withContext(Dispatchers.IO) {
            try {
                val downloadDataPoints =
                    dataPointRepository.downloadDataPoints((parameters[KEY_SURVEY_ID] as Long?)!!)
                DownloadResult(DownloadResult.ResultCode.SUCCESS, downloadDataPoints)
            } catch (ex: AssignmentRequiredException) {
                Timber.e(ex)
                DownloadResult(
                    DownloadResult.ResultCode.ERROR_ASSIGNMENT_MISSING,
                    0
                )
            } catch (ex: Exception) {
                Timber.e(ex)
                DownloadResult(
                    DownloadResult.ResultCode.ERROR_OTHER,
                    0
                )
            }
        }
    }

    companion object {
        const val KEY_SURVEY_ID = "survey_id"
    }
}
