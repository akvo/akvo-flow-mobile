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

package org.akvo.flow.data.net.s3

import io.reactivex.observers.TestObserver
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.akvo.flow.BuildConfig
import org.akvo.flow.data.net.RestServiceFactory
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class S3RestApiTest {

    private lateinit var serviceFactory: RestServiceFactory

    @Before
    fun setUp() {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val httpClient2: OkHttpClient.Builder = createHttpClient(loggingInterceptor)
        val okHttpClient = httpClient2.build()

        serviceFactory = RestServiceFactory(null, okHttpClient)
    }

    @Test
    @Ignore
    fun shouldDownloadImageCorrectly() {
        val s3RestApi = S3RestApi(serviceFactory, baseUrl(), "uat1")

        val observer = TestObserver<ResponseBody>()
        s3RestApi.downloadMedia("6af199a2-a507-4def-ad97-b81f944c9929.jpg").subscribe(observer)

        observer.assertNoErrors()
    }

    @Test
    @Ignore
    fun shouldDownloadFolderCorrectly() {
        val s3RestApi = S3RestApi(serviceFactory, baseUrl(), "uat1")

        val observer = TestObserver<ResponseBody>()
        s3RestApi.downloadArchive("10029122.zip").subscribe(observer)

        observer.assertNoErrors()
    }

    private fun baseUrl() = BuildConfig.S3_PROXY_URL

    private fun createHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient.Builder {
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(loggingInterceptor)
        httpClient.connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS));
        return httpClient
    }
}
