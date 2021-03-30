/*
 * Copyright (C) 2020,2021 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.interactor.datapoints

import org.akvo.flow.domain.repository.DataPointRepository
import javax.inject.Inject

class MarkDatapointViewed @Inject constructor(
    private val dataPointRepository: DataPointRepository
) {

    fun execute(parameters: Map<String?, Any>?) {
        if (parameters == null || !parameters.containsKey(PARAM_DATAPOINT_ID)) {
            throw IllegalArgumentException("Missing file name")
        }
        val dataPointId = parameters[PARAM_DATAPOINT_ID] as String
        dataPointRepository.markDataPointAsViewed(dataPointId)
    }

    companion object {
        const val PARAM_DATAPOINT_ID = "data_point_id"
    }
}
