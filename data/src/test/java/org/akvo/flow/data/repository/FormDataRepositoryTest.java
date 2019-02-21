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

package org.akvo.flow.data.repository;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.datasource.DatabaseDataSource;
import org.akvo.flow.data.datasource.files.FileDataSource;
import org.akvo.flow.data.entity.ApiFormHeader;
import org.akvo.flow.data.entity.FormHeaderParser;
import org.akvo.flow.data.entity.FormIdMapper;
import org.akvo.flow.data.entity.XmlParser;
import org.akvo.flow.data.net.RestApi;
import org.akvo.flow.data.net.s3.AmazonAuthHelper;
import org.akvo.flow.data.util.ApiUrls;
import org.akvo.flow.domain.util.DeviceHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FormDataRepositoryTest {

    @Mock
    FormHeaderParser mockFormHeaderParser;

    @Mock
    XmlParser mockXmlParser;

    @Mock
    DatabaseDataSource mockDatabaseDataSource;

    @Mock
    FileDataSource mockFileDataSource;

    @Mock
    FormIdMapper mockFormIdMapper;

    @Mock
    ApiFormHeader mockApiFormHeader;

    @Mock
    DeviceHelper mockDeviceHelper;

    @Mock
    InputStream mockInputStream;

    @Mock
    DateFormat mockDateFormat;

    @Mock
    AmazonAuthHelper mockAmazonAuth;

    private MockWebServer mockWebServer;
    private FormDataRepository formDataRepository;
    private RestApi restApi;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8080);

        restApi = spy(new RestApi(mockDeviceHelper, new TestRestServiceFactory(), null,
                "1.2.3", new ApiUrls(null, null), mockAmazonAuth, mockDateFormat, null));
        DataSourceFactory dataSourceFactory = new DataSourceFactory(null, null,
                mockDatabaseDataSource, null, mockFileDataSource, null);
        formDataRepository = new FormDataRepository(mockFormHeaderParser, mockXmlParser,
                restApi, dataSourceFactory, mockFormIdMapper);

        when(mockFormHeaderParser.parseOne(anyString())).thenReturn(mockApiFormHeader);
        when(mockDateFormat
                .format(any(Date.class), any(StringBuffer.class), any(FieldPosition.class)))
                .thenReturn(new StringBuffer().append("12-12-2012"));
        when(mockAmazonAuth.getAmazonAuthForGet(anyString(), anyString(), anyString()))
                .thenReturn("123");
    }

    @Test
    public void loadFormShouldInstallTestFormIfTestId() {
        TestObserver observer = new TestObserver<Boolean>();
        when(mockDatabaseDataSource.installTestForm()).thenReturn(Observable.just(true));

        formDataRepository.loadForm("0", "deviceId").subscribe(observer);

        verify(mockDatabaseDataSource, times(1)).installTestForm();
        observer.assertNoErrors();
    }

    @Test
    public void loadFormShouldNotUpdateAlreadyUpdatedForm() {
        TestObserver observer = new TestObserver<Boolean>();
        when(mockDatabaseDataSource.insertSurveyGroup(any(ApiFormHeader.class)))
                .thenReturn(Observable.just(true));
        when(mockDatabaseDataSource.formNeedsUpdate(any(ApiFormHeader.class)))
                .thenReturn(Observable.just(false));

        MockResponse response = new MockResponse().setResponseCode(200)
                .setBody(",1,cde,abc,cde,6.0,cde,true,33");
        mockWebServer.enqueue(response);

        formDataRepository.loadForm("1", "deviceId").subscribe(observer);
        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        verify(restApi, times(0)).downloadArchive(anyString());
        verify(mockFileDataSource, times(0))
                .extractRemoteArchive(any(ResponseBody.class), anyString());
    }

    @Test
    public void loadFormShouldUpdateOutdatedForm() {
        TestObserver observer = new TestObserver<Boolean>();
        when(mockDatabaseDataSource.insertSurveyGroup(any(ApiFormHeader.class)))
                .thenReturn(Observable.just(true));
        when(mockDatabaseDataSource.formNeedsUpdate(any(ApiFormHeader.class)))
                .thenReturn(Observable.just(true));
        when(mockFileDataSource.extractRemoteArchive(any(ResponseBody.class), anyString()))
                .thenReturn(Observable.just(true));
        when(mockFileDataSource.getFormFile(anyString()))
                .thenReturn(Observable.just(mockInputStream));
        when(mockXmlParser.parse(mockInputStream)).thenReturn(Collections.<String>emptyList());
        when(mockDatabaseDataSource.insertSurvey(any(ApiFormHeader.class), anyBoolean()))
                .thenReturn(Observable.just(true));

        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(",1,cde,abc,cde,6.0,cde,true,33"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{}"));

        formDataRepository.loadForm("1", "deviceId").subscribe(observer);
        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        verify(restApi, times(1)).downloadArchive(anyString());
        verify(mockFileDataSource, times(1))
                .extractRemoteArchive(any(ResponseBody.class), anyString());
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
}
