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

package org.akvo.flow.util;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class SurveyIdGeneratorTest {

    @Test
    public void getSurveyIdFromFilePath_shouldReturnEmptyIfFolderMissing() {
        SurveyIdGenerator surveyIdGenerator = new SurveyIdGenerator();

        String surveyId = surveyIdGenerator.getSurveyIdFromFilePath("file.xml");
        assertEquals("", surveyId);
    }

    @Test
    public void getSurveyIdFromFilePath_shouldReturnCorrectIdFromFolder() {
        SurveyIdGenerator surveyIdGenerator = new SurveyIdGenerator();

        String surveyId = surveyIdGenerator
                .getSurveyIdFromFilePath("123/file.xml");
        assertEquals("123", surveyId);

        surveyId = surveyIdGenerator
                .getSurveyIdFromFilePath("folder/123/file.xml");
        assertEquals("123", surveyId);

        surveyId = surveyIdGenerator
                .getSurveyIdFromFilePath("123/folder/file.xml");
        assertEquals("123", surveyId);

        surveyId = surveyIdGenerator
                .getSurveyIdFromFilePath("folder1/123/folder/file.xml");
        assertEquals("123", surveyId);

        surveyId = surveyIdGenerator
                .getSurveyIdFromFilePath("/");
        assertEquals("", surveyId);
    }

    @Test
    public void getSurveyIdFromFilePath_shouldReturnUseClosestFolderNameIfNoId() {
        SurveyIdGenerator surveyIdGenerator = new SurveyIdGenerator();

        String surveyId = surveyIdGenerator
                .getSurveyIdFromFilePath("abc/folder/survey.xml");
        assertEquals("folder", surveyId);
    }
}
