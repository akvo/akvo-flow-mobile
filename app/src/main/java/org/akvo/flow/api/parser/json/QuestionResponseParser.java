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

import android.util.Log;

import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.util.ConstantUtil;

public class QuestionResponseParser {
    private static final String TAG = QuestionResponseParser.class.getSimpleName();

    public QuestionResponse parse(JSONObject jSurveyedLocale) {
        try {
            String val = jSurveyedLocale.getString(Attrs.ANSWER);
            String questionId = jSurveyedLocale.getString(Attrs.QUESTION_ID);
            
            return new QuestionResponse(val, ConstantUtil.VALUE_RESPONSE_TYPE, questionId);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public List<QuestionResponse> parseList(JSONArray jResponses) {
        List<QuestionResponse> responses = new ArrayList<QuestionResponse>();
        try {
            for (int i=0; i<jResponses.length(); i++) {
                JSONObject jQResponse = jResponses.getJSONObject(i);
                QuestionResponse response = parse(jQResponse);
                if (response != null) {
                    responses.add(response);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        
        return responses;
    }
    
    interface Attrs {
        String QUESTION_ID = "q";
        String ANSWER      = "a";
    }

}
