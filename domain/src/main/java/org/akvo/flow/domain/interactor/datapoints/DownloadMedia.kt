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

package org.akvo.flow.domain.interactor.datapoints

import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.schedulers.Schedulers
import org.akvo.flow.domain.executor.PostExecutionThread
import org.akvo.flow.domain.executor.ThreadExecutor
import org.akvo.flow.domain.repository.DataPointRepository
import javax.inject.Inject

class DownloadMedia @Inject constructor(
    private val dataPointRepository: DataPointRepository,
    private val threadExecutor: ThreadExecutor,
    private val postExecutionThread: PostExecutionThread

) {

    private val disposables = CompositeDisposable()

    fun execute(observer: DisposableCompletableObserver, parameters: Map<String?, Any>?) {
        val observable: Completable = buildUseCaseObservable(parameters)
            .subscribeOn(Schedulers.from(threadExecutor))
            .observeOn(postExecutionThread.scheduler)
        addDisposable(observable.subscribeWith(observer))
    }

    fun dispose() {
        if (!disposables.isDisposed) {
            disposables.clear()
        }
    }

    private fun <T> buildUseCaseObservable(parameters: Map<String?, T>?): Completable {
        if (parameters == null || !parameters.containsKey(PARAM_FILE_PATH)) {
            return Completable.error(IllegalArgumentException("Missing file name"))
        }
        val filePath = parameters[PARAM_FILE_PATH] as String
        return dataPointRepository.cleanPathAndDownLoadMedia(filePath)
    }

    private fun addDisposable(disposable: Disposable) {
        disposables.add(disposable)
    }

    companion object {
        const val PARAM_FILE_PATH = "data_point_id"
    }
}
