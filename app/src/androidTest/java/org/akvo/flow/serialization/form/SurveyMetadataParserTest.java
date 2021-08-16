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

import junit.framework.TestCase;

import org.akvo.flow.utils.entity.SurveyMetadata;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class SurveyMetadataParserTest extends TestCase {

    public void testGenerateXmlStream() {
        String xmlTest = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><survey name=\"New form\" defaultLanguageCode=\"en\" version='1.0' app=\"akvoflow-uat1\" surveyGroupId=\"104339127\" surveyGroupName=\"Bootstrap\" surveyId=\"107149135\"><questionGroup><heading>New group - please change name</heading><question order=\"1\" type=\"free\" mandatory=\"true\" localeNameFlag=\"false\" id=\"104319123\"><text>New question - please change name</text></question></questionGroup></survey>";
        InputStream inputStream = new ByteArrayInputStream(xmlTest.getBytes());
        SurveyMetadataParser surveyMetadataParser = new SurveyMetadataParser();
        SurveyMetadata surveyMetadata = surveyMetadataParser.parse(inputStream);
        assertNotNull(surveyMetadata);
        assertEquals("akvoflow-uat1", surveyMetadata.getApp());
        assertEquals("107149135", surveyMetadata.getId());
        assertEquals("New form", surveyMetadata.getName());
        assertEquals(104339127L, surveyMetadata.getSurveyGroup().getId());
        assertFalse(surveyMetadata.getSurveyGroup().isMonitored());
        assertEquals("Bootstrap", surveyMetadata.getSurveyGroup().getName());
        assertNull(surveyMetadata.getSurveyGroup().getRegisterSurveyId());
        assertEquals(1.0, surveyMetadata.getVersion());
    }
}
