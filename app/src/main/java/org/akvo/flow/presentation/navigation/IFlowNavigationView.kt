/*
 * Copyright (C) 2017,2020 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.presentation.navigation

import org.akvo.flow.domain.SurveyGroup

interface IFlowNavigationView {
    fun notifySurveyDeleted(surveyGroupId: Long)
    fun selectSurvey(surveyGroup: SurveyGroup?)
    fun displaySurveys(surveys: List<ViewSurvey>, selectedSurveyId: Long?)
    fun displayUsers(selectedUserName: String?, viewUsers: List<ViewUser>?)
    fun onUserLongPress(viewUser: ViewUser?)
    fun displayAddUser()
    fun displaySurveyError()
    fun displayUsersError()
    fun displayErrorDeleteSurvey()
    fun displayErrorSelectSurvey()
    fun displayUserEditError()
    fun displayUserDeleteError()
    fun displayUserSelectError()
    fun displayEditUser(currentUser: ViewUser?)
}
