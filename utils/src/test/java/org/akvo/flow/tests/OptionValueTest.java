/*
 * Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.tests;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import org.akvo.flow.utils.entity.Dependency;
import org.akvo.flow.utils.entity.Option;
import org.akvo.flow.utils.entity.OptionValue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OptionValueTest {

    @Test
    public void testSingle() {
        List<Option> options = new ArrayList<>();
        Option option = new Option("United Kingdom", "UK");
        options.add(option);

        String response = OptionValue.serialize(options);
        List<Option> values = OptionValue.deserialize(response);

        assertEquals(1, values.size());
        assertEquals("United Kingdom", values.get(0).getText());
        assertEquals("UK", values.get(0).getCode());
    }

    @Test
    public void testSingleOther() {
        List<Option> options = new ArrayList<>();
        Option option = new Option("United Kingdom", "OTHER", true);
        options.add(option);

        String response = OptionValue.serialize(options);
        List<Option> values = OptionValue.deserialize(response);

        assertEquals(1, values.size());
        assertEquals("United Kingdom", values.get(0).getText());
        assertEquals("OTHER", values.get(0).getCode());
        assertTrue(values.get(0).isOther());
    }

    @Test
    public void testSingleNoCodes() {
        List<Option> options = new ArrayList<>();
        Option option = new Option("United Kingdom", null, false, new HashMap<>());
        options.add(option);

        String response = OptionValue.serialize(options);
        List<Option> values = OptionValue.deserialize(response);

        assertEquals(1, values.size());
        assertEquals("United Kingdom", values.get(0).getText());
        assertNull(values.get(0).getCode());
    }

    @Test
    public void testMultiple() {
        List<Option> options = new ArrayList<>();
        Option option = new Option("United Kingdom", "UK");
        options.add(option);

        option = new Option("Spain", "ES");
        options.add(option);

        String response = OptionValue.serialize(options);

        List<Option> values = OptionValue.deserialize(response);

        assertEquals(2, values.size());
        assertEquals("United Kingdom", values.get(0).getText());
        assertEquals("UK", values.get(0).getCode());
        assertEquals("Spain", values.get(1).getText());
        assertEquals("ES", values.get(1).getCode());
    }

    @Test
    public void testDependencies() {
        List<Option> options = new ArrayList<>();
        Option option = new Option("United Kingdom", "UK");
        options.add(option);

        option = new Option("Spain", "ES");
        options.add(option);

        String response = OptionValue.serialize(options);

        Dependency dependency = new Dependency("123","Spain");
        assertTrue(dependency.isMatch(response));

        dependency.setAnswer("Spain|Finland");
        assertTrue(dependency.isMatch(response));

        dependency.setAnswer("Spain|United Kingdom");
        assertTrue(dependency.isMatch(response));

        dependency.setAnswer("Other");
        assertFalse(dependency.isMatch(response));
    }
}
