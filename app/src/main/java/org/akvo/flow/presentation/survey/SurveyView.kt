/*
 * Copyright (C) 2018,2020,2021 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.presentation.survey

import org.akvo.flow.domain.entity.User
import org.akvo.flow.presentation.entity.ViewApkData

interface SurveyView {
    fun showNewVersionAvailable(apkData: ViewApkData?)
    fun showMissingUserError()
    fun openEmptyForm(user: User, formId: String?)
    fun showMissingFormError()
    fun showMissingCascadeError()
    fun displaySelectedUser(name: String)
    fun navigateToForm(
        datapointId: String,
        surveyInstanceId: Long,
        readOnly: Boolean,
        registrationFormId: String
    )
    fun displayRecord(datapointId: String)
    fun navigateToForm(formId: String, user: User, datapointId: String)
    fun enableClickListener()
}
