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

package org.akvo.flow.data.entity.images

import org.akvo.flow.data.entity.ApiDataPoint
import org.akvo.flow.data.entity.ApiSurveyInstance
import org.akvo.flow.data.util.MediaHelper
import javax.inject.Inject

class DataPointImageMapper @Inject constructor(
    private val mediaHelper: MediaHelper
) {

    fun getImagesList(
        dataPoints: List<ApiDataPoint>,
        assignedFormIds: MutableList<String>
    ): List<String> {
        val images = mutableListOf<String>()
        dataPoints.forEach { dataPoint ->
            dataPoint.surveyInstances.forEach { surveyInstance ->
                addImageIfFormAssigned(surveyInstance, images, assignedFormIds)
            }
        }
        return images
    }

    private fun addImageIfFormAssigned(
        surveyInstance: ApiSurveyInstance,
        images: MutableList<String>,
        assignedFormIds: MutableList<String>
    ) {
        if (assignedFormIds.contains(surveyInstance.surveyId.toString())) {
            surveyInstance.qasList.forEach { questionAnswer ->
                if (!questionAnswer.answer.isNullOrBlank() && ("IMAGE" == questionAnswer.type)) {
                    images.add(mediaHelper.cleanMediaFileName(questionAnswer.answer))
                }
            }
        }
    }
}
