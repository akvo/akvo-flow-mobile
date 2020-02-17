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

package org.akvo.flow.presentation.form.view.languages

import org.akvo.flow.presentation.Presenter
import org.akvo.flow.ui.model.LanguageMapper
import javax.inject.Inject

class LanguagesPresenter @Inject constructor(private val languageMapper: LanguageMapper): Presenter {
    override fun destroy() {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun loadLanguages(formId: String) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        //        val languages: List<Language> = languageMapper
//            .transform(mLanguages, mSurvey.getAvailableLanguageCodes())
    }

    fun saveLanguages(selectedLanguages: Set<String>) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
