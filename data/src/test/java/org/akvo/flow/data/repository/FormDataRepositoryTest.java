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

package org.akvo.flow.data.repository;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.database.Cursor;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.datasource.DatabaseDataSource;
import org.akvo.flow.data.datasource.files.FileDataSource;
import org.akvo.flow.data.entity.ApiFormHeader;
import org.akvo.flow.data.entity.form.DataForm;
import org.akvo.flow.data.entity.form.DataFormMapper;
import org.akvo.flow.data.entity.form.DomainFormMapper;
import org.akvo.flow.data.entity.form.FormHeaderParser;
import org.akvo.flow.data.entity.form.FormIdMapper;
import org.akvo.flow.data.net.RestApi;
import org.akvo.flow.data.net.s3.AmazonAuthHelper;
import org.akvo.flow.data.net.s3.BodyCreator;
import org.akvo.flow.data.net.s3.S3RestApi;
import org.akvo.flow.domain.util.DeviceHelper;
import org.akvo.flow.utils.XmlFormParser;
import org.akvo.flow.utils.entity.Form;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(MockitoJUnitRunner.class)
public class FormDataRepositoryTest {

    @Mock
    FormHeaderParser mockFormHeaderParser;

    @Mock
    XmlFormParser mockXmlParser;

    @Mock
    DatabaseDataSource mockDatabaseDataSource;

    @Mock
    FileDataSource mockFileDataSource;

    @Mock
    FormIdMapper mockFormIdMapper;

    @Mock
    DeviceHelper mockDeviceHelper;

    @Mock
    InputStream mockInputStream;

    @Mock
    DateFormat mockDateFormat;

    @Mock
    AmazonAuthHelper mockAmazonAuth;

    @Mock
    Cursor mockCursor;

    @Mock
    Form mockForm;

    @Mock
    DomainFormMapper mockDomainFormMapper;

    @Mock
    DataFormMapper mockDataFormMapper;

    private MockWebServer mockWebServer;
    private FormDataRepository formDataRepository;
    private RestApi restApi;
    private S3RestApi s3RestApi;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8080);

        restApi = spy(new RestApi(mockDeviceHelper, new TestRestServiceFactory(), "1.2.3", ""));
        s3RestApi = spy(new S3RestApi(new TestRestServiceFactory(), mockAmazonAuth, mockDateFormat,
                new BodyCreator(), ""));
        doReturn("12-12-2012GMT").when(s3RestApi).formattedDate();
        DataSourceFactory dataSourceFactory = new DataSourceFactory(null, null,
                mockDatabaseDataSource, null, mockFileDataSource, null);
        formDataRepository = new FormDataRepository(mockFormHeaderParser, mockXmlParser,
                restApi, dataSourceFactory, mockFormIdMapper, s3RestApi, mockDomainFormMapper, mockDataFormMapper);
        ApiFormHeader apiFormHeader = new ApiFormHeader("123456", "", "", "", 1.0, "", true, "");
        when(mockFormHeaderParser.parseOne(anyString())).thenReturn(apiFormHeader);
        when(mockAmazonAuth.getAmazonAuthForGet(anyString(), anyString(), anyString()))
                .thenReturn("123");
        when(mockDatabaseDataSource.insertSurveyGroup(any(ApiFormHeader.class)))
                .thenReturn(Observable.just(true));
        doNothing().when(mockDatabaseDataSource).saveForm(anyBoolean(), any(DataForm.class));
        when(mockFileDataSource.extractRemoteArchive(any(ResponseBody.class), anyString()))
                .thenReturn(Observable.just(true));
        when(mockFileDataSource.getFormFile(anyString()))
                .thenReturn(mockInputStream);
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
        when(mockDatabaseDataSource.formNeedsUpdate(any(ApiFormHeader.class)))
                .thenReturn(Observable.just(false));

        MockResponse response = new MockResponse().setResponseCode(200)
                .setBody(",1,cde,abc,cde,6.0,cde,true,33");
        mockWebServer.enqueue(response);

        formDataRepository.loadForm("1", "deviceId").subscribe(observer);
        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        verify(s3RestApi, times(0)).downloadArchive("123456.zip");
        verify(mockFileDataSource, times(0))
                .extractRemoteArchive(any(ResponseBody.class), anyString());
    }

    @Test
    public void loadFormShouldUpdateOutdatedForm() {
        TestObserver observer = new TestObserver<Boolean>();

        when(mockDatabaseDataSource.formNeedsUpdate(any(ApiFormHeader.class)))
                .thenReturn(Observable.just(true));
        when(mockXmlParser.parseXmlForm(any(InputStream.class), any(Double.class))).thenReturn(mockForm);

        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(",1,cde,abc,cde,6.0,cde,true,33"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{}"));

        formDataRepository.loadForm("1", "deviceId").subscribe(observer);
        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        verify(s3RestApi, times(1)).downloadArchive("123456.zip");
        verify(mockFileDataSource, times(1))
                .extractRemoteArchive(any(ResponseBody.class), anyString());
    }

    @Test
    public void reloadFormsShouldUpdateOutdatedForms() {
        TestObserver observer = new TestObserver<Boolean>();
        when(mockDatabaseDataSource.getFormIds()).thenReturn(Observable.just(mockCursor));
        List<String> formIds = new ArrayList<>(3);
        formIds.add("1");
        formIds.add("2");
        formIds.add("3");
        when(mockFormIdMapper.mapToFormId(mockCursor)).thenReturn(formIds);
        when(mockDatabaseDataSource.formNeedsUpdate(any(ApiFormHeader.class)))
                .thenReturn(Observable.just(true));
        when(mockDatabaseDataSource.deleteAllForms()).thenReturn(Observable.just(true));
        when(mockXmlParser.parseXmlForm(any(InputStream.class), any(Double.class))).thenReturn(mockForm);

        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(
                ",1,cde,abc,cde,6.0,cde,true,33\n"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(
                ",2,cde,abc,cde,6.0,cde,true,34\n"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(
                ",3,cde,abc,cde,6.0,cde,true,35\n"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{}"));

        formDataRepository.reloadForms("deviceId").subscribe(observer);
        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        verify(restApi, times(3)).downloadFormHeader(anyString(), anyString());
    }

    @Test
    public void reloadFormsShouldNotReloadEmptyForms() {
        TestObserver observer = new TestObserver<Boolean>();
        when(mockDatabaseDataSource.getFormIds()).thenReturn(Observable.just(mockCursor));
        List<String> formIds = new ArrayList<>(0);
        when(mockFormIdMapper.mapToFormId(mockCursor)).thenReturn(formIds);
        when(mockDatabaseDataSource.deleteAllForms()).thenReturn(Observable.just(true));

        formDataRepository.reloadForms("deviceId").subscribe(observer);
        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        verify(restApi, times(0)).downloadFormHeader(anyString(), anyString());
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
}
