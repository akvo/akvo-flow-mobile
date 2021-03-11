/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.data.net.gae

import io.reactivex.Observable
import org.akvo.flow.data.entity.ApiApkData
import org.akvo.flow.data.entity.time.TimeResult
import org.akvo.flow.data.util.ApiUrls
import retrofit2.http.GET
import retrofit2.http.Query

interface FlowApiService {
    @GET(ApiUrls.APK_VERSION_SERVICE_PATH)
    fun loadApkData(@Query(ApiUrls.ANDROID_BUILD_VERSION) version: String?): Observable<ApiApkData>

    @GET(ApiUrls.FORM_HEADER_PATH)
    fun downloadFormHeader(
        @Query(ApiUrls.SURVEY_ID) formId: String?,
        @Query(ApiUrls.PHONE_NUMBER) phoneNumber: String?,
        @Query(ApiUrls.ANDROID_ID) androidId: String?,
        @Query(ApiUrls.IMEI) imei: String?,
        @Query(ApiUrls.VERSION) version: String?,
        @Query(ApiUrls.DEVICE_ID) deviceId: String?
    ): Observable<String>

    @GET(ApiUrls.FORMS_HEADER_PATH)
    fun downloadFormsHeader(
        @Query(ApiUrls.PHONE_NUMBER) phoneNumber: String?,
        @Query(ApiUrls.ANDROID_ID) androidId: String?,
        @Query(ApiUrls.IMEI) imei: String?, @Query(ApiUrls.VERSION) version: String?,
        @Query(ApiUrls.DEVICE_ID) deviceId: String?
    ): Observable<String>

    @GET(ApiUrls.SERVER_TIME_PATH)
    suspend fun fetchServerTime(@Query(ApiUrls.TIMESTAMP) timestamp: String): TimeResult
}