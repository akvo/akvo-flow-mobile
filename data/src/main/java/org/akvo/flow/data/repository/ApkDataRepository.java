/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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

import android.support.annotation.NonNull;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.entity.ApiApkData;
import org.akvo.flow.data.entity.ApkDataMapper;
import org.akvo.flow.domain.entity.ApkData;
import org.akvo.flow.domain.repository.ApkRepository;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

public class ApkDataRepository implements ApkRepository {

    private final DataSourceFactory dataSourceFactory;
    private final ApkDataMapper mapper;

    @Inject
    public ApkDataRepository(DataSourceFactory dataSourceFactory, ApkDataMapper mapper) {
        this.dataSourceFactory = dataSourceFactory;
        this.mapper = mapper;
    }

    @Override
    public Observable<ApkData> loadApkData(@NonNull String baseUrl) {
        return dataSourceFactory.createNetworkDataSource().getApkData(baseUrl)
                .map(new Func1<ApiApkData, ApkData>() {
                    @Override
                    public ApkData call(ApiApkData apiApkData) {
                        return mapper.transform(apiApkData);
                    }
                });
    }
}
