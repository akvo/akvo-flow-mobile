/*
 * Copyright (C) 2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.repository

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.spy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.akvo.flow.data.datasource.DataSourceFactory
import org.akvo.flow.data.datasource.DatabaseDataSource
import org.akvo.flow.data.entity.ApiDataPoint
import org.akvo.flow.data.entity.ApiLocaleResult
import org.akvo.flow.data.entity.images.DataPointImageMapper
import org.akvo.flow.data.net.RestApi
import org.akvo.flow.data.net.RestServiceFactory
import org.akvo.flow.data.net.s3.AmazonAuthHelper
import org.akvo.flow.data.net.s3.BodyCreator
import org.akvo.flow.data.net.s3.S3RestApi
import org.akvo.flow.data.util.MediaHelper
import org.akvo.flow.domain.exception.AssignmentRequiredException
import org.akvo.flow.domain.util.DeviceHelper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import java.net.HttpURLConnection
import java.text.DateFormat
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class DataPointDataRepositoryTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Mock
    internal var mockDatabaseDataSource: DatabaseDataSource = mock(DatabaseDataSource::class.java)

    @Mock
    var mockApiResponse: ApiLocaleResult = mock(ApiLocaleResult::class.java)

    @Mock
    internal var mockApiDataPoints: List<ApiDataPoint> = emptyList()

    @Mock
    internal var mockDeviceHelper = DeviceHelper(null)

    @Mock
    var mockDataSourceFactory: DataSourceFactory = mock(DataSourceFactory::class.java)

    @Mock
    internal var mediaHelper = MediaHelper()

    @Mock
    internal var mapper = DataPointImageMapper(mediaHelper)

    @Mock
    internal var mockS3RestApi: S3RestApi = S3RestApi(
        RestServiceFactory(null, null),
        AmazonAuthHelper(null, null),
        DateFormat.getDateInstance(),
        BodyCreator(),
        ""
    )

    lateinit var spyHttpException: HttpException

    @Mock
    val mockRestApi: RestApi = mock(RestApi::class.java)

    @Before
    fun setUp() {
        `when`(mapper.getImagesList(anyList())).thenReturn(emptyList())
        `when`(mockDeviceHelper.androidId).thenReturn("123")
        spyHttpException = spy(HttpException(retrofit2.Response.success("")))
        `when`(mockDataSourceFactory.dataBaseDataSource).thenReturn(mockDatabaseDataSource)
        //doReturn(mockApiResponse).`when`(mockRestApi).downloadDataPoints(anyLong(), anyString())
    /*    mockRestApi.stub {
            onBlocking {
                downloadDataPoints(
                    anyLong(),
                    anyString()
                )
            }.doReturn(ApiLocaleResult(emptyList(), "", 0, 0, 0, cursor = ""))
        }*/
    }

    @Test(expected = AssignmentRequiredException::class)
    fun downloadDataPointsShouldReturnExpectedErrorWhenAssignmentMissing() = runBlockingTest {
        doThrow(spyHttpException).`when`(mockRestApi).downloadDataPoints(anyLong(), anyString())
        doReturn(HttpURLConnection.HTTP_FORBIDDEN).`when`(spyHttpException).code()

        val repository =
            DataPointDataRepository(
                mockDataSourceFactory,
                mockRestApi,
                mockS3RestApi,
                mapper,
                mediaHelper
            )

        repository.downloadDataPoints(123L)
    }

    @Test(expected = HttpException::class)
    fun downloadDataPointsShouldReturnExpectedErrorWhenNotAssignmentMissing() = runBlockingTest {
        doThrow(spyHttpException).`when`(mockRestApi).downloadDataPoints(anyLong(), anyString())
        doReturn(HttpURLConnection.HTTP_BAD_GATEWAY).`when`(spyHttpException).code()

        val repository =
            DataPointDataRepository(
                mockDataSourceFactory,
                mockRestApi,
                mockS3RestApi,
                mapper,
                mediaHelper
            )

        repository.downloadDataPoints(123L)
    }

    @Test(expected = Exception::class)
    fun downloadDataPointsShouldReturnAnyErrorWhenNotAssignmentMissing() {
        runBlocking {
            doThrow(Exception()).`when`(mockRestApi).downloadDataPoints(anyLong(), anyString())

            val repository =
                DataPointDataRepository(
                    mockDataSourceFactory,
                    mockRestApi,
                    mockS3RestApi,
                    mapper,
                    mediaHelper
                )

            repository.downloadDataPoints(123L)
        }
    }

    @Test
    fun downloadDataPointsShouldReturnCorrectResultIfSuccess() = mainCoroutineRule.runBlockingTest {
        doReturn(1).`when`(mockDatabaseDataSource)!!.syncDataPoints(
            anyList()
        )
        doReturn(null).`when`(mockDatabaseDataSource)!!.getDataPointCursor(anyLong())
        //doReturn(mockApiDataPoints).`when`(mockApiResponse)!!.dataPoints

 /*      mockRestApi.stub {
            onBlocking {
                downloadDataPoints(
                    anyLong(),
                    anyString()
                )
            }.doReturn(mockApiResponse)
        }*/

        //doReturn(mockApiResponse).`when`(mockRestApi).downloadDataPoints(anyLong(), anyString())
   /*    `when`(mockRestApi.downloadDataPoints(anyLong(), anyString())).thenReturn(
            ApiLocaleResult(emptyList(), "", 0, 0, 0, cursor = "")
        )*/
       // doReturn(mockApiResponse).`when`(mockRestApi).downloadDataPoints(anyLong(), anyString())
        val repository =
            DataPointDataRepository(
                mockDataSourceFactory,
                mockRestApi,
                mockS3RestApi,
                mapper,
                mediaHelper
            )

        val result: Int = repository.downloadDataPoints(123L)

        assertEquals(1, result)
    }
}

@ExperimentalCoroutinesApi
private fun MainCoroutineRule.runBlockingTest(block: suspend () -> Unit) =
    this.testDispatcher.runBlockingTest {
        block()
    }

@ExperimentalCoroutinesApi
class MainCoroutineRule(
    val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) : TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
}