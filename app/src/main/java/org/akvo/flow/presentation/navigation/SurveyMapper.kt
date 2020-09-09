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
package org.akvo.flow.presentation.navigation

import org.akvo.flow.domain.entity.Survey
import java.util.ArrayList
import javax.inject.Inject

class SurveyMapper @Inject constructor() {

    fun transform(surveys: List<Survey>?): List<ViewSurvey> {
        val viewSurveys: MutableList<ViewSurvey> = ArrayList()
        if (surveys != null) {
            for (s in surveys) {
                viewSurveys.add(transform(s))
            }
        }
        return viewSurveys
    }

    private fun transform(survey: Survey): ViewSurvey {
        return ViewSurvey(
            survey.id, survey.name, survey.isMonitored,
            survey.registrationSurveyId
        )
    }
}
