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

package org.akvo.flow.domain.languages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.akvo.flow.domain.repository.FormRepository
import org.akvo.flow.domain.repository.LanguagesRepository
import timber.log.Timber
import javax.inject.Inject

class LoadLanguages @Inject constructor(
    private val formRepository: FormRepository,
    private val languagesRepository: LanguagesRepository
) {

    suspend fun execute(parameters: Map<String, Any>): LanguageResult {
        if (!parameters.containsKey(PARAM_SURVEY_ID) || !parameters.containsKey(PARAM_FORM_ID)) {
            return LanguageResult.Error(IllegalArgumentException("Missing survey id or form id"))
        }
        return withContext(Dispatchers.IO) {
            try {
                val surveyId = parameters[PARAM_SURVEY_ID] as Long
                val formId = parameters[PARAM_FORM_ID] as String
                LanguageResult.Success(
                    languagesRepository.getSavedLanguages(surveyId),
                    formRepository.loadFormLanguages(formId)
                )
            } catch (e: Exception) {
                Timber.e(e)
                LanguageResult.Error(e)
            }
        }
    }

    companion object {
        const val PARAM_SURVEY_ID = "survey_id"
        const val PARAM_FORM_ID = "form_id"
    }

    sealed class LanguageResult {
        data class Success(val savedLanguages: Set<String>, val availableLanguages: Set<String>) :
            LanguageResult()

        data class Error(val exception: Exception) : LanguageResult()
    }
}
