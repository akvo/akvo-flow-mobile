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

package org.akvo.flow.domain.interactor.forms

import kotlinx.coroutines.withContext
import org.akvo.flow.domain.executor.CoroutineDispatcher
import org.akvo.flow.domain.repository.FormInstanceRepository
import timber.log.Timber
import javax.inject.Inject

class UpdateFormInstance @Inject constructor(
    private val formInstanceRepository: FormInstanceRepository,
    private val coroutineDispatcher: CoroutineDispatcher
) {

    suspend fun execute(parameters: Map<String, Any>): FormVersionUpdateResult {
        if (parameters[PARAM_FORM_INSTANCE_ID] == null|| parameters[PARAM_FORM_VERSION] == null) {
            throw IllegalArgumentException("Missing form instance id or form version")
        }
        return withContext(coroutineDispatcher.getDispatcher()) {
            try {
                val formInstanceId = parameters[PARAM_FORM_INSTANCE_ID] as Long
                val formVersion = parameters[PARAM_FORM_VERSION] as Double
                val updated: Long = formInstanceRepository.updateFormVersion(formInstanceId, formVersion)
                if (updated > 0) {
                    FormVersionUpdateResult.FormVersionUpdated
                } else {
                    FormVersionUpdateResult.FormVersionNotUpdated
                }
            } catch (e: Exception) {
                Timber.e(e)
                FormVersionUpdateResult.FormVersionNotUpdated
            }
        }
    }

    companion object {
        const val PARAM_FORM_INSTANCE_ID = "form_instance_id"
        const val PARAM_FORM_VERSION = "form_version"
    }

    sealed class FormVersionUpdateResult {
        object FormVersionUpdated: FormVersionUpdateResult()
        object FormVersionNotUpdated: FormVersionUpdateResult()
    }
}
