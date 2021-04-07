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

package org.akvo.flow.domain.interactor.settings

import kotlinx.coroutines.withContext
import org.akvo.flow.domain.executor.CoroutineDispatcher
import org.akvo.flow.domain.repository.UserRepository
import timber.log.Timber
import javax.inject.Inject

class FormVersionUpdateNotified @Inject constructor(
    private val userRepository: UserRepository,
    private val coroutineDispatcher: CoroutineDispatcher,
) {

    suspend fun execute(parameters: Map<String, Any>): FormVersionNotifiedResult {
        if (parameters[PARAM_FORM_ID] == null || parameters[PARAM_FORM_VERSION] == null) {
            throw IllegalArgumentException("Missing form id or form version")
        }
        return withContext(coroutineDispatcher.getDispatcher()) {
            try {
                val formId = parameters[PARAM_FORM_ID] as String
                val formVersion = parameters[PARAM_FORM_VERSION] as Double
                val notified: Int = userRepository.formVersionUpdateNotified(formId, formVersion)
                if (notified > 0) {
                    FormVersionNotifiedResult.FormVersionNotified
                } else {
                    FormVersionNotifiedResult.FormVersionNotNotified
                }
            } catch (e: Exception) {
                Timber.e(e)
                FormVersionNotifiedResult.FormVersionNotNotified
            }
        }
    }

    companion object {
        const val PARAM_FORM_ID = "form_id"
        const val PARAM_FORM_VERSION = "form_version"
    }

    sealed class FormVersionNotifiedResult {
        object FormVersionNotified: FormVersionNotifiedResult()
        object FormVersionNotNotified: FormVersionNotifiedResult()
    }
}
