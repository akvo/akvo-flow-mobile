/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.serialization.response;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.SurveyInstance;

public class SurveyInstanceParser {

    public SurveyInstance parse(JSONObject jSurveyInstance) throws JSONException {
        String uuid = jSurveyInstance.getString(Attrs.UUID);
        String surveyId = jSurveyInstance.getString(Attrs.SURVEY_ID);
        long date = jSurveyInstance.getLong(Attrs.DATE);
        String submitter = jSurveyInstance.optString(Attrs.SUBMITTER);

        JSONArray jQuestionResponses = jSurveyInstance.getJSONArray(Attrs.QUESTION_RESPONSE_LIST);
        List<QuestionResponse> responses = new QuestionResponseParser().parseList(jQuestionResponses);

        return new SurveyInstance(uuid, surveyId, submitter, date, responses);
    }

    public List<SurveyInstance> parseList(JSONArray jSurveyInstances) throws JSONException {
        List<SurveyInstance> surveyInstances = new ArrayList<SurveyInstance>();
        for (int i=0; i<jSurveyInstances.length(); i++) {
            JSONObject jSurveyInstance = jSurveyInstances.getJSONObject(i);
            SurveyInstance surveyInstance = parse(jSurveyInstance);
            if (surveyInstance != null) {
                surveyInstances.add(surveyInstance);
            }
        }

        return surveyInstances;
    }
    
    interface Attrs {
        String UUID                   = "uuid";
        String DATE                   = "collectionDate";
        String SURVEY_ID              = "surveyId";
        String QUESTION_RESPONSE_LIST = "qasList";
        String SUBMITTER              = "submitter";
    }

}
