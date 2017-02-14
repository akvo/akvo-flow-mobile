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

import android.text.TextUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TextUtils.class)
public class SurveyIdGeneratorTest {

    @Before
    public void setup() {
        PowerMockito.mockStatic(TextUtils.class);
        PowerMockito.when(TextUtils.isDigitsOnly(any(CharSequence.class)))
                .thenAnswer(new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock invocation) throws Throwable {
                        CharSequence str = (CharSequence) invocation.getArguments()[0];
                        final int len = str.length();
                        for (int cp, i = 0; i < len; i += Character.charCount(cp)) {
                            cp = Character.codePointAt(str, i);
                            if (!Character.isDigit(cp)) {
                                return false;
                            }
                        }
                        return true;
                    }
                });
    }

    @Test
    public void getSurveyIdFromFilePath_shouldReturnEmptyIfNullParts() throws Exception {
        SurveyIdGenerator surveyIdGenerator = new SurveyIdGenerator();

        String surveyId = surveyIdGenerator.getSurveyIdFromFilePath(null);
        assertEquals("", surveyId);
    }

    @Test
    public void getSurveyIdFromFilePath_shouldReturnEmptyIfFolderMissing() throws Exception {
        SurveyIdGenerator surveyIdGenerator = new SurveyIdGenerator();

        String surveyId = surveyIdGenerator.getSurveyIdFromFilePath("");
        assertEquals("", surveyId);
    }

    @Test
    public void getSurveyIdFromFilePath_shouldReturnCorrectIdFromFolder() throws Exception {
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
    public void getSurveyIdFromFilePath_shouldReturnUseClosestFolderNameIfNoId() throws Exception {
        SurveyIdGenerator surveyIdGenerator = new SurveyIdGenerator();

        String surveyId = surveyIdGenerator
                .getSurveyIdFromFilePath("abc/folder/survey.xml");
        assertEquals("folder", surveyId);
    }
}
