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

import android.database.Cursor;

import org.akvo.flow.database.ResponseColumns;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import androidx.core.util.Pair;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class ResponseMapperTest {

    @Mock
    Cursor mockCursor;

    private final ResponseMapper mapper = new ResponseMapper();

    @Before
    public void setUp() {
        when(mockCursor.getColumnIndexOrThrow(ResponseColumns.QUESTION_ID)).thenReturn(0);
        when(mockCursor.getColumnIndexOrThrow(ResponseColumns.ITERATION)).thenReturn(1);
    }

    @Test
    public void mapIdIterationShouldParseCorrectlyCompoundQuestionIdIteration() {
        when(mockCursor.getString(0)).thenReturn("abc|1");
        when(mockCursor.getInt(1)).thenReturn(0);

        Pair<String, Integer> mapped = mapper.mapIdIteration(mockCursor);

        assertEquals("abc", mapped.first);
        assertEquals(1, mapped.second.intValue());
    }

    @Test
    public void mapIdIterationShouldParseCorrectlyCorrectIterationField() {
        when(mockCursor.getString(0)).thenReturn("abc");
        when(mockCursor.getInt(1)).thenReturn(0);

        Pair<String, Integer> mapped = mapper.mapIdIteration(mockCursor);

        assertEquals("abc", mapped.first);
        assertEquals(0, mapped.second.intValue());
    }

    @Test
    public void mapIdIterationShouldParseCorrectlyCorrectIterationInvalid() {
        when(mockCursor.getString(0)).thenReturn("abc");
        when(mockCursor.getInt(1)).thenReturn(-1);

        Pair<String, Integer> mapped = mapper.mapIdIteration(mockCursor);

        assertEquals("abc", mapped.first);
        assertEquals(0, mapped.second.intValue());
    }
}
