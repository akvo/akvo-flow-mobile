/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.data.entity;

import org.akvo.flow.data.entity.form.FormHeaderParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

@RunWith(PowerMockRunner.class)
public class FormHeaderParserTest {

    @Test
    public void parseMultipleShouldReturnEmptyIfNullResponse() {
        FormHeaderParser formHeaderParser = new FormHeaderParser();

        final List<ApiFormHeader> results = formHeaderParser.parseMultiple(null);

        assertTrue(results.isEmpty());
    }

    @Test
    public void parseMultipleShouldReturnEmptyIfEmptyResponse() {
        FormHeaderParser formHeaderParser = new FormHeaderParser();

        final List<ApiFormHeader> results = formHeaderParser.parseMultiple("");

        assertTrue(results.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseMultipleShouldThrowExceptionIfInvalidResponseLength() {
        FormHeaderParser formHeaderParser = new FormHeaderParser();

        formHeaderParser.parseMultiple("abc,cde");
    }

    @Test
    public void parseMultipleShouldParseCorrectResponse() {
        FormHeaderParser formHeaderParser = new FormHeaderParser();

        final List<ApiFormHeader> results = formHeaderParser.parseMultiple(",1,cde,abc,cde,6.0,cde,true,33");

        assertFalse(results.isEmpty());
    }

    @Test
    public void parseOneShouldReturnCorrectValuesIfStartsWithComa() {
        FormHeaderParser formHeaderParser = new FormHeaderParser();

        ApiFormHeader result = formHeaderParser.parseOne(",1,cde,abc,cde,6.0,cde,true,33");

        assertNotNull(result);
        assertEquals("33", result.getRegistrationSurveyId());
    }

    @Test
    public void parseOneShouldReturnCorrectValuesIfStartsWithoutComa() {
        FormHeaderParser formHeaderParser = new FormHeaderParser();

        ApiFormHeader result = formHeaderParser.parseOne("1,cde,abc,cde,6.0,cde,true,33");

        assertNotNull(result);
        assertEquals("1", result.getId());
    }
}
