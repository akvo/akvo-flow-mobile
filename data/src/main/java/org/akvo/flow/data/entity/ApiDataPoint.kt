/*
 * Copyright (C) 2017,2020 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.data.entity

import com.squareup.moshi.Json

data class ApiDataPoint(
    @Json(name = "displayName") @JvmField val displayName: String = "",
    @Json(name = "id") @JvmField val id: String? = null,
    @Json(name = "lat") @JvmField val latitude: Double? = null,
    @Json(name = "lon") @JvmField val longitude: Double? = null,
    @Json(name = "surveyGroupId") @JvmField val surveyGroupId: Long = 0,
    @Json(name = "lastUpdateDateTime") @JvmField val lastModified: Long = 0,
    @Json(name = "surveyInstances") @JvmField val surveyInstances: List<ApiSurveyInstance> = emptyList()
)
        