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

package org.akvo.flow.domain.interactor.forms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.akvo.flow.domain.entity.DomainForm
import org.akvo.flow.domain.repository.FormRepository
import javax.inject.Inject

class GetSurveyForms @Inject constructor(
    private val formRepository: FormRepository
) {

    suspend fun execute(parameters: Map<String, Any>): List<DomainForm> {
        if (!parameters.containsKey(PARAM_SURVEY_ID)) {
            throw IllegalArgumentException("Missing survey id")
        }
        return withContext(Dispatchers.IO) {
            try {
                val surveyId = parameters[PARAM_SURVEY_ID] as Long
                formRepository.getForms(surveyId)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    companion object {
        const val PARAM_SURVEY_ID = "survey_id"
    }
}
