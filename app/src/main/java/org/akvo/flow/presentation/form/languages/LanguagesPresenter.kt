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

package org.akvo.flow.presentation.form.languages

import io.reactivex.observers.DisposableCompletableObserver
import org.akvo.flow.domain.languages.SaveLanguages
import org.akvo.flow.presentation.Presenter
import javax.inject.Inject

class LanguagesPresenter @Inject constructor(
    private val languageMapper: LanguageMapper,
    private val saveLanguagesUseCase: SaveLanguages
) : Presenter {

    private var view: LanguagesView? = null

    fun setView(view: LanguagesView) {
        this.view = view
    }

    override fun destroy() {
        saveLanguagesUseCase.dispose()
    }

    fun loadLanguages(surveyId: Long) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        //        val languages: List<Language> = languageMapper
//            .transform(mLanguages, mSurvey.getAvailableLanguageCodes())
    }

    fun saveLanguages(selectedLanguages: Set<String>, surveyId: Long) {
        val params: MutableMap<String, Any> = HashMap(4)
        params[SaveLanguages.PARAM_SURVEY_ID] = surveyId
        params[SaveLanguages.PARAM_LANGUAGES_LIST] = selectedLanguages
        saveLanguagesUseCase.execute(object : DisposableCompletableObserver() {
            override fun onComplete() {
               view?.onLanguagesSaved()
            }

            override fun onError(e: Throwable) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }, params)
    }

}
