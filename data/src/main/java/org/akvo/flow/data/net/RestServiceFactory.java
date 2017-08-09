/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Singleton
public class RestServiceFactory {

    private static final int CONNECTION_TIMEOUT = 10;
    /**
     * Requests to GAE take a long time especially when there are a lot of datapoints
     */
    public static final int NO_TIMEOUT = 0;

    private final HttpLoggingInterceptor loggingInterceptor;
    private final SimpleDateFormat dateFormat;
    private final Encoder encoder;

    @Inject
    public RestServiceFactory(HttpLoggingInterceptor loggingInterceptor,
            SimpleDateFormat simpleDateFormat, Encoder encoder) {
        this.loggingInterceptor = loggingInterceptor;
        this.dateFormat = simpleDateFormat;
        this.encoder = encoder;
    }

    public <T> T createRetrofitService(@NonNull String baseUrl, final Class<T> clazz, String key) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(loggingInterceptor);
        httpClient.connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        httpClient.readTimeout(NO_TIMEOUT, TimeUnit.SECONDS);
        httpClient.addInterceptor(new HMACInterceptor(key, dateFormat, encoder));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        return retrofit.create(clazz);
    }
}
