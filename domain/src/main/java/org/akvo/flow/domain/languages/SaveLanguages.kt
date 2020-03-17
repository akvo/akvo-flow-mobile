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

package org.akvo.flow.domain.languages

import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableCompletableObserver
import org.akvo.flow.domain.executor.PostExecutionThread
import org.akvo.flow.domain.executor.SchedulerCreator
import org.akvo.flow.domain.repository.LanguagesRepository
import javax.inject.Inject

class SaveLanguages @Inject constructor(
    private val postExecutionThread: PostExecutionThread,
    private val schedulerCreator: SchedulerCreator,
    private val languagesRepository: LanguagesRepository
) {

    private val disposables = CompositeDisposable()

    fun execute(observer: DisposableCompletableObserver, parameters: Map<String, Any>) {
        val observable: Completable = buildUseCaseObservable(parameters)
            .subscribeOn(schedulerCreator.obtainScheduler())
            .observeOn(postExecutionThread.scheduler)
        addDisposable(observable.subscribeWith(observer))
    }

    fun dispose() {
        if (!disposables.isDisposed) {
            disposables.clear()
        }
    }

    private fun <T> buildUseCaseObservable(parameters: Map<String, T>): Completable {
        if (!parameters.containsKey(PARAM_SURVEY_ID) || !parameters.containsKey(PARAM_LANGUAGES_LIST)) {
            return Completable.error(IllegalArgumentException("Missing survey id or selected languages"))
        }
        val surveyId = parameters[PARAM_SURVEY_ID] as Long
        val languages = parameters[PARAM_LANGUAGES_LIST] as Set<String>
        return languagesRepository.saveLanguages(surveyId, languages)
    }

    private fun addDisposable(disposable: Disposable) {
        disposables.add(disposable)
    }

    companion object {
        const val PARAM_SURVEY_ID = "survey_id"
        const val PARAM_LANGUAGES_LIST = "languages"
    }
}
