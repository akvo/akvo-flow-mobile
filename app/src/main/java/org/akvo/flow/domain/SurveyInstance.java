package org.akvo.flow.domain;

import java.util.List;

public class SurveyInstance {
    private String mUuid;
    private String mSurveyId;
    private String mSubmitter;
    private long mDate;
    private List<QuestionResponse> mResponses;
    
    public SurveyInstance(String uuid, String surveyId, String submitter, long date,
            List<QuestionResponse> responses) {
        mUuid = uuid;
        mSurveyId = surveyId;
        mSubmitter = submitter;
        mDate = date;
        mResponses = responses;
    }
    
    public String getUuid() {
        return mUuid;
    }
    
    public String getSurveyId() {
        return mSurveyId;
    }

    public String getSubmitter() {
        return mSubmitter;
    }

    public long getDate() {
        return mDate;
    }
    
    public List<QuestionResponse> getResponses() {
        return mResponses;
    }

}
