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
    var questionId: String? = null,
    var isMandatory: Boolean = false,
    var text: String? = null,
    var order: Int = 0,
    var isAllowOther: Boolean = false,
    var questionHelp: MutableList<QuestionHelp> = mutableListOf(),
    var type: String? = null,
    var options: MutableList<Option>? = null,
    var isAllowMultiple: Boolean = false,
    var isLocked: Boolean = false,
    var languageTranslationMap: HashMap<String?, AltText> = HashMap(),
    var dependencies: MutableList<Dependency> = mutableListOf(),
    var isLocaleName: Boolean = false,
    var isLocaleLocation: Boolean = false,
    var isDoubleEntry: Boolean = false,
    var isAllowPoints: Boolean = false,
    var isAllowLine: Boolean = false,
    var isAllowPolygon: Boolean = false,
    var caddisflyRes: String? = null,
    var cascadeResource: String? = null,
    var levels: MutableList<Level> = mutableListOf(),
    var validationRule: ValidationRule? = null,
) {

    fun addAltText(altText: AltText) {
        languageTranslationMap[altText.languageCode] = altText
    }

    //TODO: remove
    fun getIteration(): Int {
        return if (isRepeatable()) {
            val questionIdAndIteration: List<String> = questionId?.split("\\|") ?: emptyList()
            val iteration = questionIdAndIteration[1]
            iteration.toInt()
        } else {
            -1
        }
    }

    //TODO: remove
    fun isRepeatable(): Boolean {
        return questionId?.contains("|") ?: false
    }

    //TODO: this is only useful for repeated question groups, should be removed
    /**
     * Clone a question and update the question ID. This is only relevant for repeat-question-groups,
     * which require different instances of the question for each iteration.
     * Note: Excluding dependencies, all non-primitive variables are *not* deep-copied.
     */
    fun copy(question: Question, questionId: String): Question {
        val q = Question(
            questionId = questionId,
            text = question.text,
            order = question.order,
            isMandatory = question.isMandatory,
            type = question.type,
            isAllowOther = question.isAllowOther,
            isAllowMultiple = question.isAllowMultiple,
            isLocked = question.isLocked,
            isLocaleName = question.isLocaleName,
            isLocaleLocation = question.isLocaleLocation,
            isDoubleEntry = question.isDoubleEntry,
            isAllowPoints = question.isAllowPoints,
            isAllowLine = question.isAllowLine,
            isAllowPolygon = question.isAllowPolygon,
            cascadeResource = question.cascadeResource,
            validationRule = question.validationRule,
            questionHelp = question.questionHelp,
            languageTranslationMap = question.languageTranslationMap,
            levels = question.levels,
            caddisflyRes = question.caddisflyRes)

        // Deep-copy dependencies
        q.dependencies = ArrayList()
        for (d in question.dependencies) {
            q.dependencies.add(Dependency(d.question, d.answer))
        }

        // Deep-copy options
        val options1 = question.options
        if (options1 != null) {
            q.options = mutableListOf()
            for (o in options1) {
                q.options!!.add(Option(o.text, o.code, o.isOther, o.altTextMap))
            }
        }
        return q
    }
}
