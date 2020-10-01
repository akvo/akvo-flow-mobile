/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.data.entity.ApiLocaleResult
import org.akvo.flow.data.util.ApiUrls
import retrofit2.http.GET
import retrofit2.http.Query

interface DataPointDownloadService {

    /**
     * params need to be in alphabetic order
     */
    @GET(ApiUrls.DATA_POINTS)
    suspend fun getAssignedDataPoints(
        @Query(ApiUrls.ANDROID_ID) androidId: String,
        @Query(ApiUrls.CURSOR) cursor: String? = null,
        @Query(ApiUrls.SURVEY_ID) surveyId: String
    ): ApiLocaleResult

    /**
     * params need to be in alphabetic order
     */
    @GET(ApiUrls.DATA_POINTS_V2)
    suspend fun getAssignedDataPointsV2(
        @Query(ApiUrls.ANDROID_ID) androidId: String,
        @Query(ApiUrls.CURSOR) cursor: String? = null,
        @Query(ApiUrls.SURVEY_ID) surveyId: String
    ): ApiLocaleResult
}
