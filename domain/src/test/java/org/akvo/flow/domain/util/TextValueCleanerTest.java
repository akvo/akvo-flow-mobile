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

package org.akvo.flow.domain.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class TextValueCleanerTest {

    @Test
    public void cleanValueShouldReplaceTabBySpace() {
        TextValueCleaner textValueCleaner = new TextValueCleaner();

        String cleanedValue = textValueCleaner
                .cleanVal(TextValueCleaner.TAB + "abc" + TextValueCleaner.TAB);

        assertEquals(" abc ", cleanedValue);
    }

    @Test
    public void cleanValueShouldReplaceComaBySpace() {
        TextValueCleaner textValueCleaner = new TextValueCleaner();

        String cleanedValue = textValueCleaner
                .cleanVal(TextValueCleaner.COMA + "abc" + TextValueCleaner.COMA);

        assertEquals(" abc ", cleanedValue);
    }

    @Test
    public void cleanValueShouldReplaceNewlineBySpace() {
        TextValueCleaner textValueCleaner = new TextValueCleaner();

        String cleanedValue = textValueCleaner
                .cleanVal(TextValueCleaner.NEWLINE + "abc" + TextValueCleaner.NEWLINE);

        assertEquals(" abc ", cleanedValue);
    }

    @Test
    public void cleanValueShouldReturnNullIfNullString() {
        TextValueCleaner textValueCleaner = new TextValueCleaner();

        String cleanedValue = textValueCleaner.cleanVal(null);

        assertNull(cleanedValue);
    }

    @Test
    public void cleanValueShouldReturnSameIfNothingToReplace() {
        TextValueCleaner textValueCleaner = new TextValueCleaner();

        String cleanedValue = textValueCleaner.cleanVal("abc");

        assertEquals("abc", cleanedValue);
    }

    @Test
    public void sanitizeValueShouldReturnSameIfNothingToReplace() {
        TextValueCleaner textValueCleaner = new TextValueCleaner();

        String cleanedValue = textValueCleaner.sanitizeValue("abc");

        assertEquals("abc", cleanedValue);
    }

    @Test
    public void sanitizeValueShouldReturnNullIfNullString() {
        TextValueCleaner textValueCleaner = new TextValueCleaner();

        String cleanedValue = textValueCleaner.sanitizeValue(null);

        assertNull(cleanedValue);
    }

    @Test
    public void sanitizeValueShouldTrimSpaces() {
        TextValueCleaner textValueCleaner = new TextValueCleaner();

        String cleanedValue = textValueCleaner.sanitizeValue(" abc ");

        assertEquals("abc", cleanedValue);
    }

    @Test
    public void sanitizeValueShouldReplaceNewLine() {
        TextValueCleaner textValueCleaner = new TextValueCleaner();

        String cleanedValue = textValueCleaner.sanitizeValue(
                TextValueCleaner.NEWLINE + "a" + TextValueCleaner.NEWLINE + "bc"
                        + TextValueCleaner.NEWLINE);

        assertEquals("a bc", cleanedValue);
    }

    @Test
    public void sanitizeValueShouldReplaceTab() {
        TextValueCleaner textValueCleaner = new TextValueCleaner();

        String cleanedValue = textValueCleaner.sanitizeValue(
                TextValueCleaner.TAB + "a" + TextValueCleaner.TAB + "bc" + TextValueCleaner.TAB);

        assertEquals("a bc", cleanedValue);
    }
}
