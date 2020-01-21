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
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.akvo.flow.BuildConfig
import org.akvo.flow.data.net.RestServiceFactory
import org.akvo.flow.data.net.S3User
import org.akvo.flow.data.net.SignatureHelper
import org.akvo.flow.data.util.ApiUrls
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@RunWith(MockitoJUnitRunner::class)
class S3RestApiTest {

    private lateinit var serviceFactory: RestServiceFactory
    private lateinit var apiUrls: ApiUrls
    private lateinit var amazonAuthHelper: AmazonAuthHelper

    @Before
    fun setUp() {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val httpClient2: OkHttpClient.Builder = createHttpClient(loggingInterceptor)
        val okHttpClient = httpClient2.build()

        serviceFactory = RestServiceFactory(null, okHttpClient)
        apiUrls = ApiUrls("",
            "https://${BuildConfig.AWS_BUCKET}.s3.amazonaws.com"
        )
        val s3User = S3User(
            BuildConfig.AWS_BUCKET,
            BuildConfig.AWS_ACCESS_KEY_ID,
            BuildConfig.AWS_SECRET_KEY
        )
        amazonAuthHelper = AmazonAuthHelper(SignatureHelper(), s3User)
    }

    @Test
    fun shouldDownloadImageCorrectly() {
        val df: DateFormat =
            SimpleDateFormat(REST_API_DATE_PATTERN, Locale.US)
        df.timeZone = TimeZone.getTimeZone("GMT")
        val s3RestApi =
            S3RestApi(serviceFactory, apiUrls, amazonAuthHelper, df, BodyCreator())

        val observer = TestObserver<ResponseBody>()
        s3RestApi.downloadImage("6af199a2-a507-4def-ad97-b81f944c9929.jpg").subscribe(observer)

        observer.assertNoErrors()
    }

    @Test
    fun shouldDownloadFolderCorrectly() {
        val df: DateFormat =
            SimpleDateFormat(REST_API_DATE_PATTERN, Locale.US)
        df.timeZone = TimeZone.getTimeZone("GMT")
        val s3RestApi =
            S3RestApi(serviceFactory, apiUrls, amazonAuthHelper, df, BodyCreator())

        val observer = TestObserver<ResponseBody>()
        s3RestApi.downloadArchive("10029122.zip").subscribe(observer)

        observer.assertNoErrors()
    }

    private fun createHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient.Builder {
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(loggingInterceptor)
        return httpClient
    }

    companion object {
        private const val REST_API_DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss "

    }

}
