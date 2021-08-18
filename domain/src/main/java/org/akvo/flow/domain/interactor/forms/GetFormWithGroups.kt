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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.akvo.flow.domain.repository.FormRepository
import timber.log.Timber
import javax.inject.Inject

class GetFormWithGroups@Inject constructor(private val formRepository: FormRepository) {

    suspend fun execute(parameters: Map<String, Any>): FormResult {
        if (!parameters.containsKey(PARAM_FORM_ID)) {
            return FormResult.ParamError("Missing form id")
        }
        return withContext(Dispatchers.IO) {
            try {
                val domainForm =
                    formRepository.getFormWithGroups(parameters[PARAM_FORM_ID] as String)
                FormResult.Success(domainForm)
            } catch (e: Exception) {
                Timber.e(e)
                FormResult.GenericError
            }
        }
    }

    companion object {
        const val PARAM_FORM_ID = "form_id"
    }
}