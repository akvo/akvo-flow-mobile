/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.api.parser.json;

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

        JSONArray jQuestionResponses = jSurveyInstance.getJSONArray(Attrs.QUESTION_RESPONSE_LIST);
        List<QuestionResponse> responses = new QuestionResponseParser().parseList(jQuestionResponses);

        return new SurveyInstance(uuid, surveyId, date, responses);
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
    }

}
