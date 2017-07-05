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

package org.akvo.flow.data.repository;

import android.database.Cursor;
import android.text.TextUtils;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.entity.ApiDataPoint;
import org.akvo.flow.data.entity.ApiLocaleResult;
import org.akvo.flow.data.entity.DataPointMapper;
import org.akvo.flow.data.entity.SyncedTimeMapper;
import org.akvo.flow.data.net.FlowRestApi;
import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.exception.AssignmentRequiredException;
import org.akvo.flow.domain.repository.SurveyRepository;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.HttpException;
import rx.Observable;
import rx.functions.Func1;

public class SurveyDataRepository implements SurveyRepository {

    private final DataSourceFactory dataSourceFactory;
    private final DataPointMapper dataPointMapper;
    private final SyncedTimeMapper syncedTimeMapper;
    private final FlowRestApi restApi;

    @Inject
    public SurveyDataRepository(DataSourceFactory dataSourceFactory,
            DataPointMapper dataPointMapper,
            SyncedTimeMapper syncedTimeMapper, FlowRestApi restApi) {
        this.dataSourceFactory = dataSourceFactory;
        this.dataPointMapper = dataPointMapper;
        this.syncedTimeMapper = syncedTimeMapper;
        this.restApi = restApi;
    }

    @Override
    public Observable<List<DataPoint>> getDataPoints(Long surveyGroupId,
            Double latitude, Double longitude, Integer orderBy) {
        return dataSourceFactory.getDataBaseDataSource()
                .getDataPoints(surveyGroupId, latitude, longitude, orderBy).concatMap(
                        new Func1<Cursor, Observable<List<DataPoint>>>() {
                            @Override
                            public Observable<List<DataPoint>> call(Cursor cursor) {
                                return Observable.just(dataPointMapper.getDataPoints(cursor));
                            }
                        });
    }

    @Override
    public Observable<Integer> syncRemoteDataPoints(final long surveyGroupId) {
        return getServerBaseUrl()
                .concatMap(new Func1<String, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(final String serverBaseUrl) {
                        return dataSourceFactory.getPropertiesDataSource().getApiKey()
                                .concatMap(new Func1<String, Observable<Integer>>() {
                                    @Override
                                    public Observable<Integer> call(String apiKey) {
                                        return syncDataPoints(serverBaseUrl, apiKey, surveyGroupId);
                                    }
                                });
                    }
                })
                .onErrorResumeNext(new Func1<Throwable, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(Throwable throwable) {
                        if (isErrorForbidden(throwable)) {
                            throw new AssignmentRequiredException("Dashboard Assignment missing");
                        } else {
                            return Observable.error(throwable);
                        }
                    }
                });
    }

    private boolean isErrorForbidden(Throwable throwable) {
        return throwable instanceof HttpException
                && ((HttpException) throwable).code() == HttpURLConnection.HTTP_FORBIDDEN;
    }

    private String getSyncedTime(long surveyGroupId) {
        Cursor syncedTime = dataSourceFactory.getDataBaseDataSource().getSyncedTime(surveyGroupId);
        return syncedTimeMapper.getTime(syncedTime);
    }

    private Observable<String> getServerBaseUrl() {
        return dataSourceFactory.getSharedPreferencesDataSource().getBaseUrl().concatMap(
                new Func1<String, Observable<String>>() {
                    @Override
                    public Observable<String> call(String baseUrl) {
                        if (TextUtils.isEmpty(baseUrl)) {
                            return dataSourceFactory.getPropertiesDataSource().getBaseUrl();
                        } else {
                            return Observable.just(baseUrl);
                        }
                    }
                });
    }

    private Observable<Integer> syncDataPoints(final String baseUrl, final String apiKey,
            final long surveyGroupId) {
        final List<ApiDataPoint> lastBatch = new ArrayList<>();
        final List<ApiDataPoint> allResults = new ArrayList<>();
        return loadAndSave(baseUrl, apiKey, surveyGroupId, lastBatch, allResults)
                .repeatWhen(new Func1<Observable<? extends Void>, Observable<?>>() {
                    @Override
                    public Observable<?> call(final Observable<? extends Void> observable) {
                        return observable.delay(5, TimeUnit.SECONDS);
                    }
                })
                .takeUntil(new Func1<List<ApiDataPoint>, Boolean>() {
                    @Override
                    public Boolean call(List<ApiDataPoint> apiDataPoints) {
                        return apiDataPoints.isEmpty();
                    }
                })
                .filter(new Func1<List<ApiDataPoint>, Boolean>() {
                    @Override
                    public Boolean call(List<ApiDataPoint> apiDataPoints) {
                        return apiDataPoints.isEmpty();
                    }
                }).map(new Func1<List<ApiDataPoint>, Integer>() {
                    @Override
                    public Integer call(List<ApiDataPoint> apiDataPoints) {
                        return allResults.size();
                    }
                });
    }

    private Observable<List<ApiDataPoint>> loadAndSave(final String baseUrl, final String apiKey,
            final long surveyGroupId, final List<ApiDataPoint> lastBatch,
            final List<ApiDataPoint> allResults) {
        return loadNewDataPoints(baseUrl, apiKey, surveyGroupId)
                .flatMap(new Func1<ApiLocaleResult, Observable<List<ApiDataPoint>>>() {
                    @Override
                    public Observable<List<ApiDataPoint>> call(ApiLocaleResult apiLocaleResult) {
                        return saveToDataBase(apiLocaleResult, lastBatch, allResults);
                    }
                });
    }

    private Observable<List<ApiDataPoint>> saveToDataBase(ApiLocaleResult apiLocaleResult,
            List<ApiDataPoint> lastBatch, List<ApiDataPoint> allResults) {
        List<ApiDataPoint> dataPoints = apiLocaleResult.getDataPoints();
        dataPoints.removeAll(lastBatch); //remove duplicates
        lastBatch.clear();
        lastBatch.addAll(dataPoints);
        allResults.addAll(dataPoints);
        return dataSourceFactory.getDataBaseDataSource()
                .syncSurveyedLocales(dataPoints);
    }

    private Observable<ApiLocaleResult> loadNewDataPoints(final String baseUrl, final String apiKey,
            final long surveyGroupId) {
        return restApi
                .loadNewDataPoints(baseUrl, apiKey, surveyGroupId, getSyncedTime(surveyGroupId));
    }
}
