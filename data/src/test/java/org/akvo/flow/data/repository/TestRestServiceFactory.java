package org.akvo.flow.data.repository;

import org.akvo.flow.data.net.RestServiceFactory;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class TestRestServiceFactory extends RestServiceFactory {

    private final Retrofit retrofit;

    public TestRestServiceFactory() {
        super(null, null);
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(2, TimeUnit.SECONDS) // For testing purposes
                .readTimeout(2, TimeUnit.SECONDS) // For testing purposes
                .writeTimeout(2, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:8080/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    @Override
    public <T> T createRetrofitService(final Class<T> clazz, String baseUrl) {
        return retrofit.create(clazz);
    }
}
