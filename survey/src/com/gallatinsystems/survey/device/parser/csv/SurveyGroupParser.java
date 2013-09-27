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

import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.parser.FlowParser;

public class SurveyGroupParser implements FlowParser<SurveyGroup> {

    @Override
    public SurveyGroup parse(String response) {
        String[] touple = response.split(",");
        int id = Integer.parseInt(touple[Attr.ID]);
        String name = touple[Attr.NAME];
        return new SurveyGroup(id, name);
    }

    @Override
    public List<SurveyGroup> parseList(String response) {
        List<SurveyGroup> surveyGroupList = new ArrayList<SurveyGroup>();
        StringTokenizer strTok = new StringTokenizer(response, "\n");
        while (strTok.hasMoreTokens()) {
            String currentLine = strTok.nextToken();
            SurveyGroup surveyGroup = parse(currentLine);
            surveyGroupList.add(surveyGroup);
        }
        
        return surveyGroupList;
    }

    interface Attr {
        int ID   = 0;
        int NAME = 1;
    }

}
