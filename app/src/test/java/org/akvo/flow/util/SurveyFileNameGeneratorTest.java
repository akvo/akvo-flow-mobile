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
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
public class SurveyFileNameGeneratorTest {

    @Test
    public void generateFileName_shouldReturnEmptyIfOnlySlash() throws Exception {
        SurveyFileNameGenerator fileNameGenerator = new SurveyFileNameGenerator();

        String fileName = fileNameGenerator.generateFileName("/");

        assertEquals("", fileName);
    }

    @Test
    public void generateFileName_shouldReturnCorrectSurveyName() throws Exception {
        SurveyFileNameGenerator fileNameGenerator = new SurveyFileNameGenerator();

        String fileName = fileNameGenerator.generateFileName("file.xml");
        assertEquals("file.xml", fileName);

        fileName = fileNameGenerator.generateFileName("folder/file.xml");
        assertEquals("file.xml", fileName);

        fileName = fileNameGenerator.generateFileName("folder/folder/file.xml");
        assertEquals("file.xml", fileName);
    }
}