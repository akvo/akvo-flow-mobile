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

package org.akvo.flow.presentation.form.view.entity

import org.akvo.flow.domain.entity.DomainForm
import org.akvo.flow.domain.entity.Response
import javax.inject.Inject

class ViewFormMapper @Inject constructor(private val viewQuestionGroupMapper: ViewQuestionGroupMapper) {

    fun transform(domainForm: DomainForm, responses: List<Response>): ViewForm {
        return ViewForm(
            domainForm.name,
            domainForm.version,
            viewQuestionGroupMapper.transform(domainForm.groups, responses)
        )
    }
}
