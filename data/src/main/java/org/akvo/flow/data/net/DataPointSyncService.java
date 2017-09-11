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

import org.akvo.flow.data.entity.ApiLocaleResult;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

import static org.akvo.flow.data.util.Constants.ANDROID_ID;
import static org.akvo.flow.data.util.Constants.IMEI;
import static org.akvo.flow.data.util.Constants.LAST_UPDATED;
import static org.akvo.flow.data.util.Constants.PHONE_NUMBER;
import static org.akvo.flow.data.util.Constants.SURVEYED_LOCALE;
import static org.akvo.flow.data.util.Constants.SURVEY_GROUP;

interface DataPointSyncService {

    @GET(SURVEYED_LOCALE)
    @Headers("Cache-Control: no-cache")
    Observable<ApiLocaleResult> loadNewDataPoints(@Query(ANDROID_ID) String androidId,
            @Query(IMEI) String imei, @Query(LAST_UPDATED) String lastUpdated,
            @Query(PHONE_NUMBER) String phoneNumber, @Query(SURVEY_GROUP) String surveyGroup);
}
