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

import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.util.ConstantUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class QuestionResponseParser {

    public QuestionResponse parse(JSONObject jSurveyedLocale) {
        try {
            String val = jSurveyedLocale.getString(Attrs.ANSWER);
            String questionId = jSurveyedLocale.getString(Attrs.QUESTION_ID);
            
            return new QuestionResponse(val, ConstantUtil.VALUE_RESPONSE_TYPE, questionId);
        } catch (JSONException e) {
            Timber.e(e.getMessage());
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
            Timber.e(e.getMessage());
        }
        
        return responses;
    }
    
    interface Attrs {
        String QUESTION_ID = "q";
        String ANSWER      = "a";
    }

}
