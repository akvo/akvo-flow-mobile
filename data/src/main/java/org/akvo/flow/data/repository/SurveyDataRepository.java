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
import org.akvo.flow.data.entity.DataPointMapper;
import org.akvo.flow.data.net.FlowRestApi;
import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.repository.SurveyRepository;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

public class SurveyDataRepository implements SurveyRepository {

    private final DataSourceFactory dataSourceFactory;
    private final DataPointMapper mapper;
    private final FlowRestApi restApi;

    @Inject
    public SurveyDataRepository(DataSourceFactory dataSourceFactory, DataPointMapper mapper,
            FlowRestApi restApi) {
        this.dataSourceFactory = dataSourceFactory;
        this.mapper = mapper;
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
                                return Observable.just(mapper.getDataPoints(cursor));
                            }
                        });
    }

    @Override
    public Observable<Boolean> syncRemoteDataPoints() {
        return getServerBaseUrl().concatMap(new Func1<String, Observable<? extends Boolean>>() {
            @Override public Observable<? extends Boolean> call(String serverBaseUrl) {
                return loadDataPoints(serverBaseUrl);
            }
        });
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

    private Observable<Boolean> loadDataPoints(String baseUrl) {
        return restApi.loadNewDataPoints(baseUrl).concatMap(
                new Func1<List<ApiDataPoint>, Observable<? extends Boolean>>() {
                    @Override
                    public Observable<? extends Boolean> call(List<ApiDataPoint> apiDataPoints) {
                        return null;
                    }
                });
    }

}
