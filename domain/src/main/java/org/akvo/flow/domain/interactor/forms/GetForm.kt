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
import org.akvo.flow.domain.entity.DomainForm
import org.akvo.flow.domain.executor.CoroutineDispatcher
import org.akvo.flow.domain.repository.FormRepository
import timber.log.Timber
import javax.inject.Inject

class GetForm @Inject constructor(
    private val formRepository: FormRepository,
    private val coroutineDispatcher: CoroutineDispatcher
) {

    suspend fun execute(parameters: Map<String, Any>): DomainForm? {
        if (!parameters.containsKey(PARAM_FORM_ID)) {
            throw IllegalArgumentException("Missing form id")
        }
        return withContext(coroutineDispatcher.getDispatcher()) {
            try {
                formRepository.getForm(parameters[PARAM_FORM_ID] as String)
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }
    }

    companion object {
        const val PARAM_FORM_ID = "form_id"
    }
}
