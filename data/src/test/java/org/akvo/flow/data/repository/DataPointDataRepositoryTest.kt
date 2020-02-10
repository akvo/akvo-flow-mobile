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

import com.nhaarman.mockitokotlin2.spy
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
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
import org.akvo.flow.domain.exception.AssignmentRequiredException
import org.akvo.flow.domain.util.DeviceHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import java.net.HttpURLConnection
import java.text.DateFormat
import kotlin.test.assertEquals

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
    internal var mapper = DataPointImageMapper()

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
        `when`(mockDeviceHelper.androidId).thenReturn("")
        `when`(mockDeviceHelper.imei).thenReturn("")
        `when`(mockDeviceHelper.phoneNumber).thenReturn("")
        `when`(mapper.getImagesList(anyList())).thenReturn(emptyList())
        spyRestApi = spy(RestApi(mockDeviceHelper, null, null, ""))
        spyHttpException = spy(HttpException(retrofit2.Response.success("")))
        `when`(mockDataSourceFactory.dataBaseDataSource).thenReturn(mockDatabaseDataSource)
    }

    @Test
    fun downloadDataPointsShouldReturnExpectedErrorWhenAssignmentMissing() {
        doReturn(Single.error<ApiLocaleResult>(spyHttpException)).`when`(spyRestApi)
            .downloadDataPoints(anyLong())
        doReturn(HttpURLConnection.HTTP_FORBIDDEN).`when`(spyHttpException).code()

        val repository =
            DataPointDataRepository(mockDataSourceFactory, spyRestApi, mockS3RestApi, mapper)
        val observer = TestObserver<Int>()

        repository.downloadDataPoints(123L).subscribe(observer)

        observer.assertError(AssignmentRequiredException::class.java)
    }

    @Test
    fun downloadDataPointsShouldReturnExpectedErrorWhenNotAssignmentMissing() {
        doReturn(Single.error<ApiLocaleResult>(spyHttpException)).`when`(spyRestApi)
            .downloadDataPoints(anyLong())
        doReturn(HttpURLConnection.HTTP_BAD_GATEWAY).`when`(spyHttpException).code()

        val repository =
            DataPointDataRepository(mockDataSourceFactory, spyRestApi, mockS3RestApi, mapper)
        val observer = TestObserver<Int>()

        repository.downloadDataPoints(123L).subscribe(observer)

        observer.assertError(HttpException::class.java)
    }

    @Test
    fun downloadDataPointsShouldReturnAnyErrorWhenNotAssignmentMissing() {
        doReturn(Single.error<ApiLocaleResult>(Exception())).`when`(spyRestApi)
            .downloadDataPoints(anyLong())

        val repository =
            DataPointDataRepository(mockDataSourceFactory, spyRestApi, mockS3RestApi, mapper)
        val observer = TestObserver<Int>()

        repository.downloadDataPoints(123L).subscribe(observer)

        observer.assertError(Exception::class.java)
    }

    @Test
    fun downloadDataPointsShouldReturnCorrectResultIfSuccess() {
        doReturn(Single.just(mockApiResponse)).`when`(spyRestApi).downloadDataPoints(anyLong())
        doReturn(Completable.complete()).`when`(mockDatabaseDataSource)!!.syncDataPoints(
            anyList(),
            anyLong()
        )
        doReturn(mockApiDataPoints).`when`(mockApiResponse)!!.dataPoints
        doReturn(1).`when`(mockApiDataPoints)!!.size

        val repository =
            DataPointDataRepository(mockDataSourceFactory, spyRestApi, mockS3RestApi, mapper)
        val observer = TestObserver<Int>()

        repository.downloadDataPoints(123L).subscribe(observer)

        observer.assertNoErrors()
        assertEquals(1, observer.values()[0])
    }

    private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)
}
