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

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.akvo.flow.data.datasource.DataSourceFactory
import org.akvo.flow.data.datasource.DatabaseDataSource
import org.akvo.flow.data.entity.ApiDataPoint
import org.akvo.flow.data.entity.ApiLocaleResult
import org.akvo.flow.data.net.RestApi
import org.akvo.flow.domain.exception.AssignmentRequiredException
import org.akvo.flow.domain.util.DeviceHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.runners.MockitoJUnitRunner
import retrofit2.HttpException
import retrofit2.Response
import java.net.HttpURLConnection
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class DataPointDataRepositoryTest {

    @Mock
    internal var mockDatabaseDataSource: DatabaseDataSource? = null

    @Mock
    internal var mockApiResponse: ApiLocaleResult? = null

    @Mock
    internal var mockApiDataPoints: List<ApiDataPoint>? = null

    @Mock
    internal var mockDeviceHelper: DeviceHelper? = null

    @Mock
    internal var mockDataSourceFactory: DataSourceFactory =
        DataSourceFactory(null, null, null, null, null, null)

    private var spyHttpException: HttpException = spy(HttpException(Response.success("")))

    private lateinit var spyRestApi: RestApi

    @Before
    fun setUp() {
        `when`(mockDeviceHelper!!.androidId).thenReturn("")
        `when`(mockDeviceHelper!!.imei).thenReturn("")
        `when`(mockDeviceHelper!!.phoneNumber).thenReturn("")
        spyRestApi = spy(RestApi(mockDeviceHelper, null, null, null))
        `when`(mockDataSourceFactory.dataBaseDataSource).thenReturn(mockDatabaseDataSource)
    }

    @Test
    fun downloadDataPointsShouldReturnExpectedErrorWhenAssignmentMissing() {
        doReturn(Single.error<ApiLocaleResult>(spyHttpException)).`when`(spyRestApi)
            .downloadDataPoints(anyLong())
        doReturn(HttpURLConnection.HTTP_FORBIDDEN).`when`(spyHttpException).code()

        val repository = DataPointDataRepository(mockDataSourceFactory, spyRestApi)
        val observer = TestObserver<Int>()

        repository.downloadDataPoints(123L).subscribe(observer)

        observer.assertError(AssignmentRequiredException::class.java)
    }

    @Test
    fun downloadDataPointsShouldReturnExpectedErrorWhenNotAssignmentMissing() {
        doReturn(Single.error<ApiLocaleResult>(spyHttpException)).`when`(spyRestApi)
            .downloadDataPoints(anyLong())
        doReturn(HttpURLConnection.HTTP_BAD_GATEWAY).`when`(spyHttpException).code()

        val repository = DataPointDataRepository(mockDataSourceFactory, spyRestApi)
        val observer = TestObserver<Int>()

        repository.downloadDataPoints(123L).subscribe(observer)

        observer.assertError(HttpException::class.java)
    }

    @Test
    fun downloadDataPointsShouldReturnAnyErrorWhenNotAssignmentMissing() {
        doReturn(Single.error<ApiLocaleResult>(Exception())).`when`(spyRestApi)
            .downloadDataPoints(anyLong())

        val repository = DataPointDataRepository(mockDataSourceFactory, spyRestApi)
        val observer = TestObserver<Int>()

        repository.downloadDataPoints(123L).subscribe(observer)

        observer.assertError(Exception::class.java)
    }

    @Test
    fun downloadDataPointsShouldReturnCorrectResultIfSuccess() {
        doReturn(Single.just(mockApiResponse)).`when`(spyRestApi).downloadDataPoints(anyLong())
        doReturn(Completable.complete()).`when`(mockDatabaseDataSource)!!.syncDataPoints(any(), anyLong())
        doReturn(mockApiDataPoints).`when`(mockApiResponse)!!.dataPoints
        doReturn(1).`when`(mockApiDataPoints)!!.size

        val repository = DataPointDataRepository(mockDataSourceFactory, spyRestApi)
        val observer = TestObserver<Int>()

        repository.downloadDataPoints(123L).subscribe(observer)

        observer.assertNoErrors()
        assertEquals(1, observer.values()[0])
    }
}
