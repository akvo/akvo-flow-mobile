/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */
package org.akvo.flow.event;

import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.ui.view.QuestionView;

import java.util.List;
import java.util.Map;

public interface SurveyListener {
    public List<QuestionGroup> getQuestionGroups();
    public String getDefaultLanguage();
    public String[] getLanguages();
    public boolean isReadOnly();
    public void onSurveySubmit();
    public void nextTab();
    public void openQuestion(String questionId);
    public Map<String, QuestionResponse> getResponses();
    public void deleteResponse(String questionId);
    public QuestionView getQuestionView(String questionId);
}
