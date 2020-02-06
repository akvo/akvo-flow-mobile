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

package org.akvo.flow.presentation.record

import io.reactivex.observers.DisposableSingleObserver
import org.akvo.flow.domain.exception.CascadeResourceMissing
import org.akvo.flow.domain.interactor.forms.GetFormInstanceId
import org.akvo.flow.presentation.Presenter
import org.akvo.flow.service.BootstrapService
import timber.log.Timber
import javax.inject.Inject

class RecordPresenter @Inject constructor(private val getFormInstanceIdUseCase: GetFormInstanceId) :
    Presenter {

    var view: RecordView? = null

    override fun destroy() {
        getFormInstanceIdUseCase.dispose()
    }

    fun onSurveyClick(formId: String, datapointId: String) {
        if (BootstrapService.isProcessing) {
            view?.showBootStrapPendingError()
            return
        }
        val params: MutableMap<String, Any> = HashMap(4)
        params[GetFormInstanceId.PARAM_FORM_ID] = formId
        params[GetFormInstanceId.PARAM_DATAPOINT_ID] = datapointId
        getFormInstanceIdUseCase.execute(object: DisposableSingleObserver<Long>() {
            override fun onSuccess(formInstanceId: Long) {
                view?.navigateToForm(formId, formInstanceId)
            }

            override fun onError(e: Throwable) {
                if (e is CascadeResourceMissing) {
                    view?.showMissingCascadeError()
                } else {
                    Timber.e(e)
                }
                //TODO: add error messages
            }

        }, params)
    }
}