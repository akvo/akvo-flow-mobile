/*
 * Copyright (C) 2017,2021 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.serialization.form

import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.domain.SurveyMetadata
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

class SurveyMetadataHandler : DefaultHandler() {

    val surveyMetadata = SurveyMetadata()

    /**
     * read in the attributes of the new xml element and set the appropriate
     * values on the object(s) being hydrated.
     */
    @Throws(SAXException::class)
    override fun startElement(
        uri: String, localName: String, name: String,
        attributes: Attributes
    ) {
        super.startElement(uri, localName, name, attributes)
        if (localName.equals(SURVEY, ignoreCase = true)) {
            if (attributes.getValue(SURVEY_ID) != null) {
                surveyMetadata.id = attributes.getValue(SURVEY_ID)
            }
            if (attributes.getValue(NAME) != null) {
                surveyMetadata.name = attributes.getValue(NAME)
            }
            if (attributes.getValue(VERSION) != null) {
                surveyMetadata.version = attributes.getValue(VERSION).toDouble()
            }
            if (attributes.getValue(SURVEY_GROUP_ID) != null &&
                attributes.getValue(SURVEY_GROUP_NAME) != null
            ) {
                val surveyGroupId = java.lang.Long.valueOf(attributes.getValue(SURVEY_GROUP_ID))
                val surveyGroupName = attributes.getValue(SURVEY_GROUP_NAME)
                val surveyGroupForm = attributes.getValue(REGISTRATION_SURVEY)
                surveyMetadata.surveyGroup =
                    SurveyGroup(surveyGroupId, surveyGroupName, surveyGroupForm,
                        surveyGroupForm != null)
            }
            surveyMetadata.app = attributes.getValue(APP)
            if (attributes.getValue(ALIAS) != null) {
                surveyMetadata.alias = attributes.getValue(ALIAS)
            }
        }
    }

    companion object {
        private const val SURVEY = "survey"
        private const val APP = "app"
        private const val SURVEY_ID = "surveyId"
        private const val NAME = "name"
        private const val VERSION = "version"
        private const val SURVEY_GROUP_ID = "surveyGroupId"
        private const val SURVEY_GROUP_NAME = "surveyGroupName"
        private const val REGISTRATION_SURVEY = "registrationSurvey"
        private const val ALIAS = "alias"
    }
}
