/*
 * Copyright (C) 2020 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.presentation.datapoints.list.entity

import android.location.Location
import org.akvo.flow.domain.entity.DataPoint
import java.text.DecimalFormat
import javax.inject.Inject

class DistanceMapper @Inject constructor() {

    fun mapDistance(latitude: Double?, longitude: Double?, dataPoint: DataPoint): String {
        if (latitude != null && longitude != null && dataPoint.latitude != null && dataPoint.longitude != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                latitude,
                longitude,
                dataPoint.latitude,
                dataPoint.longitude,
                results
            )
            val distance = results[0].toDouble()
            return getDisplayLength(distance)
        }
        return ""
    }

    private fun getDisplayLength(distance: Double): String {
        // default: km
        var unit = "km"
        var factor = 0.001 // convert from meters to km

        // for distances smaller than 1 km, use meters as unit
        if (distance < 1000.0) {
            factor = 1.0
            unit = "m"
        }
        val df = DecimalFormat("### $unit")
        val dist = distance * factor
        return df.format(dist)
    }
}