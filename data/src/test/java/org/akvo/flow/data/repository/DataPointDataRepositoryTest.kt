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

import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.spy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
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
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import java.net.HttpURLConnection
import java.text.DateFormat
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class DataPointDataRepositoryTest {

    @Mock
    internal var mockDatabaseDataSource: DatabaseDataSource? = null

    @Mock
    internal var mockApiResponse: ApiLocaleResult? = null

    @Mock
    internal var mockApiDataPoints: List<ApiDataPoint> = emptyList()

    @Mock
    internal var mockDeviceHelper = DeviceHelper(null)

    @Mock
    internal var mockDataSourceFactory: DataSourceFactory =
        DataSourceFactory(null, null, null, null, null, null)

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

    private lateinit var spyHttpException: HttpException

    private lateinit var spyRestApi: RestApi

    @Before
    fun setUp() {
        `when`(mapper.getImagesList(anyList())).thenReturn(emptyList())
        spyRestApi = spy(RestApi(mockDeviceHelper, TestRestServiceFactory(), "", ""))
        spyHttpException = spy(HttpException(retrofit2.Response.success("")))
        `when`(mockDataSourceFactory.dataBaseDataSource).thenReturn(mockDatabaseDataSource)
    }

    @Test(expected = AssignmentRequiredException::class)
    fun downloadDataPointsShouldReturnExpectedErrorWhenAssignmentMissing() = runBlockingTest {
        doThrow(spyHttpException).`when`(spyRestApi).downloadDataPoints(anyLong(), anyString())
        doReturn(HttpURLConnection.HTTP_FORBIDDEN).`when`(spyHttpException).code()

        val repository =
            DataPointDataRepository(
                mockDataSourceFactory,
                spyRestApi,
                mockS3RestApi,
                mapper,
                mediaHelper
            )

        repository.downloadDataPoints(123L)
    }

    @Test(expected = HttpException::class)
    fun downloadDataPointsShouldReturnExpectedErrorWhenNotAssignmentMissing() = runBlockingTest {
        doThrow(spyHttpException).`when`(spyRestApi).downloadDataPoints(anyLong(), anyString())
        doReturn(HttpURLConnection.HTTP_BAD_GATEWAY).`when`(spyHttpException).code()

        val repository =
            DataPointDataRepository(
                mockDataSourceFactory,
                spyRestApi,
                mockS3RestApi,
                mapper,
                mediaHelper
            )

        repository.downloadDataPoints(123L)
    }

    @Test(expected = Exception::class)
    fun downloadDataPointsShouldReturnAnyErrorWhenNotAssignmentMissing()  = runBlockingTest {
        doThrow(Exception()).`when`(spyRestApi).downloadDataPoints(anyLong(), anyString())

        val repository =
            DataPointDataRepository(
                mockDataSourceFactory,
                spyRestApi,
                mockS3RestApi,
                mapper,
                mediaHelper
            )

        repository.downloadDataPoints(123L)
    }

    @Test
    fun downloadDataPointsShouldReturnCorrectResultIfSuccess()  = runBlockingTest {
        doReturn(mockApiResponse).`when`(spyRestApi).downloadDataPoints(anyLong(), anyString())
        doReturn(1).`when`(mockDatabaseDataSource)!!.syncDataPoints(
            anyList()
        )
        doReturn(mockApiDataPoints).`when`(mockApiResponse)!!.dataPoints

        val repository =
            DataPointDataRepository(
                mockDataSourceFactory,
                spyRestApi,
                mockS3RestApi,
                mapper,
                mediaHelper
            )

        val result: Int = repository.downloadDataPoints(123L)

        assertEquals(1, result)
    }
}
