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

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.datasource.DatabaseDataSource;
import org.akvo.flow.data.entity.FilesResultMapper;
import org.akvo.flow.data.net.RestApi;
import org.akvo.flow.domain.util.DeviceHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MissingAndDeletedDataRepositoryTest {

    private MockWebServer mockWebServer;
    private MissingAndDeletedDataRepository missingAndDeletedDataRepository;

    @Mock
    DeviceHelper mockDeviceHelper;

    @Mock
    DatabaseDataSource mockDatabaseDataSource;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8080);

        DataSourceFactory dataSourceFactory = new DataSourceFactory(null, null,
                mockDatabaseDataSource, null, null, null);

        RestApi restApi = new RestApi(mockDeviceHelper, new TestRestServiceFactory(),
                "1.2.3", "");

        missingAndDeletedDataRepository = spy(new MissingAndDeletedDataRepository(restApi,
                new FilesResultMapper(), dataSourceFactory));

        when(mockDeviceHelper.getPhoneNumber()).thenReturn("123");
        when(mockDeviceHelper.getImei()).thenReturn("123");
        when(mockDeviceHelper.getAndroidId()).thenReturn("123");

        when(mockDatabaseDataSource.saveMissingFiles(anySet())).thenReturn(Observable.just(true));
        when(mockDatabaseDataSource.updateFailedTransmissionsSurveyInstances(anySet()))
                .thenReturn(Observable.just(true));
        when(mockDatabaseDataSource.setDeletedForms(anySet())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                return Observable.just((HashSet) args[0]);
            }
        });
    }

    @Test
    public void testReturnEmptyData() {
        TestObserver observer = new TestObserver<Set<String>>();

        MockResponse response = new MockResponse().setResponseCode(200)
                .setBody("{\"missingUnknown\":[],\"missingFiles\":[],\"deletedForms\":[]}");
        mockWebServer.enqueue(response);

        List<String> forms = new ArrayList<>(1);
        forms.add("123");

        missingAndDeletedDataRepository.downloadMissingAndDeleted(forms, "123").subscribe(observer);

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
    }

    @Test
    public void testReturnEmptyDataWhenEmptyJson() throws InterruptedException {
        TestObserver observer = new TestObserver<Set<String>>();

        MockResponse response = new MockResponse().setResponseCode(200).setBody("{}");
        mockWebServer.enqueue(response);

        List<String> forms = new ArrayList<>(1);
        forms.add("123");
        missingAndDeletedDataRepository.downloadMissingAndDeleted(forms, "123").subscribe(observer);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertTrue(request.getPath().startsWith("/devicenotification"));

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
    }

    @Test
    public void testReturnEmptyDataWhenErrorJson() throws InterruptedException {
        TestObserver observer = new TestObserver<Set<String>>();

        MockResponse response = new MockResponse().setResponseCode(200).setBody("{abcd}");
        mockWebServer.enqueue(response);

        List<String> forms = new ArrayList<>(1);
        forms.add("123");
        missingAndDeletedDataRepository.downloadMissingAndDeleted(forms, "123").subscribe(observer);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertTrue(request.getPath().startsWith("/devicenotification"));

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoValues();
        assertEquals(1, observer.errorCount());
    }

    @Test
    public void testReturnDeletedForms() {
        TestObserver observer = new TestObserver<Set<String>>();

        MockResponse response = new MockResponse().setResponseCode(200)
                .setBody(
                        "{\"missingUnknown\":[],\"missingFiles\":[],\"deletedForms\":[1234,12345]}");
        mockWebServer.enqueue(response);

        List<String> forms = new ArrayList<>(1);
        forms.add("123");

        missingAndDeletedDataRepository.downloadMissingAndDeleted(forms, "123").subscribe(observer);
        verify(missingAndDeletedDataRepository, times(1)).getPendingFiles(forms, "123");
        verify(missingAndDeletedDataRepository, times(1))
                .saveMissing(Collections.<String>emptySet());
        Set<String> set = new HashSet<>();
        set.add("1234");
        set.add("12345");
        verify(missingAndDeletedDataRepository, times(1)).saveDeletedForms(set);

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        assertEquals(2, ((HashSet) observer.values().get(0)).size());
    }

    @Test
    public void testReturnMissingUnknown() {
        TestObserver observer = new TestObserver<Set<String>>();

        MockResponse response = new MockResponse().setResponseCode(200)
                .setBody(
                        "{\"missingUnknown\":[\"123.jpg\",\"1234.jpg\"],\"missingFiles\":[],\"deletedForms\":[]}");
        mockWebServer.enqueue(response);

        List<String> forms = new ArrayList<>(1);
        forms.add("123");

        missingAndDeletedDataRepository.downloadMissingAndDeleted(forms, "123").subscribe(observer);

        verify(missingAndDeletedDataRepository, times(1)).getPendingFiles(forms, "123");
        Set<String> set = new HashSet<>();
        set.add("123.jpg");
        set.add("1234.jpg");
        verify(missingAndDeletedDataRepository, times(1)).saveMissing(set);
        verify(missingAndDeletedDataRepository, times(1))
                .saveDeletedForms(Collections.<String>emptySet());

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        assertEquals(0, ((HashSet) observer.values().get(0)).size());
    }

    @Test
    public void testErrorReturnWhenServerError() {
        TestObserver observer = new TestObserver<Set<String>>();

        MockResponse response = new MockResponse().setResponseCode(500).setBody("Server Error");
        mockWebServer.enqueue(response);

        List<String> forms = new ArrayList<>(1);
        forms.add("123");

        missingAndDeletedDataRepository.downloadMissingAndDeleted(forms, "123").subscribe(observer);

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoValues();
        assertEquals(1, observer.errorCount());
    }

    @Test
    public void testErrorReturnWhenTimeout() {
        TestObserver observer = new TestObserver<Set<String>>();

        MockResponse response = new MockResponse().setResponseCode(200)
                .throttleBody(1024, 1, TimeUnit.SECONDS);
        mockWebServer.enqueue(response);

        List<String> forms = new ArrayList<>(1);
        forms.add("123");

        missingAndDeletedDataRepository.downloadMissingAndDeleted(forms, "123").subscribe(observer);

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoValues();
        assertEquals(1, observer.errorCount());
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
}
