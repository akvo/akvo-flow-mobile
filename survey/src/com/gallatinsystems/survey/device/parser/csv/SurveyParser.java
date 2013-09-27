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

package com.gallatinsystems.survey.device.parser.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.util.Log;

import com.gallatinsystems.survey.device.domain.Survey;
import com.gallatinsystems.survey.device.parser.FlowParser;
import com.gallatinsystems.survey.device.util.ConstantUtil;

public class SurveyParser implements FlowParser<Survey> {
    private static final String TAG = SurveyParser.class.getSimpleName();

    @Override
    public Survey parse(String response) {
        String[] touple = response.split(",");
        if (touple.length < Attr.COUNT) {
            Log.e(TAG, "Survey list response is in an unrecognized format");
            return null;
        } 
        Survey survey = new Survey();
        survey.setId(touple[Attr.ID]);
        survey.setName(touple[Attr.NAME]);
        survey.setLanguage(touple[Attr.LANGUAGE]);
        survey.setVersion(Double.parseDouble(touple[Attr.VERSION]));
        survey.setType(ConstantUtil.FILE_SURVEY_LOCATION_TYPE);
        return survey;
    }

    @Override
    public List<Survey> parseList(String response) {
        List<Survey> surveyList = new ArrayList<Survey>();
        StringTokenizer strTok = new StringTokenizer(response, "\n");
        while (strTok.hasMoreTokens()) {
            String currentLine = strTok.nextToken();
            Survey survey = parse(currentLine);
            if (survey != null) {
                surveyList.add(survey);
            }
        }
        
        return surveyList;
    }

    interface Attr {
        int DEVICE   = 0;// Unused attribute. Should not be sent
        int ID       = 1;
        int NAME     = 2;
        int LANGUAGE = 3;
        int VERSION  = 4;
        
        int COUNT    = 5;// Length of column array
    }

}
