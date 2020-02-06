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
import org.akvo.flow.data.entity.form.FormIdMapper;
import org.akvo.flow.data.entity.S3File;
import org.akvo.flow.data.entity.Transmission;
import org.akvo.flow.data.entity.TransmissionMapper;
import org.akvo.flow.data.entity.UploadError;
import org.akvo.flow.data.entity.UploadFormDeletedError;
import org.akvo.flow.data.entity.UploadSuccess;
import org.akvo.flow.data.net.RestApi;
import org.akvo.flow.data.net.s3.AmazonAuthHelper;
import org.akvo.flow.data.net.s3.BodyCreator;
import org.akvo.flow.data.util.ApiUrls;
import org.akvo.flow.domain.util.DeviceHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import okhttp3.RequestBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    FormIdMapper mockFormIdMapper;

    @Mock
    TransmissionMapper mockTransmissionMapper;

    @Mock
    DeviceHelper mockDeviceHelper;

    @Mock
    S3File mockS3File;

    @Mock
    File mockFile;

    @Mock
    SimpleDateFormat mockDateFormat;

    @Mock
    AmazonAuthHelper mockAmazonAuth;

    @Mock
    BodyCreator mockBodyCreator;

    @Mock
    RequestBody mockBody;

    private SurveyDataRepository surveyDataRepository;
    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8080);

        when(mockDataSourceFactory.getDataBaseDataSource()).thenReturn(mockDataBaseDataSource);
        when(mockDataBaseDataSource.getFormIds(anyString()))
                .thenReturn(Observable.just(mockCursor));
        when(mockDataBaseDataSource.getUnSyncedTransmissions(anyString()))
                .thenReturn(Observable.just(mockCursor));

        RestApi restApi = new RestApi(mockDeviceHelper, new TestRestServiceFactory(),
                "1.2.3", new ApiUrls("", ""), mockAmazonAuth, mockDateFormat, mockBodyCreator);
        when(mockDateFormat
                .format(any(Date.class), any(StringBuffer.class), any(FieldPosition.class)))
                .thenReturn(new StringBuffer().append("12-12-2012"));

        when(mockAmazonAuth.getAmazonAuthForPut(anyString(), anyString(), any(S3File.class)))
                .thenReturn("123");

        surveyDataRepository = new SurveyDataRepository(mockDataSourceFactory, null, restApi,
                null, null, null, mockTransmissionMapper, null, mockFormIdMapper, null);

        when(mockDeviceHelper.getPhoneNumber()).thenReturn("123");
        when(mockDeviceHelper.getImei()).thenReturn("123");
        when(mockDeviceHelper.getAndroidId()).thenReturn("123");

        when(mockBodyCreator.createBody(any(S3File.class))).thenReturn(mockBody);

        when(mockS3File.getAction()).thenReturn("submit");
        when(mockS3File.getContentType()).thenReturn("application/zip");
        when(mockS3File.getDir()).thenReturn("devicezip");
        when(mockS3File.getFile()).thenReturn(mockFile);
        when(mockS3File.isPublic()).thenReturn(false);
        when(mockS3File.getFilename()).thenReturn("abc.zip");
        when(mockS3File.getMd5Hex()).thenReturn("123");
        when(mockS3File.getMd5Base64()).thenReturn("123");
    }

    @Test
    public void shouldReturnExpectedSurveyTransmissions() {
        List<String> formIds = new ArrayList<>(2);
        formIds.add("1");
        formIds.add("2");
        when(mockFormIdMapper.mapToFormId(any(Cursor.class))).thenReturn(formIds);

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
        when(mockFormIdMapper.mapToFormId(any(Cursor.class))).thenReturn(Collections.<String>emptyList());

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
        when(mockFormIdMapper.mapToFormId(any(Cursor.class))).thenReturn(formIds);

        when(mockTransmissionMapper.transform(any(Cursor.class)))
                .thenReturn(Collections.<Transmission>emptyList());

        TestObserver observer = new TestObserver<List<Transmission>>();

        surveyDataRepository.getSurveyTransmissions("abc").subscribe(observer);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        assertEquals(0, ((ArrayList) observer.values().get(0)).size());
    }

    @Test
    public void shouldReturnFormDeletedErrorWhenFormDeleted() {
        TestObserver observer = new TestObserver<Set<String>>();
        MockResponse s3Response = new MockResponse().setResponseCode(200);
        s3Response.addHeader("ETag", "\"123\"");
        s3Response.setBody("{}");

        MockResponse gaeResponse = new MockResponse().setResponseCode(404);
        mockWebServer.enqueue(s3Response);
        mockWebServer.enqueue(gaeResponse);

        Transmission transmission = new Transmission(1L, 2L, "123", mockS3File);

        surveyDataRepository.syncTransmission(transmission, "123").subscribe(observer);

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        assertTrue(observer.values().get(0) instanceof UploadFormDeletedError);
        verify(mockDataBaseDataSource, times(0)).setFileTransmissionSucceeded(anyLong());
        verify(mockDataBaseDataSource, times(1)).setFileTransmissionFormDeleted(anyLong());
        verify(mockDataBaseDataSource, times(0)).setFileTransmissionFailed(anyLong());
    }

    @Test
    public void shouldReturnFileTransmissionFailedIfError() {
        TestObserver observer = new TestObserver<Set<String>>();
        MockResponse s3Response = new MockResponse().setResponseCode(200);
        s3Response.addHeader("ETag", "\"123\"");
        s3Response.setBody("{}");

        MockResponse gaeResponse = new MockResponse().setResponseCode(500);
        mockWebServer.enqueue(s3Response);
        mockWebServer.enqueue(gaeResponse);

        Transmission transmission = new Transmission(1L, 2L, "123", mockS3File);

        surveyDataRepository.syncTransmission(transmission, "123").subscribe(observer);

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        assertTrue(observer.values().get(0) instanceof UploadError);
        verify(mockDataBaseDataSource, times(0)).setFileTransmissionSucceeded(anyLong());
        verify(mockDataBaseDataSource, times(0)).setFileTransmissionFormDeleted(anyLong());
        verify(mockDataBaseDataSource, times(1)).setFileTransmissionFailed(anyLong());
    }

    @Test
    public void shouldReturnFileTransmissionFailedIfWrongEtag() {
        TestObserver observer = new TestObserver<Set<String>>();
        MockResponse s3Response = new MockResponse().setResponseCode(200);
        s3Response.addHeader("ETag", "\"1234\"");
        s3Response.setBody("{}");

        MockResponse gaeResponse = new MockResponse().setResponseCode(200);
        mockWebServer.enqueue(s3Response);
        mockWebServer.enqueue(gaeResponse);

        Transmission transmission = new Transmission(1L, 2L, "123", mockS3File);

        surveyDataRepository.syncTransmission(transmission, "123").subscribe(observer);

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        assertTrue(observer.values().get(0) instanceof UploadError);
        verify(mockDataBaseDataSource, times(0)).setFileTransmissionSucceeded(anyLong());
        verify(mockDataBaseDataSource, times(0)).setFileTransmissionFormDeleted(anyLong());
        verify(mockDataBaseDataSource, times(1)).setFileTransmissionFailed(anyLong());
    }

    @Test
    public void shouldReturnSuccessIfAllCallsSuccessful() {
        TestObserver observer = new TestObserver<Set<String>>();
        MockResponse s3Response = new MockResponse().setResponseCode(200);
        s3Response.addHeader("ETag", "\"123\"");
        s3Response.setBody("{}");

        MockResponse gaeResponse = new MockResponse().setResponseCode(200);
        mockWebServer.enqueue(s3Response);
        mockWebServer.enqueue(gaeResponse);

        Transmission transmission = new Transmission(1L, 2L, "123", mockS3File);

        surveyDataRepository.syncTransmission(transmission, "123").subscribe(observer);

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        assertTrue(observer.values().get(0) instanceof UploadSuccess);
        verify(mockDataBaseDataSource, times(1)).setFileTransmissionSucceeded(anyLong());
        verify(mockDataBaseDataSource, times(0)).setFileTransmissionFormDeleted(anyLong());
        verify(mockDataBaseDataSource, times(0)).setFileTransmissionFailed(anyLong());
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
}

