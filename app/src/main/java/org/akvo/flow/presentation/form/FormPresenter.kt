/*
 * Copyright (C) 2018-2019,2021 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.presentation.form

import io.reactivex.observers.DisposableCompletableObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.akvo.flow.domain.QuestionResponse
import org.akvo.flow.domain.Survey
import org.akvo.flow.domain.interactor.DefaultObserver
import org.akvo.flow.domain.interactor.ExportSurveyInstance
import org.akvo.flow.domain.interactor.UseCase
import org.akvo.flow.domain.interactor.forms.FormResult
import org.akvo.flow.domain.interactor.forms.GetFormWithGroups
import org.akvo.flow.domain.interactor.forms.GetSurveyForms
import org.akvo.flow.domain.interactor.forms.UpdateFormInstance
import org.akvo.flow.domain.interactor.responses.LoadResponses
import org.akvo.flow.domain.interactor.responses.ResponsesResult
import org.akvo.flow.domain.interactor.settings.FormVersionUpdateNotified
import org.akvo.flow.domain.interactor.settings.SetFormVersionUpdateNotified
import org.akvo.flow.presentation.Presenter
import org.akvo.flow.utils.entity.SurveyGroup
import org.jetbrains.annotations.NotNull
import timber.log.Timber
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Named

class FormPresenter @Inject constructor(
    private val exportSurveyInstance: ExportSurveyInstance,
    @param:Named("mobileUploadSet") private val mobileUploadSet: UseCase,
    @param:Named("mobileUploadAllowed") private val mobileUploadAllowed: UseCase,
    private val getSurveyForms: GetSurveyForms,
    private val updateFormInstance: UpdateFormInstance,
    private val formVersionUpdateNotified: FormVersionUpdateNotified,
    private val setFormVersionUpdateNotified: SetFormVersionUpdateNotified,
    private val getFormUseCase: GetFormWithGroups,
    private val loadResponses: LoadResponses,
    private val oldFormMapper: OldFormMapper
) : Presenter {

    private var view: FormView? = null
    private var job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun destroy() {
        exportSurveyInstance.dispose()
        mobileUploadSet.dispose()
        mobileUploadAllowed.dispose()
        uiScope.coroutineContext.cancelChildren()
    }

    fun setView(view: FormView?) {
        this.view = view
    }

    fun onSubmitPressed(surveyInstanceId: Long, formId: String, survey: SurveyGroup) {
        mobileUploadSet.execute(object : DefaultObserver<Boolean?>() {
            override fun onNext(mobileUploadSet: Boolean) {
                if (!mobileUploadSet) {
                    view?.showMobileUploadSetting(surveyInstanceId)
                } else {
                    exportInstance(surveyInstanceId, formId, survey)
                }
            }

            override fun onError(e: Throwable) {
                Timber.e(e)
                view?.showMobileUploadSetting(surveyInstanceId)
            }
        }, null)
    }

    private fun exportInstance(surveyInstanceId: Long, formId: String, survey: SurveyGroup) {
        view?.showLoading()
        val params: MutableMap<String, Any> = HashMap(2)
        params[ExportSurveyInstance.SURVEY_INSTANCE_ID_PARAM] = surveyInstanceId
        exportSurveyInstance.execute(object : DisposableCompletableObserver() {
            override fun onComplete() {
                checkConnectionSetting(formId, survey)
            }

            override fun onError(e: Throwable) {
                Timber.e(e)
                view?.hideLoading()
                view?.showErrorExport()
            }
        }, params)
    }

    private fun checkConnectionSetting(formId: String, survey: SurveyGroup) {
        mobileUploadAllowed.execute(object : DefaultObserver<Boolean?>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
                startSyncAndLeave(false, formId, survey)
            }

            override fun onNext(isAllowed: Boolean) {
                startSyncAndLeave(isAllowed, formId, survey)
            }
        }, null)
    }

    private fun startSyncAndLeave(b: Boolean, formId: String, survey: SurveyGroup) {
        view?.startSync(b)
        if (formId == survey.registerSurveyId) {
            uiScope.launch {
                val params: MutableMap<String, Any> = HashMap(2)
                params[GetSurveyForms.PARAM_SURVEY_ID] = survey.id
                val forms = getSurveyForms.execute(params)
                view?.hideLoading()
                if (forms.size <= 1) {
                    view?.dismiss()
                } else {
                   view?.goToListOfForms()
                }
            }
        } else {
            view?.hideLoading()
            view?.dismiss()
        }
    }

    fun updateInstanceVersion(form: @NotNull Survey, formInstanceId: Long) {
        if (formInstanceId == -1L) {
            return
        }
        //update submission form version
        uiScope.launch {
            val params: MutableMap<String, Any> = HashMap(4)
            params[UpdateFormInstance.PARAM_FORM_INSTANCE_ID] = formInstanceId
            params[UpdateFormInstance.PARAM_FORM_VERSION] = form.version
            params[SetFormVersionUpdateNotified.PARAM_FORM_ID] = form.id
            val result: UpdateFormInstance.FormVersionUpdateResult = updateFormInstance.execute(params)
            if (result == UpdateFormInstance.FormVersionUpdateResult.FormVersionUpdated) {
                view?.trackDraftFormVersionUpdated()
                if (formVersionUpdateNotified.execute(params) == FormVersionUpdateNotified.FormVersionNotifiedResult.FormVersionNotNotified) {
                    view?.showFormUpdated()
                    setFormVersionUpdateNotified.execute(params)
                }
            }
        }
    }

    fun loadForm(formId: String, formInstanceId: Long?, surveyGroup: SurveyGroup) {
        val params: MutableMap<String, Any> = HashMap(2)
        params[GetFormWithGroups.PARAM_FORM_ID] = formId

        view?.showLoading()
        uiScope.launch {
            try {
                coroutineScope {
                    val formResult = async { getFormUseCase.execute(params)} .await()
                    val responsesResult: ResponsesResult? = if (formInstanceId != null) {
                        val paramsResponses: MutableMap<String, Any> = HashMap(2)
                        paramsResponses[LoadResponses.PARAM_FORM_INSTANCE_ID] = formInstanceId
                        async { loadResponses.execute(paramsResponses)} .await()
                    } else {
                        null
                    }
                    when (formResult) {
                        is FormResult.Success -> {
                            val form = oldFormMapper.mapForm(surveyGroup, formResult.domainForm)
                            val responses: HashMap<String, QuestionResponse> = if (responsesResult is ResponsesResult.Success) {
                                oldFormMapper.mapResponses(responsesResult.responses, formInstanceId)
                            } else {
                                HashMap<String, QuestionResponse>()
                            }

                            view?.hideLoading()
                            view?.displayFormAndResponses(form, responses)
                        }
                        is FormResult.ParamError -> {
                            Timber.e(formResult.message)
                            view?.hideLoading()
                            view?.showErrorLoadingForm()
                        }
                        else -> {
                            //TODO: display specific error when responses could not be loaded
                            view?.hideLoading()
                            view?.showErrorLoadingForm()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                view?.hideLoading()
                view?.showErrorLoadingForm()
            }
        }
    }


}

