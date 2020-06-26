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

package org.akvo.flow.data.net.gae;

import org.akvo.flow.data.entity.ApiLocaleResult;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

import static org.akvo.flow.data.util.ApiUrls.ANDROID_ID;
import static org.akvo.flow.data.util.ApiUrls.DATA_POINTS;
import static org.akvo.flow.data.util.ApiUrls.LAST_UPDATED;
import static org.akvo.flow.data.util.ApiUrls.SURVEY_ID;

public interface DataPointDownloadService {

    @GET(DATA_POINTS)
    Single<ApiLocaleResult> getAssignedDataPoints(@Query(ANDROID_ID) String androidId,
            @Query(SURVEY_ID) String surveyId, @Query(LAST_UPDATED) String lastUpdated);
}
