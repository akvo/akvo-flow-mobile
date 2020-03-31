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

import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.domain.entity.DomainForm
import org.akvo.flow.domain.interactor.forms.GetForm
import org.akvo.flow.domain.languages.LoadLanguages
import org.akvo.flow.domain.languages.SaveLanguages
import org.akvo.flow.presentation.Presenter
import org.akvo.flow.presentation.form.languages.LanguageMapper
import org.akvo.flow.presentation.form.view.entity.ViewFormMapper
import javax.inject.Inject

class FormViewPresenter @Inject constructor(
    private val saveLanguagesUseCase: SaveLanguages,
    private val languageMapper: LanguageMapper,
    private val loadLanguages: LoadLanguages,
    private val getFormUseCase: GetForm,
    private val viewFormMapper: ViewFormMapper
) : Presenter {

    var view: IFormView? = null

    override fun destroy() {
        saveLanguagesUseCase.dispose()
        getFormUseCase.dispose()
    }

    fun loadForm(
        formId: String,
        formInstanceId: Long,
        surveyGroup: SurveyGroup,
        recordId: String
    ) {
        val params: MutableMap<String, Any> = HashMap(2)
        params[GetForm.PARAM_FORM_ID] = formId
        getFormUseCase.execute(object: DisposableSingleObserver<DomainForm>() {
            override fun onSuccess(domainForm: DomainForm) {
                view?.displayForm(viewFormMapper.transform(domainForm))
            }

            override fun onError(e: Throwable) {
                view?.showErrorLoadingForm()
            }
        }, params)
    }

    //language is saved per survey and not form
    fun saveLanguages(selectedLanguages: Set<String>, surveyId: Long) {
        val params: MutableMap<String, Any> = HashMap(4)
        params[SaveLanguages.PARAM_SURVEY_ID] = surveyId
        params[SaveLanguages.PARAM_LANGUAGES_LIST] = selectedLanguages
        saveLanguagesUseCase.execute(object : DisposableCompletableObserver() {
            override fun onComplete() {
                view?.onLanguagesSaved()
            }

            override fun onError(e: Throwable) {
                view?.onLanguagesSavedError()
            }

        }, params)
    }

    fun loadLanguages(surveyId: Long, formId: String) {
        val params: MutableMap<String, Any> = HashMap(2)
        params[LoadLanguages.PARAM_SURVEY_ID] = surveyId
        params[LoadLanguages.PARAM_FORM_ID] = formId
        loadLanguages.execute(object : DisposableSingleObserver<Pair<Set<String>, Set<String>>>() {
            override fun onSuccess(selectedAndAvailableLanguages: Pair<Set<String>, Set<String>>) {
                view?.displayLanguages(
                    languageMapper.transform(
                        selectedAndAvailableLanguages.first.toTypedArray(),
                        selectedAndAvailableLanguages.second
                    )
                )
            }

            override fun onError(e: Throwable) {
                view?.showLanguagesError()
            }
        }, params)
    }
}
