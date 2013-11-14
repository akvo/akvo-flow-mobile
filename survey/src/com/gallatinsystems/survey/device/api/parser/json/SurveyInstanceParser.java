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

package com.gallatinsystems.survey.device.api.parser.json;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.gallatinsystems.survey.device.domain.QuestionResponse;
import com.gallatinsystems.survey.device.domain.SurveyInstance;

public class SurveyInstanceParser {
    private static final String TAG = SurveyInstanceParser.class.getSimpleName();

    public SurveyInstance parse(JSONObject jSurveyInstance) {
        try {
            String uuid = jSurveyInstance.getString(Attrs.UUID);
            String surveyId = jSurveyInstance.getString(Attrs.SURVEY_ID);
            long date = jSurveyInstance.getLong(Attrs.DATE);
            
            JSONArray jQuestionResponses = jSurveyInstance.getJSONArray(Attrs.QUESTION_RESPONSE_LIST);
            List<QuestionResponse> responses = new QuestionResponseParser().parseList(jQuestionResponses);
            
            return new SurveyInstance(uuid, surveyId, date, responses);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public List<SurveyInstance> parseList(JSONArray jSurveyInstances) {
        List<SurveyInstance> surveyInstances = new ArrayList<SurveyInstance>();
        try {
            for (int i=0; i<jSurveyInstances.length(); i++) {
                JSONObject jSurveyInstance = jSurveyInstances.getJSONObject(i);
                SurveyInstance surveyInstance = parse(jSurveyInstance);
                if (surveyInstance != null) {
                    surveyInstances.add(surveyInstance);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
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
