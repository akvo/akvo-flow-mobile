/*
 * Copyright (C) 2018,2021 Stichting Akvo (Akvo Foundation)
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
 *
 */
package org.akvo.flow.presentation.form

import org.akvo.flow.domain.QuestionResponse
import org.akvo.flow.domain.Survey
import java.util.HashMap

interface FormView {
    fun showLoading()
    fun hideLoading()
    fun dismiss()
    fun showErrorExport()
    fun showMobileUploadSetting(surveyInstanceId: Long)
    fun startSync(isMobileSyncAllowed: Boolean)
    fun goToListOfForms()
    fun showFormUpdated()
    fun trackDraftFormVersionUpdated()
    fun showErrorLoadingForm()
    fun displayFormAndResponses(form: Survey, responses: HashMap<String, QuestionResponse>)
}
