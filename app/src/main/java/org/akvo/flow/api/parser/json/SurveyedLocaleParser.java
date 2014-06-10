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

import org.akvo.flow.api.response.SurveyedLocalesResponse;
import org.akvo.flow.domain.SurveyInstance;
import org.akvo.flow.domain.SurveyedLocale;

public class SurveyedLocaleParser {

    public SurveyedLocalesResponse parseResponse(String response) throws JSONException {
        List<SurveyedLocale> surveyedLocales = new ArrayList<SurveyedLocale>();
        JSONObject jResponse = new JSONObject(response);
        String syncTime = String.valueOf(jResponse.getLong(Attrs.SYNC_TIME));
        JSONArray jSurveyedLocales = jResponse.getJSONArray(Attrs.SURVEYED_LOCALE_DATA);
        for (int i=0; i<jSurveyedLocales.length(); i++) {
            JSONObject jSurveyedLocale = jSurveyedLocales.getJSONObject(i);
            SurveyedLocale surveyedLocale = parseSurveyedLocale(jSurveyedLocale);
            if (surveyedLocale != null) {
                surveyedLocales.add(surveyedLocale);
            }
        }

        return new SurveyedLocalesResponse(syncTime, surveyedLocales);
    }

    public SurveyedLocale parseSurveyedLocale(JSONObject jSurveyedLocale) throws JSONException {
        String id = jSurveyedLocale.getString(Attrs.ID);
        long surveyGroupId = jSurveyedLocale.getLong(Attrs.SURVEY_GROUP_ID);
        Double latitude = jSurveyedLocale.has(Attrs.LATITUDE) ?
                jSurveyedLocale.getDouble(Attrs.LATITUDE) : null;
        Double longitude = jSurveyedLocale.has(Attrs.LONGITUDE) ?
            jSurveyedLocale.getDouble(Attrs.LONGITUDE) : null;

        String name = jSurveyedLocale.optString(Attrs.NAME, "Unknown");

        JSONArray jSurveyInstances = jSurveyedLocale.getJSONArray(Attrs.SURVEY_INSTANCES);
        List<SurveyInstance> surveyInstances = new SurveyInstanceParser().parseList(jSurveyInstances);

        SurveyedLocale surveyedLocale = new SurveyedLocale(id, name, surveyGroupId, latitude, longitude);
        surveyedLocale.setSurveyInstances(surveyInstances);

        return surveyedLocale;
    }
    
    interface Attrs {
        // Main response
        String SYNC_TIME             = "lastUpdateTime";
        String SURVEYED_LOCALE_DATA  = "surveyedLocaleData";
        
        // SurveyedLocale
        String ID               = "id";
        String SURVEY_GROUP_ID  = "surveyGroupId";
        String NAME             = "displayName";
        String LATITUDE         = "lat";
        String LONGITUDE        = "lon";
        String SURVEY_INSTANCES = "surveyInstances";
    }

}
