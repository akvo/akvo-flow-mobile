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
package org.akvo.flow.domain.interactor

import io.reactivex.Observable
import org.akvo.flow.domain.executor.PostExecutionThread
import org.akvo.flow.domain.executor.ThreadExecutor
import org.akvo.flow.domain.repository.SurveyRepository
import org.akvo.flow.domain.repository.UserRepository
import javax.inject.Inject

class SaveSelectedSurvey @Inject constructor(
    threadExecutor: ThreadExecutor?,
    postExecutionThread: PostExecutionThread?,
    private val userRepository: UserRepository,
    private val surveyRepository: SurveyRepository
) : UseCase(threadExecutor, postExecutionThread) {
    override fun <T> buildUseCaseObservable(parameters: Map<String, T>): Observable<*> {
        if (parameters[KEY_SURVEY_ID] == null) {
            return Observable.error<Any>(IllegalArgumentException("Missing survey group id"))
        }
        val surveyId = parameters[KEY_SURVEY_ID] as Long?
        return userRepository.setSelectedSurvey(surveyId!!)
            .concatMap { result: Boolean ->
                surveyRepository.setSurveyViewed(surveyId)
                Observable.just(result)
            }
    }

    companion object {
        const val KEY_SURVEY_ID = "survey_id"
    }
}
