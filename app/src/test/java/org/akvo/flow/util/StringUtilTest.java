/*
 * Copyright (C) 2016-2017,2019 Stichting Akvo (Akvo Foundation)
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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class StringUtilTest {

    @Test
    public void isValid_ShouldReturnFalseIfNull() {
        boolean result = StringUtil.isValid(null);
        assertFalse(result);
    }

    @Test
    public void isValid_ShouldReturnFalseIfEmpty() {
        boolean result = StringUtil.isValid("");
        assertFalse(result);
    }

    @Test
    public void isValid_ShouldReturnFalseIfNullString() {
        boolean result = StringUtil.isValid("null");
        assertFalse(result);
    }

    @Test
    public void isValid_ShouldReturnTrueIfValid() {
        boolean result = StringUtil.isValid("value");
        assertTrue(result);
    }

    @Test
    public void isNullOrEmpty_ShouldReturnTrueIfNull() {
        boolean result = StringUtil.isNullOrEmpty(null);
        assertTrue(result);
    }

    @Test
    public void isNullOrEmpty_ShouldReturnTrueIfEmpty() {
        boolean result = StringUtil.isNullOrEmpty("");
        assertTrue(result);
    }

    @Test
    public void isNullOrEmpty_ShouldReturnTrueIfOnlySpaces() {
        boolean result = StringUtil.isNullOrEmpty("  ");
        assertTrue(result);
    }

    @Test
    public void isNullOrEmpty_ShouldReturnFalseIfNotEmpty() {
        boolean result = StringUtil.isNullOrEmpty(" hello ");
        assertFalse(result);
    }

    @Test
    public void controlToSpace_ShouldReturnEmptyIfNullValue() {
        String result = StringUtil.controlToSpace(null);
        assertEquals("", result);
    }

    @Test
    public void controlToSpace_ShouldReturnEmptyIfEmptyValue() {
        String result = StringUtil.controlToSpace("");
        assertEquals("", result);
    }

    @Test
    public void controlToSpace_ShouldReturnStringWithoutControlChars() {
        //testing with 2 line feeds
        String result = StringUtil.controlToSpace("" +'\u0010'+'\u0010');
        assertEquals("  ", result);
    }
}
