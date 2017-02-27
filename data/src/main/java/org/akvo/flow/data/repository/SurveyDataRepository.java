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
import org.akvo.flow.domain.repository.SurveyRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

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
        return getServerBaseUrl().concatMap(new Func1<String, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(final String serverBaseUrl) {
                return dataSourceFactory.getPropertiesDataSource().getApiKey().concatMap(
                                        new Func1<String, Observable<Integer>>() {
                                            @Override
                                            public Observable<Integer> call(String apiKey) {
                                                return syncDataPoints(serverBaseUrl, apiKey,
                                                        surveyGroupId);
                                            }
                                        });
                            }
                        });
    }

    private String getSyncedTime(long surveyGroupId) {
        return syncedTimeMapper
                .getTime(dataSourceFactory.getDataBaseDataSource().getSyncedTime(surveyGroupId));
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

    private Observable<Integer> syncDataPoints(String baseUrl, String apiKey, long surveyGroupId) {
        final List<ApiDataPoint> lastBatch = new ArrayList<>();
        final List<ApiDataPoint> allSyncedDataPoints = new ArrayList<>();
        Timber.d("syncDataPoints");
        return restApi
                .loadNewDataPoints(baseUrl, apiKey, surveyGroupId, getSyncedTime(surveyGroupId))
                .concatMap(new Func1<ApiLocaleResult, Observable<List<ApiDataPoint>>>() {
                    @Override
                    public Observable<List<ApiDataPoint>> call(ApiLocaleResult apiLocaleResult) {
                        List<ApiDataPoint> dataPoints = apiLocaleResult.getDataPoints();
                        Timber.d("syncDataPoints received %d with count %d", dataPoints.size(),
                                apiLocaleResult.getResultCount());
                        dataPoints.removeAll(lastBatch); //remove duplicates
                        Timber.d("syncDataPoints after removing batch %d", dataPoints.size());
                        Timber.d("concatMap: will sync with database ");
                        return dataSourceFactory.getDataBaseDataSource()
                                .syncSurveyedLocales(dataPoints);
                    }
                })
                .repeat()
                .takeWhile(new Func1<List<ApiDataPoint>, Boolean>() {
                    @Override
                    public Boolean call(List<ApiDataPoint> apiDataPoints) {
                        Timber.d("takeWhile: syncDataPoints %d", apiDataPoints.size());
                        boolean done = !apiDataPoints.isEmpty();
                        if (done) {
                            Timber.d("Will load more");
                        } else {
                            Timber.d("Done loading");
                        }
                        allSyncedDataPoints.addAll(apiDataPoints);
                        lastBatch.clear();
                        lastBatch.addAll(apiDataPoints);
                        return done;
                    }
                })
                .map(new Func1<List<ApiDataPoint>, Integer>() {
                    @Override
                    public Integer call(List<ApiDataPoint> apiDataPoints) {
                        int size = allSyncedDataPoints.size();
                        Timber.d("Will notify ui about new %d datapoints", size);
                        return size;
                    }
                });
    }

}
