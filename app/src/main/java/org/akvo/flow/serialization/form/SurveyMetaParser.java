/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation, either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.serialization.form;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Parser for Survey definitions (CSV). No question-answer pairs
 * will be returned.
 */
public class SurveyMetaParser {

    public Survey parse(String response) {
        String[] touple = response.split(",");
        if (touple.length < Attr.COUNT) {
            throw new IllegalArgumentException("Survey list response is in an unrecognized format");
        }
        Survey survey = new Survey();
        survey.setId(touple[Attr.ID]);
        survey.setName(touple[Attr.NAME]);
        survey.setLanguage(touple[Attr.LANGUAGE]);
        survey.setVersion(Double.parseDouble(touple[Attr.VERSION]));

        // Parse the SurveyGroup
        long groupId = Long.parseLong(touple[Attr.GROUP_ID]);
        String groupName = touple[Attr.GROUP_NAME];
        boolean monitored = Boolean.valueOf(touple[Attr.GROUP_MONITORED]);
        String registerSurveyId = touple[Attr.GROUP_REGISTRATION_SURVEY];

        // Assign registration form id, if missing.
        if (TextUtils.isEmpty(registerSurveyId) || "null".equalsIgnoreCase(registerSurveyId)) {
            registerSurveyId = survey.getId();
        }

        SurveyGroup group = new SurveyGroup(groupId, groupName, registerSurveyId, monitored);

        survey.setSurveyGroup(group);

        survey.setType(ConstantUtil.FILE_SURVEY_LOCATION_TYPE);
        return survey;
    }

    /**
     * Survey metadata feeds might contain no phone, thus we will
     * need to prepend the rows with a fake comma to ensure consistency.
     *
     * @param response
     * @param addColumn
     * @return survey list
     */
    @NonNull
    public List<Survey> parseList(String response, boolean addColumn) {
        List<Survey> surveyList = new ArrayList<>();
        StringTokenizer strTok = new StringTokenizer(response, "\n");
        while (strTok.hasMoreTokens()) {
            String currentLine = strTok.nextToken();
            if (addColumn) {
                // Add a fake column
                currentLine = "," + currentLine;
            }
            Survey survey = parse(currentLine);
            if (survey != null) {
                surveyList.add(survey);
            }
        }

        return surveyList;
    }

    public List<Survey> parseList(String response) {
        return parseList(response, false);
    }

    interface Attr {
        int DEVICE = 0;// Unused attribute. Should not be sent
        int ID = 1;
        int NAME = 2;
        int LANGUAGE = 3;
        int VERSION = 4;

        // SurveyGroup information
        int GROUP_ID = 5;
        int GROUP_NAME = 6;
        int GROUP_MONITORED = 7;
        int GROUP_REGISTRATION_SURVEY = 8;

        int COUNT = 9;// Length of column array
    }

}
