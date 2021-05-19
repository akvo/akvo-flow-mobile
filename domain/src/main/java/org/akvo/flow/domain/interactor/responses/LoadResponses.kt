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

package org.akvo.flow.domain.interactor.responses

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.akvo.flow.domain.repository.ResponseRepository
import timber.log.Timber
import javax.inject.Inject

class LoadResponses @Inject constructor(private val responseRepository: ResponseRepository) {

    suspend fun execute(parameters: Map<String, Any>): ResponsesResult {
        Timber.d("Started getting responses")
        if (!parameters.containsKey(PARAM_FORM_INSTANCE_ID)) {
            return ResponsesResult.ParamError("Missing form instance id")
        }
        return withContext(Dispatchers.IO) {
            try {
                val responses =
                    responseRepository.getResponses(parameters[PARAM_FORM_INSTANCE_ID] as Long)
                Timber.d("Ended getting responses")
                ResponsesResult.Success(responses)
            } catch (e: Exception) {
                Timber.e(e)
                ResponsesResult.GenericError
            }
        }
    }

    companion object {
        const val PARAM_FORM_INSTANCE_ID = "form_instance"
    }
}