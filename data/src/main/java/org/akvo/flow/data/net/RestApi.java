/*
 * Copyright (C) 2017-2020 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.data.entity.ApiApkData;
import org.akvo.flow.data.entity.ApiFilesResult;
import org.akvo.flow.data.entity.ApiLocaleResult;
import org.akvo.flow.data.net.gae.DataPointDownloadService;
import org.akvo.flow.data.net.gae.DeviceFilesService;
import org.akvo.flow.data.net.gae.FlowApiService;
import org.akvo.flow.data.net.gae.ProcessingNotificationService;
import org.akvo.flow.domain.util.DeviceHelper;

import java.util.List;

import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;

@Singleton
public class RestApi {
    private final String androidId;
    private final String imei;
    private final String phoneNumber;
    private final RestServiceFactory serviceFactory;
    private final String version;
    private final String baseUrl;

    public RestApi(DeviceHelper deviceHelper, RestServiceFactory serviceFactory, String version,
            String baseUrl) {
        this.androidId = deviceHelper.getAndroidId();
        this.imei = deviceHelper.getImei();
        this.phoneNumber = deviceHelper.getPhoneNumber();
        this.serviceFactory = serviceFactory;
        this.version = version;
        this.baseUrl = baseUrl;
    }

    @SuppressWarnings("unchecked")
    public Single<ApiLocaleResult> downloadDataPoints(long surveyId) {

        return serviceFactory.createRetrofitServiceWithInterceptor(DataPointDownloadService.class,
                baseUrl).getAssignedDataPoints(androidId, surveyId + "")
                .onErrorResumeNext(new ErrorLoggerFunction(
                        "Error downloading datapoints for survey: " + surveyId));
    }

    @SuppressWarnings("unchecked")
    public Observable<ApiFilesResult> getPendingFiles(List<String> formIds, String deviceId) {
        return serviceFactory.createRetrofitService(DeviceFilesService.class, baseUrl)
                .getFilesLists(phoneNumber, androidId, imei, version, deviceId, formIds)
                .onErrorResumeNext(new ErrorLoggerFunction(
                        "Error getting device pending files"));
    }

    @SuppressWarnings("unchecked")
    public Observable<?> notifyFileAvailable(String action, String formId,
            String filename, String deviceId) {
        return serviceFactory
                .createRetrofitService(ProcessingNotificationService.class, baseUrl)
                .notifyFileAvailable(action, formId, filename, phoneNumber, androidId, imei,
                        version, deviceId)
                .onErrorResumeNext(new ErrorLoggerFunction(
                        "Error notifying the file is available"));
    }

    @SuppressWarnings("unchecked")
    public Observable<ApiApkData> loadApkData(String appVersion) {
        return serviceFactory.createRetrofitService(FlowApiService.class, baseUrl)
                .loadApkData(appVersion)
                .onErrorResumeNext(new ErrorLoggerFunction(
                        "Error downloading apk data for version " + appVersion));
    }

    @SuppressWarnings("unchecked")
    public Observable<String> downloadFormHeader(String formId, String deviceId) {
        return serviceFactory
                .createScalarsRetrofitService(FlowApiService.class, baseUrl)
                .downloadFormHeader(formId, phoneNumber, androidId, imei, version, deviceId)
                .onErrorResumeNext(new ErrorLoggerFunction(
                        "Error downloading form " + formId + " header"));
    }

    @SuppressWarnings("unchecked")
    public Observable<String> downloadFormsHeader(String deviceId) {
        return serviceFactory
                .createScalarsRetrofitService(FlowApiService.class, baseUrl)
                .downloadFormsHeader(phoneNumber, androidId, imei, version, deviceId)
                .onErrorResumeNext(new ErrorLoggerFunction("Error downloading all form headers"));
    }
}
