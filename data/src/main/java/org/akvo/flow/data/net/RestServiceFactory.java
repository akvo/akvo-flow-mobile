/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.net;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.OkHttpClient;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

@Singleton
public class RestServiceFactory {

    private final OkHttpClient okHttpClientWithHmac;
    private final OkHttpClient okHttpClient;

    @Inject
    public RestServiceFactory(OkHttpClient okHttpClientWithHmac, OkHttpClient okHttpClient) {
        this.okHttpClientWithHmac = okHttpClientWithHmac;
        this.okHttpClient = okHttpClient;
    }

    public <T> T createRetrofitServiceWithInterceptor(final Class<T> clazz, String baseUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClientWithHmac)
                .build();
        return retrofit.create(clazz);
    }

    public <T> T createSimpleRetrofitService(final Class<T> clazz, String baseUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(clazz);
    }

    public <T> T createRetrofitService(final Class<T> clazz, String baseUrl) {
        return createRetrofit(clazz, baseUrl, okHttpClient, GsonConverterFactory.create());
    }

    public <T> T createScalarsRetrofitService(final Class<T> clazz, String baseUrl) {
        return createRetrofit(clazz, baseUrl, okHttpClient, ScalarsConverterFactory.create());
    }

    private <T> T createRetrofit(Class<T> clazz, String baseUrl, OkHttpClient okHttpClient,
            Converter.Factory converter) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(converter)
                .client(okHttpClient)
                .build();
        return retrofit.create(clazz);
    }
}
