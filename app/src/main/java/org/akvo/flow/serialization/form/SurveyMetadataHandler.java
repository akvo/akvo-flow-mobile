/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.serialization.form;

import org.akvo.flow.domain.SurveyMetadata;
import org.akvo.flow.domain.SurveyGroup;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SurveyMetadataHandler extends DefaultHandler {

    private static final String SURVEY = "survey";
    private static final String APP = "app";
    private static final String SURVEY_ID = "surveyId";
    private static final String NAME = "name";
    private static final String VERSION = "version";
    private static final String SURVEY_GROUP_ID = "surveyGroupId";
    private static final String SURVEY_GROUP_NAME = "surveyGroupName";
    private static final String REGISTRATION_SURVEY = "registrationSurvey";

    private final SurveyMetadata surveyMetadata = new SurveyMetadata();

    public SurveyMetadata getSurveyMetadata() {
        return surveyMetadata;
    }

    /**
     * read in the attributes of the new xml element and set the appropriate
     * values on the object(s) being hydrated.
     */
    public void startElement(String uri, String localName, String name,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, name, attributes);
        if (localName.equalsIgnoreCase(SURVEY)) {
            if (attributes.getValue(SURVEY_ID) != null) {
                surveyMetadata.setId(attributes.getValue(SURVEY_ID));
            }
            if (attributes.getValue(NAME) != null) {
                surveyMetadata.setName(attributes.getValue(NAME));
            }
            if (attributes.getValue(VERSION) != null) {
                surveyMetadata.setVersion(Double.parseDouble(attributes.getValue(VERSION)));
            }
            if (attributes.getValue(SURVEY_GROUP_ID) != null &&
                    attributes.getValue(SURVEY_GROUP_NAME) != null) {
                long surveyGroupId = Long.valueOf(attributes.getValue(SURVEY_GROUP_ID));
                String surveyGroupName = attributes.getValue(SURVEY_GROUP_NAME);
                String surveyGroupForm = attributes.getValue(REGISTRATION_SURVEY);
                surveyMetadata.setSurveyGroup(
                        new SurveyGroup(surveyGroupId, surveyGroupName, surveyGroupForm,
                                surveyGroupForm != null));
            }
            surveyMetadata.setApp(attributes.getValue(APP));
        }
    }

}
