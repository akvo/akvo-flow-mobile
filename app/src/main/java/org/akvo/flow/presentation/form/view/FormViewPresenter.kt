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

package org.akvo.flow.presentation.form.view

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.domain.interactor.forms.FormResult
import org.akvo.flow.domain.interactor.forms.GetFormWithGroups
import org.akvo.flow.domain.interactor.responses.LoadResponses
import org.akvo.flow.domain.interactor.responses.ResponsesResult
import org.akvo.flow.domain.languages.LoadLanguages
import org.akvo.flow.domain.languages.SaveLanguages
import org.akvo.flow.presentation.Presenter
import org.akvo.flow.presentation.form.languages.LanguageMapper
import org.akvo.flow.presentation.form.view.entity.ViewFormMapper
import timber.log.Timber
import javax.inject.Inject

class FormViewPresenter @Inject constructor(
    private val saveLanguagesUseCase: SaveLanguages,
    private val languageMapper: LanguageMapper,
    private val loadLanguages: LoadLanguages,
    private val getFormUseCase: GetFormWithGroups,
    private val viewFormMapper: ViewFormMapper,
    private val loadResponses: LoadResponses
) : Presenter {

    var view: IFormView? = null

    private var job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun destroy() {
        uiScope.coroutineContext.cancelChildren()
    }

    fun loadForm(
        formId: String,
        formInstanceId: Long,
        surveyGroup: SurveyGroup,
        recordId: String
    ) {
        val params: MutableMap<String, Any> = HashMap(2)
        params[GetFormWithGroups.PARAM_FORM_ID] = formId

        val paramsResponses: MutableMap<String, Any> = HashMap(2)
        paramsResponses[LoadResponses.PARAM_FORM_INSTANCE_ID] = formInstanceId
        //TODO: show loading
        uiScope.launch {
            try {
                // coroutineScope is needed, else in case of any network error, it will crash
                coroutineScope {
                    val formResult = async { getFormUseCase.execute(params)} .await()
                    val responsesResult = async { loadResponses.execute(paramsResponses)} .await()
                    if (formResult is FormResult.Success && responsesResult is ResponsesResult.Success) {
                        view?.displayForm(viewFormMapper.transform(formResult.domainForm, responsesResult.responses))
                    } else if (formResult is FormResult.ParamError ) {
                        Timber.e(formResult.message)
                        view?.showErrorLoadingForm()
                    } else {
                        //TODO: display specific error when responses could not be loaded
                        view?.showErrorLoadingForm()
                    }
                }
            } catch (e: Exception) {
                view?.showErrorLoadingForm()
            }
        }
    }

    //language is saved per survey and not form
    fun saveLanguages(selectedLanguages: Set<String>, surveyId: Long) {
        val params: MutableMap<String, Any> = HashMap(4)
        params[SaveLanguages.PARAM_SURVEY_ID] = surveyId
        params[SaveLanguages.PARAM_LANGUAGES_LIST] = selectedLanguages
        uiScope.launch {
           when(saveLanguagesUseCase.execute(params)) {
               is SaveLanguages.SaveLanguageResult.Success -> view?.onLanguagesSaved()
               is SaveLanguages.SaveLanguageResult.Error -> view?.onLanguagesSavedError()
           }
        }
    }

    //TODO: use the loaded form
    fun loadLanguages(surveyId: Long, formId: String) {
        val params: MutableMap<String, Any> = HashMap(2)
        params[LoadLanguages.PARAM_SURVEY_ID] = surveyId
        params[LoadLanguages.PARAM_FORM_ID] = formId
        uiScope.launch {
            when(val result = loadLanguages.execute(params)) {
                is LoadLanguages.LanguageResult.Success ->  view?.displayLanguages(
                    languageMapper.transform(
                        result.savedLanguages.toTypedArray(),
                        result.availableLanguages
                    )
                )
                is LoadLanguages.LanguageResult.Error -> view?.showLanguagesError()
            }
        }
    }
}
