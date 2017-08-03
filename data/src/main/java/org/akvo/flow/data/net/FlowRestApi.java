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
import android.text.TextUtils;

import org.akvo.flow.data.entity.ApiApkData;
import org.akvo.flow.data.entity.ApiLocaleResult;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;

@Singleton
public class FlowRestApi {
    private final String androidId;
    private final String imei;
    private final String phoneNumber;
    private final RestServiceFactory serviceFactory;
    private final Encoder encoder;

    @Inject
    public FlowRestApi(DeviceHelper deviceHelper, RestServiceFactory serviceFactory,
            Encoder encoder) {
        this.androidId = deviceHelper.getAndroidId();
        this.imei = deviceHelper.getImei();
        this.phoneNumber = deviceHelper.getPhoneNumber();
        this.serviceFactory = serviceFactory;
        this.encoder = encoder;
    }

    public Observable<ApiLocaleResult> loadNewDataPoints(@NonNull String baseUrl,
            @NonNull String apiKey, long surveyGroup, @NonNull String timestamp) {
        String lastUpdated = !TextUtils.isEmpty(timestamp) ? timestamp : "0";
        String phoneNumber = encoder.encodeParam(this.phoneNumber);
        return serviceFactory.createRetrofitService(baseUrl, DataPointSyncService.class, apiKey)
                .loadNewDataPoints(androidId, imei, lastUpdated, phoneNumber, surveyGroup + "");
    }

    public Observable<ApiApkData> loadApkData(@NonNull String baseUrl) {
        return serviceFactory.createRetrofitService(baseUrl, FlowApiService.class)
                .loadApkData();
    }
}
