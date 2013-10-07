package com.gallatinsystems.survey.device.domain;

import java.util.List;

public class SurveyInstance {
    private String mUuid;
    private String mSurveyId;
    private long mDate;
    private List<QuestionResponse> mResponses;
    
    public SurveyInstance(String uuid, String surveyId, long date, List<QuestionResponse> responses) {
        mUuid = uuid;
        mSurveyId = surveyId;
        mDate = date;
        mResponses = responses;
    }
    
    public String getUuid() {
        return mUuid;
    }
    
    public String getSurveyId() {
        return mSurveyId;
    }
    
    public long getDate() {
        return mDate;
    }
    
    public List<QuestionResponse> getResponses() {
        return mResponses;
    }

}
