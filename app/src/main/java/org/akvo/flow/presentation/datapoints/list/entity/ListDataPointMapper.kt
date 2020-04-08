/*
 * Copyright (C) 2017,2019-2020 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.presentation.datapoints.list.entity

import org.akvo.flow.domain.entity.DataPoint
import org.akvo.flow.presentation.datapoints.DisplayNameMapper
import java.util.ArrayList
import javax.inject.Inject

class ListDataPointMapper @Inject constructor(private val displayNameMapper: DisplayNameMapper, private val dateMapper: DateMapper) {
    private fun transform(dataPoint: DataPoint?): ListDataPoint? {
        if (dataPoint == null) {
            return null
        }
        return ListDataPoint(
            displayNameMapper.createDisplayName(dataPoint.name),
            dataPoint.status,
            dataPoint.id,
            formatCoordinate(dataPoint.latitude),
            formatCoordinate(dataPoint.longitude),
            dateMapper.formatDate(dataPoint.lastModified),
            dataPoint.wasViewed()
        )
    }

    private fun formatCoordinate(coordinate: Double?): Double {
        return coordinate ?: ListDataPoint.INVALID_COORDINATE
    }

    fun transform(dataPoints: List<DataPoint?>?): List<ListDataPoint> {
        if (dataPoints == null) {
            return emptyList()
        }
        val listDataPoints: MutableList<ListDataPoint> = ArrayList(dataPoints.size)
        for (dataPoint in dataPoints) {
            transform(dataPoint)?.let { listDataPoint ->
                listDataPoints.add(listDataPoint)
            }
        }
        return listDataPoints
    }
}
