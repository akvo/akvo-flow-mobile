/*
 *  Copyright (C) 2013-2014 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.domain.SurveyInstance;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.domain.response.SurveyedLocalesResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class SurveyedLocaleParser {

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
            Timber.e(e, e.getMessage());
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
