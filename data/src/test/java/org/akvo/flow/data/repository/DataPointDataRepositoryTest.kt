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

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.akvo.flow.data.datasource.DataSourceFactory
import org.akvo.flow.data.datasource.DatabaseDataSource
import org.akvo.flow.data.entity.ApiLocaleResult
import org.akvo.flow.data.entity.images.DataPointImageMapper
import org.akvo.flow.data.net.RestApi
import org.akvo.flow.data.net.s3.S3RestApi
import org.akvo.flow.data.util.MediaHelper
import org.akvo.flow.domain.exception.AssignmentRequiredException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.HttpException
import java.net.HttpURLConnection
import kotlin.test.assertEquals

@RunWith(JUnit4::class)
class DataPointDataRepositoryTest {

    @MockK
    lateinit var mockDataSourceFactory: DataSourceFactory

    @RelaxedMockK
    lateinit var mockRestApi: RestApi

    @MockK
    lateinit var mockS3RestApi: S3RestApi

    @MockK
    lateinit var mockMapper: DataPointImageMapper

    @MockK
    lateinit var mediaHelper: MediaHelper

    @RelaxedMockK
    lateinit var mockDatabaseDataSource: DatabaseDataSource

    @MockK
    lateinit var spyHttpException: HttpException

    lateinit var repository: DataPointDataRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockMapper.getImagesList(any()) } returns emptyList()
        every { mockDataSourceFactory.dataBaseDataSource } returns mockDatabaseDataSource
        every { mockDatabaseDataSource.getDataPointCursor(any()) } returns null
        repository =
            DataPointDataRepository(
                mockDataSourceFactory,
                mockRestApi,
                mockS3RestApi,
                mockMapper,
                mediaHelper
            )
    }

    @Test
    fun downloadDataPointsShouldReturnExpectedErrorWhenAssignmentMissing() = runBlocking {
        coEvery {
            mockRestApi.downloadDataPoints(
                any(),
                any()
            )
        } coAnswers { throw spyHttpException }
        every { spyHttpException.code() } returns HttpURLConnection.HTTP_FORBIDDEN

        try {
            val result = repository.downloadDataPoints(123L)
        } catch (e: Exception) {
            assert(e is AssignmentRequiredException)
        }
    }

    @Test
    fun downloadDataPointsShouldReturnExpectedErrorWhenNotAssignmentMissing() = runBlocking {
        coEvery {
            mockRestApi.downloadDataPoints(
                any(),
                any()
            )
        } coAnswers { throw spyHttpException }
        every { spyHttpException.code() } returns HttpURLConnection.HTTP_BAD_GATEWAY

        try {
            val result: Int = repository.downloadDataPoints(123L)
        } catch (e: Exception) {
            assert(e is HttpException)
        }
    }

    @Test
    fun downloadDataPointsShouldReturnAnyErrorWhenNotAssignmentMissing() = runBlocking {
        coEvery {
            mockRestApi.downloadDataPoints(
                any(),
                any()
            )
        } coAnswers { throw Exception("error") }

        try {
            val result: Int = repository.downloadDataPoints(123L)
        } catch (e: Throwable) {
            assert(e is Exception && e.message == "error")
        }
    }

    @Test
    fun downloadDataPointsShouldReturnCorrectResultIfSuccess() = runBlocking {
        coEvery { mockRestApi.downloadDataPoints(any(), any()) } returns ApiLocaleResult(
            emptyList(), "", 0, 0, 0, ""
        )
        every { mockDatabaseDataSource.syncDataPoints(any()) } returns 1

        val result: Int = repository.downloadDataPoints(123L)

        assertEquals(1, result)
    }
}
