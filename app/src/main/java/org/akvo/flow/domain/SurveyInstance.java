/*
 *  Copyright (C) 2012-2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

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
