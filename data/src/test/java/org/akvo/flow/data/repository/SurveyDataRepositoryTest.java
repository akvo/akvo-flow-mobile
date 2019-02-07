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

package org.akvo.flow.data.repository;

import android.database.Cursor;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.datasource.DatabaseDataSource;
import org.akvo.flow.data.entity.FormIdMapper;
import org.akvo.flow.data.entity.Transmission;
import org.akvo.flow.data.entity.TransmissionMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SurveyDataRepositoryTest {

    @Mock
    DataSourceFactory mockDataSourceFactory;

    @Mock
    DatabaseDataSource mockDataBaseDataSource;

    @Mock
    Cursor mockCursor;

    @Mock
    FormIdMapper mockMapper;

    @Mock
    TransmissionMapper mockTransmissionMapper;

    private SurveyDataRepository surveyDataRepository;

    @Before
    public void setUp() {
        surveyDataRepository = spy(
                new SurveyDataRepository(mockDataSourceFactory, null, null, null, null, null, null,
                        mockMapper, mockTransmissionMapper, null, null));

        when(mockDataSourceFactory.getDataBaseDataSource()).thenReturn(mockDataBaseDataSource);
        when(mockDataBaseDataSource.getFormIds(anyString()))
                .thenReturn(Observable.just(mockCursor));
        when(mockDataBaseDataSource.getUnSyncedTransmissions(anyString()))
                .thenReturn(Observable.just(mockCursor));
    }

    @Test
    public void shouldReturnExpectedSurveyTransmissions() {
        List<String> formIds = new ArrayList<>(2);
        formIds.add("1");
        formIds.add("2");
        when(mockMapper.mapToFormId(any(Cursor.class))).thenReturn(formIds);

        List<Transmission> oneTransmissionList = new ArrayList<>(1);
        oneTransmissionList.add(new Transmission(0L, 0L, "1", null));
        List<Transmission> twoTransmissionsList = new ArrayList<>(2);
        twoTransmissionsList.add(new Transmission(1L, 1L, "2", null));
        twoTransmissionsList.add(new Transmission(2L, 2L, "2", null));
        when(mockTransmissionMapper.transform(any(Cursor.class))).thenReturn(oneTransmissionList)
                .thenReturn(twoTransmissionsList);

        TestObserver observer = new TestObserver<List<Transmission>>();

        surveyDataRepository.getSurveyTransmissions("abc").subscribe(observer);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        assertEquals(3, ((ArrayList) observer.values().get(0)).size());
    }

    @Test
    public void shouldReturnEmptySurveyTransmissionsForEmptyFormsList() {
        when(mockMapper.mapToFormId(any(Cursor.class))).thenReturn(Collections.<String>emptyList());

        TestObserver observer = new TestObserver<List<Transmission>>();

        surveyDataRepository.getSurveyTransmissions("").subscribe(observer);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        assertEquals(0, ((ArrayList) observer.values().get(0)).size());
    }

    @Test
    public void shouldReturnEmptyTransmissionsIfNotFound() {
        List<String> formIds = new ArrayList<>(2);
        formIds.add("1");
        formIds.add("2");
        when(mockMapper.mapToFormId(any(Cursor.class))).thenReturn(formIds);

        when(mockTransmissionMapper.transform(any(Cursor.class))).thenReturn(Collections.<Transmission>emptyList());

        TestObserver observer = new TestObserver<List<Transmission>>();

        surveyDataRepository.getSurveyTransmissions("abc").subscribe(observer);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        assertEquals(0, ((ArrayList) observer.values().get(0)).size());
    }

}

