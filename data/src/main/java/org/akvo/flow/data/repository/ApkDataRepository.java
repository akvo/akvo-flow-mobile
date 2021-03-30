/*
 * Copyright (C) 2016-2018 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.data.repository;

import androidx.annotation.NonNull;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.entity.ApiApkData;
import org.akvo.flow.data.entity.ApkDataMapper;
import org.akvo.flow.data.net.RestApi;
import org.akvo.flow.domain.entity.ApkData;
import org.akvo.flow.domain.repository.ApkRepository;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class ApkDataRepository implements ApkRepository {

    private final DataSourceFactory dataSourceFactory;
    private final RestApi restApi;
    private final ApkDataMapper mapper;

    @Inject
    public ApkDataRepository(DataSourceFactory dataSourceFactory, RestApi restApi,
            ApkDataMapper mapper) {
        this.dataSourceFactory = dataSourceFactory;
        this.restApi = restApi;
        this.mapper = mapper;
    }

    @Override
    public Observable<ApkData> loadApkData(String androidVersion) {
        return restApi.loadApkData(androidVersion)
                .map(new Function<ApiApkData, ApkData>() {
                    @Override
                    public ApkData apply(ApiApkData apiApkData) {
                        return mapper.transform(apiApkData);
                    }
                });
    }

    @Override
    public Observable<Boolean> saveApkDataPreference(@NonNull ApkData apkData) {
        dataSourceFactory.getSharedPreferencesDataSource().setApkData(apkData);
        return Observable.just(true);
    }

    @Override
    public ApkData getApkDataPreference() {
        return dataSourceFactory.getSharedPreferencesDataSource().getApkData();
    }
}
