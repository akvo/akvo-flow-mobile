/*
 * Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.utils.entity

import java.util.HashMap

data class Question(
    val questionId: String? = null,
    val isMandatory: Boolean = false,
    var text: String? = null,
    val order: Int = 0,
    var isAllowOther: Boolean = false,
    val renderType: String? = null,
    var questionHelp: MutableList<QuestionHelp> = mutableListOf(),
    val type: String? = null,
    var options: MutableList<Option>? = null,
    var isAllowMultiple: Boolean = false,
    val isLocked: Boolean = false,
    val languageTranslationMap: HashMap<String?, AltText> = HashMap(),
    val dependencies: MutableList<Dependency> = mutableListOf(),
    val useStrength: Boolean = false,
    val strengthMin: Int = 0,
    val strengthMax: Int = 0,
    val isLocaleName: Boolean = false,
    val isLocaleLocation: Boolean = false,
    val isDoubleEntry: Boolean = false,
    val isAllowPoints: Boolean = false,
    val isAllowLine: Boolean = false,
    val isAllowPolygon: Boolean = false,
    val caddisflyRes: String? = null,
    val cascadeResource: String? = null,
    val levels: MutableList<Level> = mutableListOf(),
) {

    fun addAltText(altText: AltText) {
        languageTranslationMap[altText.languageCode] = altText
    }
}
