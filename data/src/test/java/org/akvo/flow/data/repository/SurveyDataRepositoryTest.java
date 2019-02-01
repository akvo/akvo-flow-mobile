package org.akvo.flow.data.repository;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.entity.FilesResultMapper;
import org.akvo.flow.data.net.RestApi;
import org.akvo.flow.data.util.ApiUrls;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.observers.TestObserver;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SurveyDataRepositoryTest {

    private MockWebServer mockWebServer;
    private MissingAndDeletedDataRepository missingAndDeletedDataRepository;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8080);

        DataSourceFactory dataSourceFactory = new DataSourceFactory(null, null,
                new TestDataBaseDataSource(), null, null, null);

        RestApi restApi = new RestApi(new TestDeviceHelper(), new TestRestServiceFactory(), null,
                "1.2.3", new ApiUrls(null, null), null, null);

        missingAndDeletedDataRepository = spy(new MissingAndDeletedDataRepository(restApi,
                new FilesResultMapper(), dataSourceFactory));
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
                .setBody("{\"missingUnknown\":[],\"missingFiles\":[],\"deletedForms\":[1234,12345]}");
        mockWebServer.enqueue(response);

        List<String> forms = new ArrayList<>(1);
        forms.add("123");

        missingAndDeletedDataRepository.downloadMissingAndDeleted(forms, "123").subscribe(observer);
        verify(missingAndDeletedDataRepository, times(1)).getPendingFiles(forms, "123");
        verify(missingAndDeletedDataRepository, times(1)).saveMissing(Collections.<String>emptySet());
        Set<String> set = new HashSet<>();
        set.add("1234");
        set.add("12345");
        verify(missingAndDeletedDataRepository, times(1)).saveDeletedForms(set);

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        assertEquals(2, ((HashSet)observer.values().get(0)).size());
    }

    @Test
    public void testReturnMissingUnknown() {
        TestObserver observer = new TestObserver<Set<String>>();

        MockResponse response = new MockResponse().setResponseCode(200)
                .setBody("{\"missingUnknown\":[\"123.jpg\",\"1234.jpg\"],\"missingFiles\":[],\"deletedForms\":[]}");
        mockWebServer.enqueue(response);

        List<String> forms = new ArrayList<>(1);
        forms.add("123");

        missingAndDeletedDataRepository.downloadMissingAndDeleted(forms, "123").subscribe(observer);

        verify(missingAndDeletedDataRepository, times(1)).getPendingFiles(forms, "123");
        Set<String> set = new HashSet<>();
        set.add("123.jpg");
        set.add("1234.jpg");
        verify(missingAndDeletedDataRepository, times(1)).saveMissing(set);
        verify(missingAndDeletedDataRepository, times(1)).saveDeletedForms(Collections.<String>emptySet());

        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        assertEquals(0, ((HashSet)observer.values().get(0)).size());
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