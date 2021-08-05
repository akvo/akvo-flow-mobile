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

package org.akvo.flow.data.entity.form

import org.akvo.flow.domain.entity.DomainForm
import org.akvo.flow.domain.entity.DomainQuestionGroup
import org.akvo.flow.domain.entity.question.DomainQuestion
import javax.inject.Inject

class DomainFormMapper @Inject constructor() {

    fun mapForm(dataForm: DataForm): DomainForm {
        return DomainForm(
            dataForm.id,
            dataForm.formId,
            dataForm.surveyId,
            dataForm.name,
            dataForm.version.toString(),
            dataForm.type,
            dataForm.location,
            dataForm.filename,
            dataForm.language,
            dataForm.cascadeDownloaded,
            dataForm.deleted
        )
    }

    fun mapForms(dataForms: MutableList<DataForm>): MutableList<DomainForm> {
        val domainForms = mutableListOf<DomainForm>()
        for (f in dataForms) {
            domainForms.add(mapForm(f))
        }
        return domainForms
    }

    fun mapForms(dataForm: DataForm, parseForm: DataForm): DomainForm {
        return DomainForm(
            dataForm.id,
            dataForm.formId,
            dataForm.surveyId,
            parseForm.name,
            parseForm.version.toString(),
            dataForm.type,
            dataForm.location,
            dataForm.filename,
            dataForm.language,
            dataForm.cascadeDownloaded,
            dataForm.deleted,
            groups = mapGroups(parseForm.groups)
        )
    }

    private fun mapGroups(groups: List<DataQuestionGroup>): List<DomainQuestionGroup> {
        val domainGroups = mutableListOf<DomainQuestionGroup>()
        for (group in groups) {
            domainGroups.add(DomainQuestionGroup(group.heading,
                group.repeatable,
                mapQuestions(group.questions)))
        }
        return domainGroups
    }

    private fun mapQuestions(questions: MutableList<DataQuestion>): MutableList<DomainQuestion> {
        val domainQuestions = mutableListOf<DomainQuestion>()
        for (question in questions) {
            domainQuestions.add(DomainQuestion(
                question.questionId,
                question.isMandatory,
                question.text,
                question.order,
                question.isAllowOther,
                question.renderType,
                question.questionHelp,
                question.type,
                question.options,
                question.isAllowMultiple,
                question.isLocked,
                question.languageTranslationMap,
                question.dependencies,
                question.useStrength,
                question.strengthMin,
                question.strengthMax,
                question.isLocaleName,
                question.isLocaleLocation,
                question.isDoubleEntry,
                question.isAllowPoints,
                question.isAllowLine,
                question.isAllowPolygon,
                question.caddisflyRes,
                question.cascadeResource,
                question.levels
            ))
        }
        return domainQuestions
    }
}
