/*
 * Copyright (C) 2020-2021 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.interactor.forms

import kotlinx.coroutines.withContext
import org.akvo.flow.domain.executor.CoroutineDispatcher
import org.akvo.flow.domain.repository.FormInstanceRepository
import javax.inject.Inject

class GetFormInstance @Inject constructor(
    private val formInstanceRepository: FormInstanceRepository,
    private val coroutineDispatcher: CoroutineDispatcher,
) {

    suspend fun execute(parameters: Map<String, Any>): GetFormInstanceResult {
        if (!parameters.containsKey(PARAM_FORM_ID) || !parameters.containsKey(PARAM_DATAPOINT_ID)) {
            throw IllegalArgumentException("Missing form id")
        }
        return withContext(coroutineDispatcher.getDispatcher()) {
            try {
                val formId = parameters[PARAM_FORM_ID] as String
                val datapointId = parameters[PARAM_DATAPOINT_ID] as String
                val result = formInstanceRepository.getFormInstance(formId, datapointId)
                if (result.first == -1L) {
                    GetFormInstanceResult.GetFormInstanceResultFailure
                } else {
                    GetFormInstanceResult.GetFormInstanceResultSuccess(result.first, result.second != 0)
                }
            } catch (e: Exception) {
                GetFormInstanceResult.GetFormInstanceResultFailure
            }
        }
    }

    companion object {
        const val PARAM_FORM_ID = "form_id"
        const val PARAM_DATAPOINT_ID = "datapoint_id"
    }

    sealed class GetFormInstanceResult {
        data class GetFormInstanceResultSuccess(val surveyInstanceId: Long, val readOnly: Boolean): GetFormInstanceResult()
        object  GetFormInstanceResultFailure: GetFormInstanceResult()
    }
}
