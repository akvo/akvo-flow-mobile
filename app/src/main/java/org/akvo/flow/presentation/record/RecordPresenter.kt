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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.domain.entity.DataPoint
import org.akvo.flow.domain.entity.DomainForm
import org.akvo.flow.domain.entity.User
import org.akvo.flow.domain.interactor.datapoints.GetDataPoint
import org.akvo.flow.domain.interactor.forms.GetForm
import org.akvo.flow.domain.interactor.forms.GetRecentSubmittedFormInstance
import org.akvo.flow.domain.interactor.forms.GetSavedFormInstance
import org.akvo.flow.domain.interactor.users.GetSelectedUser
import org.akvo.flow.domain.interactor.users.ResultCode
import org.akvo.flow.presentation.Presenter
import org.akvo.flow.presentation.datapoints.DisplayNameMapper
import timber.log.Timber
import javax.inject.Inject

class RecordPresenter @Inject constructor(
    private val getDataPoint: GetDataPoint,
    private val displayNameMapper: DisplayNameMapper,
    private val getSelectedUserUseCase: GetSelectedUser,
    private val getFormUseCase: GetForm,
    private val getSavedFormInstanceUseCase: GetSavedFormInstance,
    private val getRecentSubmittedFormInstanceUseCase: GetRecentSubmittedFormInstance
) : Presenter {

    var view: RecordView? = null
    private var job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun destroy() {
        getFormUseCase.dispose()
        getSavedFormInstanceUseCase.dispose()
        getRecentSubmittedFormInstanceUseCase.dispose()
        uiScope.coroutineContext.cancelChildren()
    }

    fun onFormClick(formId: String, datapointId: String, survey: SurveyGroup) {
        //check user
        uiScope.launch {
            val userResult = getSelectedUserUseCase.execute()
            if (userResult.resultCode == ResultCode.SUCCESS) {
                val params: MutableMap<String, Any> = HashMap(2)
                params[GetForm.PARAM_FORM_ID] = formId
                getFormUseCase.execute(object : DisposableSingleObserver<DomainForm>() {
                    override fun onSuccess(domainForm: DomainForm) {
                        if (!domainForm.cascadeDownloaded) {
                            view?.showMissingCascadeError()
                        } else {
                            getSavedFormInstance(formId, datapointId, survey, domainForm, userResult.user)
                        }
                    }

                    override fun onError(e: Throwable) {
                        view?.showFormNotFound()
                    }
                }, params)
            } else {
                view?.showMissingUserError()
            }
        }
    }

    private fun getSavedFormInstance(
        formId: String,
        datapointId: String,
        survey: SurveyGroup,
        domainForm: DomainForm,
        user: User
    ) {
        val params: MutableMap<String, Any> = HashMap(4)
        params[GetSavedFormInstance.PARAM_FORM_ID] = formId
        params[GetSavedFormInstance.PARAM_DATAPOINT_ID] = datapointId
        getSavedFormInstanceUseCase.execute(object :
            DisposableSingleObserver<Long>() {
            override fun onSuccess(formInstanceId: Long) {
                if (formInstanceId != -1L) {
                    view?.navigateToForm(formId, formInstanceId)
                } else {
                    if (formId != survey.registerSurveyId) {
                        verifyLatestSubmittedForm(
                            domainForm,
                            formId,
                            datapointId,
                            user
                        )
                    } else {
                        view?.navigateToForm(formId, user)
                    }
                }
            }

            override fun onError(e: Throwable) {
                if (formId != survey.registerSurveyId) {
                    verifyLatestSubmittedForm(
                        domainForm,
                        formId,
                        datapointId,
                        user
                    )
                } else {
                    view?.navigateToForm(formId, user)

                }
            }
        }, params)
    }

    private fun verifyLatestSubmittedForm(
        form: DomainForm,
        formId: String,
        datapointId: String,
        user: User
    ) {
        val params: MutableMap<String, Any> = HashMap(4)
        params[GetRecentSubmittedFormInstance.PARAM_FORM_ID] = formId
        params[GetRecentSubmittedFormInstance.PARAM_DATAPOINT_ID] = datapointId
        getRecentSubmittedFormInstanceUseCase.execute(object :
            DisposableSingleObserver<Long>() {
            override fun onSuccess(formInstanceId: Long) {
                if (formInstanceId != -1L) {
                    view?.displayWarningDialog(form.name, formId, user)
                } else {
                    view?.navigateToForm(formId, user)
                }
            }

            override fun onError(e: Throwable) {
                Timber.e(e)
                view?.navigateToForm(formId, user)
            }
        }, params)
    }

    fun loadDataPoint(dataPointId: String) {
        val params: MutableMap<String, Any> = HashMap(2)
        params[GetDataPoint.PARAM_DATA_POINT_ID] = dataPointId
        getDataPoint.execute(object : DisposableSingleObserver<DataPoint>() {
            override fun onSuccess(dataPoint: DataPoint) {
                val displayName: String = displayNameMapper.createDisplayName(dataPoint.name)
                view?.showDataPointTitle(displayName)
            }

            override fun onError(e: Throwable) {
                Timber.e(e)
                view?.showDataPointError()
            }
        }, params)
    }
}
