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

package com.gallatinsystems.survey.device.parser.json;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.gallatinsystems.survey.device.domain.SurveyInstance;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.parser.FlowParser;

public class SurveyedLocaleParser implements FlowParser<SurveyedLocale> {
    private static final String TAG = SurveyedLocaleParser.class.getSimpleName();

    @Override
    public SurveyedLocale parse(InputStream inputStream) {
        return null;
    }

    @Override
    public SurveyedLocale parse(String response) {
        return null;
    }

    @Override
    public List<SurveyedLocale> parseList(String response) {
        List<SurveyedLocale> surveyedLocales = new ArrayList<SurveyedLocale>();
        try {
            JSONObject jResponse = new JSONObject(response);
            JSONArray jSurveyedLocales = jResponse.getJSONArray(Attrs.SURVEYED_LOCALE_DATA);
            for (int i=0; i<jSurveyedLocales.length(); i++) {
                JSONObject jSurveyedLocale = jSurveyedLocales.getJSONObject(i);
                SurveyedLocale surveyedLocale = parse(jSurveyedLocale);
                if (surveyedLocale != null) {
                    surveyedLocales.add(surveyedLocale);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        
        return surveyedLocales;
    }

    public SurveyedLocale parse(JSONObject jSurveyedLocale) {
        try {
            String id = jSurveyedLocale.getString(Attrs.ID);
            int surveyGroupId = jSurveyedLocale.getInt(Attrs.SURVEY_GROUP_ID);
            String name = "Unknown";// TODO
            double latitude = 0.0d;
            double longitude = 0.0d;
            
            if (jSurveyedLocale.has(Attrs.LATITUDE)) {
                latitude = jSurveyedLocale.optDouble(Attrs.LATITUDE);
            }
            if (jSurveyedLocale.has(Attrs.LONGITUDE)) {
                longitude = jSurveyedLocale.optDouble(Attrs.LONGITUDE);
            }
            
            JSONArray jSurveyInstances = jSurveyedLocale.getJSONArray(Attrs.SURVEY_INSTANCES);
            List<SurveyInstance> surveyInstances = new SurveyInstanceParser().parseList(jSurveyInstances);
            
            SurveyedLocale surveyedLocale = new SurveyedLocale(id, name, surveyGroupId, latitude, longitude);
            surveyedLocale.setSurveyInstances(surveyInstances);
            
            return surveyedLocale;
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
    
    public int getSurveyedLocaleCount(String response) {
        try {
            JSONObject jResponse = new JSONObject(response);
            return jResponse.getInt(Attrs.SURVEYED_LOCALE_COUNT);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return 0;
        }
    }
    
    interface Attrs {
        // Main response
        String SURVEYED_LOCALE_COUNT = "surveyedLocaleCount";
        String SURVEYED_LOCALE_DATA = "surveyedLocaleData";
        
        // SurveyedLocale
        String ID               = "id";
        String SURVEY_GROUP_ID  = "surveyGroupId";
        String NAME             = "name";
        String LATITUDE         = "lat";
        String LONGITUDE        = "lon";
        String SURVEY_INSTANCES = "surveyInstances";
    }

}
