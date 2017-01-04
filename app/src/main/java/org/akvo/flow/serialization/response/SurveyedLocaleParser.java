/*
 *  Copyright (C) 2013-2014 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.serialization.response;

import android.util.Log;

import org.akvo.flow.domain.SurveyInstance;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.domain.response.SurveyedLocalesResponse;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SurveyedLocaleParser {
    private static final String TAG = SurveyedLocaleParser.class.getSimpleName();

    public SurveyedLocalesResponse parseResponse(String response) {
        final List<SurveyedLocale> surveyedLocales = new ArrayList<SurveyedLocale>();
        String error = null;
        try {
            JSONObject jResponse = new JSONObject(response);
            JSONArray jSurveyedLocales = jResponse.getJSONArray(Attrs.SURVEYED_LOCALE_DATA);
            for (int i = 0; i < jSurveyedLocales.length(); i++) {
                JSONObject jSurveyedLocale = jSurveyedLocales.getJSONObject(i);
                SurveyedLocale surveyedLocale = parseSurveyedLocale(jSurveyedLocale);
                surveyedLocales.add(surveyedLocale);
            }
        } catch (JSONException e) {
            // Something went wrong in the parsing. We consider this invalid data,
            // and will stop the sync, to avoid storing corrupted data.
            PersistentUncaughtExceptionHandler.recordException(e);
            Log.e(TAG, e.getMessage(), e);
            error = "Invalid JSON response";
        }

        return new SurveyedLocalesResponse(surveyedLocales, error);
    }

    public SurveyedLocale parseSurveyedLocale(JSONObject jSurveyedLocale) throws JSONException {
        String id = jSurveyedLocale.getString(Attrs.ID);
        long lastModified = jSurveyedLocale.getLong(Attrs.LAST_MODIFIED);
        long surveyGroupId = jSurveyedLocale.getLong(Attrs.SURVEY_GROUP_ID);
        Double latitude =
                jSurveyedLocale.has(Attrs.LATITUDE) && !jSurveyedLocale.isNull(Attrs.LATITUDE) ?
                        jSurveyedLocale.getDouble(Attrs.LATITUDE) : null;
        Double longitude =
                jSurveyedLocale.has(Attrs.LONGITUDE) && !jSurveyedLocale.isNull(Attrs.LONGITUDE) ?
                        jSurveyedLocale.getDouble(Attrs.LONGITUDE) : null;

        String name = jSurveyedLocale.has(Attrs.NAME) && !jSurveyedLocale.isNull(Attrs.NAME) ?
                jSurveyedLocale.getString(Attrs.NAME) : null;

        JSONArray jSurveyInstances = jSurveyedLocale.getJSONArray(Attrs.SURVEY_INSTANCES);
        List<SurveyInstance> surveyInstances = new SurveyInstanceParser()
                .parseList(jSurveyInstances);

        SurveyedLocale surveyedLocale = new SurveyedLocale(id, name, lastModified, surveyGroupId,
                latitude, longitude);
        surveyedLocale.setSurveyInstances(surveyInstances);

        return surveyedLocale;
    }

    interface Attrs {
        // Main response
        String SURVEYED_LOCALE_DATA = "surveyedLocaleData";

        // SurveyedLocale
        String ID = "id";
        String SURVEY_GROUP_ID = "surveyGroupId";
        String NAME = "displayName";
        String LATITUDE = "lat";
        String LONGITUDE = "lon";
        String SURVEY_INSTANCES = "surveyInstances";
        String LAST_MODIFIED = "lastUpdateDateTime";
    }

}
