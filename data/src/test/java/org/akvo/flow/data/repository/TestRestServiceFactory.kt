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
 *
 */
package org.akvo.flow.data.repository

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.akvo.flow.data.net.RestServiceFactory
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class TestRestServiceFactory : RestServiceFactory(null, null) {
    private val retrofit: Retrofit
    private val retrofitScalar: Retrofit
    override fun <T> createRetrofitService(clazz: Class<T>, baseUrl: String): T {
        return retrofit.create(clazz)
    }

    override fun <T> createScalarsRetrofitService(clazz: Class<T>, baseUrl: String): T {
        return retrofitScalar.create(clazz)
    }

    override fun <T> createRetrofitServiceWithInterceptor(clazz: Class<T>?, baseUrl: String?): T {
        return retrofit.create(clazz)
    }

    init {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(2, TimeUnit.SECONDS) // For testing purposes
            .readTimeout(2, TimeUnit.SECONDS) // For testing purposes
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()
        retrofit = Retrofit.Builder()
            .baseUrl("http://localhost:8080/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        retrofitScalar = Retrofit.Builder()
            .baseUrl("http://localhost:8080/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(okHttpClient)
            .build()
    }
}