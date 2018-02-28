/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.navigation;

import org.akvo.flow.domain.SurveyGroup;

import java.util.List;

public interface IFlowNavigationView {

    void notifySurveyDeleted(long surveyGroupId);

    void selectSurvey(SurveyGroup viewSurveyId);

    void displaySurveys(List<ViewSurvey> surveys, Long selectedSurveyId);

    void displayUsers(String selectedUserName, List<ViewUser> viewUsers);

    void onUserLongPress(ViewUser viewUser);

    void displayAddUser();

    void displaySurveyError();

    void displayUsersError();

    void displayErrorDeleteSurvey();

    void displayErrorSelectSurvey();

    void displayUserEditError();

    void displayUserDeleteError();

    void displayUserSelectError();

    void displayEditUser(ViewUser currentUser);
}
