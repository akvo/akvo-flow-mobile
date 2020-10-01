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
package org.akvo.flow.data.net

import io.reactivex.Observable
import org.akvo.flow.data.entity.ApiApkData
import org.akvo.flow.data.entity.ApiFilesResult
import org.akvo.flow.data.entity.ApiLocaleResult
import org.akvo.flow.data.net.gae.DataPointDownloadService
import org.akvo.flow.data.net.gae.DeviceFilesService
import org.akvo.flow.data.net.gae.FlowApiService
import org.akvo.flow.data.net.gae.ProcessingNotificationService
import org.akvo.flow.domain.util.DeviceHelper
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class RestApi(
    private val deviceHelper: DeviceHelper,
    private val serviceFactory: RestServiceFactory,
    private val version: String,
    private val baseUrl: String
) {
    suspend fun downloadDataPoints(surveyId: Long, cursor: String? = null): ApiLocaleResult {
        return serviceFactory.createRetrofitServiceWithInterceptor(
            DataPointDownloadService::class.java,
            baseUrl
        )
            .getAssignedDataPointsV2(deviceHelper.androidId, cursor, surveyId.toString() + "")
    }

    fun getPendingFiles(formIds: List<String?>?, deviceId: String?): Observable<ApiFilesResult> {
        return serviceFactory.createRetrofitService(DeviceFilesService::class.java, baseUrl)
            .getFilesLists(
                deviceHelper.phoneNumber,
                deviceHelper.androidId,
                deviceHelper.imei,
                version,
                deviceId,
                formIds
            )
            .doOnError { t -> Timber.e(Exception(t), "Error getting device pending files") }
    }

    fun notifyFileAvailable(
        action: String?, formId: String?,
        filename: String?, deviceId: String?
    ): Observable<*> {
        return serviceFactory
            .createRetrofitService(ProcessingNotificationService::class.java, baseUrl)
            .notifyFileAvailable(
                action, formId, filename, deviceHelper.phoneNumber,
                deviceHelper.androidId, deviceHelper.imei, version, deviceId
            )
            .doOnError { t ->
                Timber.e(Exception(t), "Error notifying the file is available")
            }
    }

    fun loadApkData(appVersion: String): Observable<ApiApkData> {
        return serviceFactory.createRetrofitService(FlowApiService::class.java, baseUrl)
            .loadApkData(appVersion)
            .doOnError { t ->
                Timber.e(Exception(t), "Error downloading apk data for version $appVersion")
            }
    }

    fun downloadFormHeader(formId: String, deviceId: String?): Observable<String> {
        return serviceFactory
            .createScalarsRetrofitService(FlowApiService::class.java, baseUrl)
            .downloadFormHeader(
                formId,
                deviceHelper.phoneNumber,
                deviceHelper.androidId,
                deviceHelper.imei,
                version,
                deviceId
            )
            .doOnError { t ->
                Timber.e(Exception(t), "Error downloading form $formId header")
            }
    }

    fun downloadFormsHeader(deviceId: String?): Observable<String> {
        return serviceFactory
            .createScalarsRetrofitService(FlowApiService::class.java, baseUrl)
            .downloadFormsHeader(
                deviceHelper.phoneNumber,
                deviceHelper.androidId,
                deviceHelper.imei,
                version,
                deviceId
            )
            .doOnError { t ->
                Timber.e(Exception(t), "Error downloading all form headers")
            }
    }
}
