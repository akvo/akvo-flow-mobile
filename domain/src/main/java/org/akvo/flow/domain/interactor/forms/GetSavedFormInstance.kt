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

package org.akvo.flow.domain.interactor.forms

import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import org.akvo.flow.domain.executor.PostExecutionThread
import org.akvo.flow.domain.executor.SchedulerCreator
import org.akvo.flow.domain.repository.FormInstanceRepository
import javax.inject.Inject

class GetSavedFormInstance @Inject constructor(
    private val formInstanceRepository: FormInstanceRepository,
    private val postExecutionThread: PostExecutionThread,
    private val schedulerCreator: SchedulerCreator
) {

    private val disposables = CompositeDisposable()

    fun execute(observer: DisposableSingleObserver<Long>, parameters: Map<String, Any>) {
        val observable: Single<Long> = buildUseCaseObservable(parameters)
            .subscribeOn(schedulerCreator.obtainScheduler())
            .observeOn(postExecutionThread.scheduler)
        addDisposable(observable.subscribeWith(observer))
    }

    fun dispose() {
        if (!disposables.isDisposed) {
            disposables.clear()
        }
    }

    private fun <T> buildUseCaseObservable(parameters: Map<String, T>): Single<Long> {
        if (!parameters.containsKey(PARAM_FORM_ID) || !parameters.containsKey(PARAM_DATAPOINT_ID) ) {
            return Single.error(IllegalArgumentException("Missing form id"))
        }
        val formId = parameters[PARAM_FORM_ID] as String
        val datapointId = parameters[PARAM_DATAPOINT_ID] as String
        return formInstanceRepository.getSavedFormInstance(formId, datapointId)
    }

    private fun addDisposable(disposable: Disposable) {
        disposables.add(disposable)
    }

    companion object {
        const val PARAM_FORM_ID = "form_id"
        const val PARAM_DATAPOINT_ID = "datapoint_id"
    }
}