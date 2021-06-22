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

import android.content.Context
import android.text.TextUtils
import org.akvo.flow.R
import java.util.ArrayList
import javax.inject.Inject

class LanguageMapper @Inject constructor(private val context: Context) {

    fun transform(
        languageCodesSelected: Array<String>?,
        languageCodesAvailable: Set<String?>?
    ): List<Language> {
        if (languageCodesSelected == null || languageCodesAvailable == null) {
            return emptyList()
        }
        val languages: MutableList<Language> = ArrayList(languageCodesAvailable.size)
        for (language in languageCodesAvailable) {
            val transformed = transform(language, languageCodesSelected)
            transformed?.let {
                languages.add(transformed)
            }
        }
        return languages
    }

    private fun transform(languageCode: String?, languageCodesSelected: Array<String>): Language? {
        if (TextUtils.isEmpty(languageCode)) {
            return null
        }
        val selected = listOf(*languageCodesSelected).contains(languageCode)
        val res = context.resources
        val languageCodes = res.getStringArray(R.array.alllanguagecodes)
        val languages = res.getStringArray(R.array.alllanguages)
        /*
         * This presupposes that both arrays languages and language codes have the same order
         * and contain the same languages
         */
        val languagePosition = listOf(*languageCodes).indexOf(languageCode)
        if (languagePosition < 0 || languagePosition >= languages.size) {
            return null
        }
        val languageName = languages[languagePosition]
        return Language(languageCode!!, languageName, selected)
    }
}
