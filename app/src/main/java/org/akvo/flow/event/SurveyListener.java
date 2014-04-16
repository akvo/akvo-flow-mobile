package org.akvo.flow.event;

import org.akvo.flow.domain.QuestionGroup;

import java.util.List;

public interface SurveyListener {
    public List<QuestionGroup> getQuestionGroups();
    public long getSurveyInstanceId();
    public String getDefaultLanguage();
    public String[] getLanguages();
    public boolean isReadOnly();

    public void onSurveySubmit();
}
