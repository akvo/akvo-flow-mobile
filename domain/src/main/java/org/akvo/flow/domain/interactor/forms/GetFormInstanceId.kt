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

import android.text.TextUtils
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.akvo.flow.domain.entity.User
import org.akvo.flow.domain.exception.CascadeResourceMissing
import org.akvo.flow.domain.exception.UserNotFound
import org.akvo.flow.domain.executor.PostExecutionThread
import org.akvo.flow.domain.executor.ThreadExecutor
import org.akvo.flow.domain.repository.SurveyRepository
import org.akvo.flow.domain.repository.UserRepository
import org.akvo.flow.domain.util.Constants
import javax.inject.Inject

class GetFormInstanceId @Inject constructor(
    private val surveyRepository: SurveyRepository,
    private val userRepository: UserRepository,
    private val threadExecutor: ThreadExecutor,
    private val postExecutionThread: PostExecutionThread
) {

    private val disposables = CompositeDisposable()

    fun execute(observer: DisposableSingleObserver<Long>, parameters: Map<String, Any>) {
        val observable: Single<Long> = buildUseCaseObservable(parameters)
            .subscribeOn(Schedulers.from(threadExecutor))
            .observeOn(postExecutionThread.scheduler)
        addDisposable(observable.subscribeWith(observer))
    }

    fun dispose() {
        if (!disposables.isDisposed) {
            disposables.clear()
        }
    }

    private fun <T> buildUseCaseObservable(parameters: Map<String, T>): Single<Long> {
        if (!parameters.containsKey(PARAM_FORM_ID)) {
            return Single.error(IllegalArgumentException("Missing form id"))
        }
        val formId = parameters[PARAM_FORM_ID] as String
        val datapointId = parameters[PARAM_DATAPOINT_ID] as String
        return surveyRepository.getFormMeta(formId).flatMap {
            when {
                !it.first -> {
                    Single.error(CascadeResourceMissing())
                }
                else -> {
                    getUser(formId, datapointId, it.second)
                }
            }
        }
    }

    private fun getUser(formId: String, datapointId: String, formVersion: String): Single<Long> {
        return userRepository.selectedUser.firstOrError().flatMap { userId ->
            when {
                (userId == Constants.INVALID_USER_ID) -> {
                    Single.error(UserNotFound())
                }
                else -> {
                    surveyRepository.getUser(userId).firstOrError()
                        .flatMap { user ->
                            when {
                                TextUtils.isEmpty(user.name) -> {
                                    Single.error(UserNotFound())
                                }
                                else -> {
                                    getFormInstance(formId, datapointId, formVersion, user)
                                }
                            }

                        }
                }
            }
        }
    }

    private fun getFormInstance(formId: String, datapointId: String, formVersion: String, user: User): Single<Long> {
        return surveyRepository.fetchSurveyInstance(formId, datapointId, formVersion, user.id, user.name)
    }

    private fun addDisposable(disposable: Disposable) {
        disposables.add(disposable)
    }

    companion object {
        const val PARAM_FORM_ID = "form_id"
        const val PARAM_DATAPOINT_ID = "datapoint_id"
    }
}
