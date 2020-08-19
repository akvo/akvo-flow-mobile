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
import org.akvo.flow.domain.repository.LanguagesRepository
import timber.log.Timber
import javax.inject.Inject

class SaveLanguages @Inject constructor(
    private val languagesRepository: LanguagesRepository
) {

    suspend fun execute(parameters: Map<String, Any>): SaveLanguageResult {
        if (!parameters.containsKey(PARAM_SURVEY_ID) || !parameters.containsKey(PARAM_LANGUAGES_LIST)) {
            return SaveLanguageResult.Error(IllegalArgumentException("Missing survey id or selected languages"))
        }
        val surveyId = parameters[PARAM_SURVEY_ID] as Long

        @Suppress("UNCHECKED_CAST")
        val languages = parameters[PARAM_LANGUAGES_LIST] as Set<String>
        return withContext(Dispatchers.IO) {
            try {
                languagesRepository.saveLanguages(surveyId, languages)
                SaveLanguageResult.Success
            } catch (e: Exception) {
                Timber.e(e)
                SaveLanguageResult.Error(e)
            }
        }
    }

    companion object {
        const val PARAM_SURVEY_ID = "survey_id"
        const val PARAM_LANGUAGES_LIST = "languages"
    }

    sealed class SaveLanguageResult {
        object Success : SaveLanguageResult()
        data class Error(val exception: Exception) : SaveLanguageResult()
    }
}
