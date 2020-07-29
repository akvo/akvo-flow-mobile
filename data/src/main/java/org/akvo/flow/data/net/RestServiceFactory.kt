/*
 * Copyright (C) 2017-2018,2020 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.data.net

import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class RestServiceFactory @Inject constructor(
    private val okHttpClientWithHmac: OkHttpClient,
    private val okHttpClient: OkHttpClient
) {
    fun <T> createRetrofitServiceWithInterceptor(
        clazz: Class<T>,
        baseUrl: String
    ): T {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttpClientWithHmac)
            .build().create(clazz)
    }

    open fun <T> createRetrofitService(clazz: Class<T>, baseUrl: String): T {
        return createRetrofit(clazz, baseUrl, okHttpClient, GsonConverterFactory.create())
    }

    open fun <T> createScalarsRetrofitService(
        clazz: Class<T>,
        baseUrl: String
    ): T {
        return createRetrofit(clazz, baseUrl, okHttpClient, ScalarsConverterFactory.create())
    }

    private fun <T> createRetrofit(
        clazz: Class<T>, baseUrl: String, okHttpClient: OkHttpClient,
        converter: Converter.Factory
    ): T {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(converter)
            .client(okHttpClient)
            .build()
        return retrofit.create(clazz)
    }
}
