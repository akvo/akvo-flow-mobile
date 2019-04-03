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

import org.akvo.flow.database.TransmissionColumns;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;

@RunWith(PowerMockRunner.class)
public class TransmissionMapperTest {

    @Mock
    Cursor mockCursor;

    @Mock
    S3FileMapper mockS3Mapper;

    @Mock
    S3File mockS3File;

    @Test
    public void shouldReturnEmptyForNullCursor() {
        TransmissionMapper mapper = new TransmissionMapper(mockS3Mapper);

        List<Transmission> transmissions = mapper.transform(null);

        assertTrue(transmissions.isEmpty());
    }

    @Test
    public void shouldReturnEmptyForEmptyCursor() {
        TransmissionMapper mapper = new TransmissionMapper(mockS3Mapper);
        given(mockCursor.getCount()).willReturn(0);
        given(mockCursor.moveToFirst()).willReturn(false);
        doNothing().when(mockCursor).close();

        List<Transmission> transmissions = mapper.transform(mockCursor);

        assertTrue(transmissions.isEmpty());
    }

    @Test
    public void shouldOmitTransmissionsWithNullS3Files() {
        TransmissionMapper mapper = new TransmissionMapper(mockS3Mapper);
        given(mockCursor.getCount()).willReturn(1);
        given(mockCursor.moveToFirst()).willReturn(true).willReturn(false); //one item
        given(mockCursor.moveToNext()).willReturn(false);
        doNothing().when(mockCursor).close();
        given(mockCursor.getColumnIndexOrThrow(TransmissionColumns._ID)).willReturn(0);
        given(mockCursor.getColumnIndexOrThrow(TransmissionColumns.SURVEY_ID)).willReturn(1);
        given(mockCursor.getColumnIndexOrThrow(TransmissionColumns.SURVEY_INSTANCE_ID)).willReturn(2);
        given(mockCursor.getColumnIndexOrThrow(TransmissionColumns.FILENAME)).willReturn(3);
        given(mockCursor.getString(3)).willReturn("");
        given(mockS3Mapper.transform(anyString())).willReturn(null);

        List<Transmission> transmissions = mapper.transform(mockCursor);

        assertTrue(transmissions.isEmpty());
    }

    @Test
    public void shouldReturnCorrectTransmissions() {
        TransmissionMapper mapper = new TransmissionMapper(mockS3Mapper);
        given(mockCursor.getCount()).willReturn(2);
        given(mockCursor.moveToFirst()).willReturn(true).willReturn(false);
        given(mockCursor.moveToNext()).willReturn(true).willReturn(false); //2 items
        doNothing().when(mockCursor).close();
        given(mockCursor.getColumnIndexOrThrow(TransmissionColumns._ID)).willReturn(0);
        given(mockCursor.getColumnIndexOrThrow(TransmissionColumns.SURVEY_ID)).willReturn(1);
        given(mockCursor.getColumnIndexOrThrow(TransmissionColumns.SURVEY_INSTANCE_ID))
                .willReturn(2);
        given(mockCursor.getColumnIndexOrThrow(TransmissionColumns.FILENAME)).willReturn(3);
        given(mockCursor.getString(3)).willReturn("");
        given(mockS3Mapper.transform(anyString())).willReturn(mockS3File);

        List<Transmission> transmissions = mapper.transform(mockCursor);

        assertEquals(2, transmissions.size());
    }
}
