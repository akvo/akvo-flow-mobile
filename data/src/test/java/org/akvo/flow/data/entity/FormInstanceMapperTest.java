/*
 * Copyright (C) 2019-2020 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.data.util.FileHelper;
import org.akvo.flow.domain.entity.InstanceIdUuid;
import org.akvo.flow.domain.entity.Response;
import org.akvo.flow.domain.util.TextValueCleaner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;

import androidx.core.util.Pair;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FormInstanceMapperTest {

    @Mock
    TextValueCleaner mockTextValueCleaner;

    @Mock
    ResponseMapper mockResponseMapper;

    @Mock
    FileHelper mockFileHelper;

    @Mock
    Cursor mockCursor;

    @Mock
    Response mockResponse;

    private FormInstanceMapper mapper;

    @Before
    public void setUp() {
        doNothing().when(mockCursor).close();
        when(mockCursor.getColumnIndexOrThrow(anyString())).thenReturn(0);
        when(mockCursor.getString(anyInt())).thenReturn("uuid");
        when(mockCursor.getLong(anyInt())).thenReturn(1L);
        when(mockCursor.getDouble(anyInt())).thenReturn(1.0);

        mapper = new FormInstanceMapper(mockTextValueCleaner, mockResponseMapper, mockFileHelper);
    }

    @Test
    public void getInstanceIdUuidsShouldReturnEmptyIfNullCursor() {
        List<InstanceIdUuid> results = mapper.getInstanceIdUuids(null);

        assertTrue(results.isEmpty());
    }

    @Test
    public void getInstanceIdUuidsShouldReturnEmptyIfEmptyCursor() {
        mapper = new FormInstanceMapper(mockTextValueCleaner, mockResponseMapper, mockFileHelper);
        when(mockCursor.moveToFirst()).thenReturn(false);

        List<InstanceIdUuid> results = mapper.getInstanceIdUuids(mockCursor);

        assertTrue(results.isEmpty());
    }

    @Test
    public void getInstanceIdUuidsShouldReturnCorrectDataFromCursor() {
        mapper = new FormInstanceMapper(mockTextValueCleaner, mockResponseMapper, mockFileHelper);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getCount()).thenReturn(1);
        when(mockCursor.moveToNext()).thenReturn(false);

        List<InstanceIdUuid> results = mapper.getInstanceIdUuids(mockCursor);

        assertFalse(results.isEmpty());
        InstanceIdUuid instanceIdUuid = results.get(0);
        assertEquals(1L, instanceIdUuid.getId());
        assertEquals("uuid", instanceIdUuid.getUuid());
    }

    @Test
    public void getInstanceIdsShouldReturnEmptyIfNullCursor() {
        List<Long> results = mapper.getInstanceIds(null);

        assertTrue(results.isEmpty());
    }

    @Test
    public void getInstanceIdsShouldReturnEmptyIfEmptyCursor() {
        when(mockCursor.moveToFirst()).thenReturn(false);

        List<Long> results = mapper.getInstanceIds(mockCursor);

        assertTrue(results.isEmpty());
    }

    @Test
    public void getInstanceIdsShouldReturnCorrectDataIfNonEmptyCursor() {
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getCount()).thenReturn(1);
        when(mockCursor.moveToNext()).thenReturn(false);

        List<Long> results = mapper.getInstanceIds(mockCursor);

        assertFalse(results.isEmpty());
        assertEquals(1L, results.get(0).longValue());
    }

    @Test
    public void getFormInstanceWithMediaShouldReturnEmptyIfNullCursor() {
        Pair<FormInstance, Set<String>> results = mapper.getFormInstanceWithMedia("123", null);

        assertNull(results.first);
        assertTrue(results.second.isEmpty());
    }

    @Test
    public void getFormInstanceWithMediaShouldReturnEmptyIfEmptyCursor() {
        when(mockCursor.moveToFirst()).thenReturn(false);

        Pair<FormInstance, Set<String>> results = mapper.getFormInstanceWithMedia("12", mockCursor);

        assertNull(results.first);
        assertTrue(results.second.isEmpty());
    }

    @Test
    public void getFormInstanceWithMediaShouldReturnEmptyResponsesIfEmptyValue() {
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.moveToNext()).thenReturn(false);

        Pair<FormInstance, Set<String>> results = mapper.getFormInstanceWithMedia("12", mockCursor);

        assertTrue(results.first.getResponses().isEmpty());
        assertTrue(results.second.isEmpty());
        verify(mockResponseMapper, times(0)).extractResponse(mockCursor, "12");
    }

    @Test
    public void getFormInstanceWithMediaShouldReturnEmptyFilenamesIfEmptyValue() {
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.moveToNext()).thenReturn(false);
        when(mockTextValueCleaner.cleanVal(anyString())).thenReturn("abc1");
        when(mockTextValueCleaner.sanitizeValue(anyString())).thenReturn("abc");
        when(mockFileHelper.getFilenameFromPath(anyString())).thenReturn("");
        when(mockResponseMapper.extractResponse(mockCursor, "abc")).thenReturn(mockResponse);

        Pair<FormInstance, Set<String>> results = mapper.getFormInstanceWithMedia("12", mockCursor);

        assertFalse(results.first.getResponses().isEmpty());
        assertTrue(results.second.isEmpty());
        verify(mockResponseMapper, times(1)).extractResponse(mockCursor, "abc");
    }

    @Test
    public void getFormInstanceWithMediaShouldReturnValidValues() {
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.moveToNext()).thenReturn(false);
        when(mockTextValueCleaner.sanitizeValue(anyString())).thenReturn("abc");
        when(mockFileHelper.getFilenameFromPath(anyString())).thenReturn("file");
        when(mockResponseMapper.extractResponse(mockCursor, "abc")).thenReturn(mockResponse);

        Pair<FormInstance, Set<String>> results = mapper.getFormInstanceWithMedia("12", mockCursor);

        assertFalse(results.first.getResponses().isEmpty());
        assertFalse(results.second.isEmpty());
        verify(mockResponseMapper, times(1)).extractResponse(mockCursor, "abc");
    }
}