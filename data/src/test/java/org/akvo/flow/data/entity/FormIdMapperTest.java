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
 *
 */

package org.akvo.flow.data.entity;

import android.database.Cursor;

import org.akvo.flow.data.entity.form.FormIdMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class FormIdMapperTest {

    @Mock
    Cursor mockCursor;

    @Test
    public void mapToFormIdShouldReturnEmptyIfNullCursor() {
        FormIdMapper mapper = new FormIdMapper();

        List<String> mapped = mapper.mapToFormId(null);

        assertTrue(mapped.isEmpty());
    }

    @Test
    public void mapToFormIdShouldReturnEmptyIfEmptyCursor() {
        FormIdMapper mapper = new FormIdMapper();
        when(mockCursor.getCount()).thenReturn(0);
        when(mockCursor.moveToFirst()).thenReturn(false);
        doNothing().when(mockCursor).close();

        List<String> mapped = mapper.mapToFormId(mockCursor);

        verify(mockCursor, times(1)).getCount();
        verify(mockCursor, times(1)).moveToFirst();
        verify(mockCursor, times(1)).close();

        assertTrue(mapped.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapToFormIdShouldFailIfMissingColumn() {
        FormIdMapper mapper = new FormIdMapper();
        when(mockCursor.getCount()).thenReturn(1);
        when(mockCursor.moveToFirst()).thenReturn(true, false);
        doNothing().when(mockCursor).close();
        doThrow(IllegalArgumentException.class).when(mockCursor).getColumnIndexOrThrow(anyString());

        mapper.mapToFormId(mockCursor);

        verify(mockCursor, times(1)).getCount();
        verify(mockCursor, times(1)).moveToFirst();
        verify(mockCursor, times(1)).close();
    }

    @Test
    public void mapToFormIdShouldParseCorrectItem() {
        FormIdMapper mapper = new FormIdMapper();
        when(mockCursor.getCount()).thenReturn(1);
        when(mockCursor.moveToFirst()).thenReturn(true, false);
        when(mockCursor.getColumnIndexOrThrow(anyString())).thenReturn(0);
        when(mockCursor.getString(0)).thenReturn("123");
        doNothing().when(mockCursor).close();

        List<String> mapped = mapper.mapToFormId(mockCursor);

        assertEquals("123", mapped.get(0));
    }
}
