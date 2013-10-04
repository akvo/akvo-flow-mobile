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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.parser.FlowParser;

public class SurveyGroupParser implements FlowParser<SurveyGroup> {

    @Override
    public SurveyGroup parse(String response) {
        String[] touple = response.split(",");
        if (touple.length < Attr.REGISTER_SURVEY + 1) {
            return null;// Wrong format...
        }
        int id = Integer.parseInt(touple[Attr.ID]);
        String name = touple[Attr.NAME];
        boolean monitored = touple.length > Attr.MONITORED ? 
                Boolean.valueOf(touple[Attr.MONITORED])
                : false;
        String registerSurveyId = touple[Attr.REGISTER_SURVEY];
        return new SurveyGroup(id, name, registerSurveyId, monitored);
    }

    @Override
    public List<SurveyGroup> parseList(String response) {
        List<SurveyGroup> surveyGroupList = new ArrayList<SurveyGroup>();
        StringTokenizer strTok = new StringTokenizer(response, "\n");
        while (strTok.hasMoreTokens()) {
            String currentLine = strTok.nextToken();
            SurveyGroup surveyGroup = parse(currentLine);
            if (surveyGroup != null) {
                surveyGroupList.add(surveyGroup);
            }
        }
        
        return surveyGroupList;
    }

    @Override
    public SurveyGroup parse(InputStream inputStream) {
        // Not implemented
        throw new RuntimeException("Method not implemented");
    }

    interface Attr {
        int ID              = 0;
        int NAME            = 1;
        int MONITORED       = 2;
        int REGISTER_SURVEY = 3;
    }

}
