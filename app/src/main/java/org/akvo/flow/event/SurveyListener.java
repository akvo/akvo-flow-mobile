package org.akvo.flow.event;

import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;

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
}
