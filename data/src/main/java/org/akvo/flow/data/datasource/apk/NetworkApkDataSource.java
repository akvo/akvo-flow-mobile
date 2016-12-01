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

package org.akvo.flow.data.datasource.apk;


import android.support.annotation.NonNull;

import org.akvo.flow.data.datasource.ApkDataSource;
import org.akvo.flow.data.entity.ApiApkData;
import org.akvo.flow.data.net.FlowRestApi;

import javax.inject.Inject;

import rx.Observable;

public class NetworkApkDataSource implements ApkDataSource {

    private final FlowRestApi restApi;

    @Inject
    public NetworkApkDataSource(FlowRestApi restApi) {
        this.restApi = restApi;
    }

    //TODO: baseUrl should be injected
    @Override
    public Observable<ApiApkData> getApkData(@NonNull String baseUrl) {
        return restApi.loadApkData(baseUrl);
    }
}
