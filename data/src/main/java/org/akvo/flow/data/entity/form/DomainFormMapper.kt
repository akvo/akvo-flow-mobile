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
import org.akvo.flow.domain.entity.question.DomainAltText
import org.akvo.flow.domain.entity.question.DomainDependency
import org.akvo.flow.domain.entity.question.DomainLevel
import org.akvo.flow.domain.entity.question.DomainOption
import org.akvo.flow.domain.entity.question.DomainQuestion
import org.akvo.flow.domain.entity.question.DomainQuestionHelp
import org.akvo.flow.utils.entity.AltText
import org.akvo.flow.utils.entity.Dependency
import org.akvo.flow.utils.entity.Form
import org.akvo.flow.utils.entity.Level
import org.akvo.flow.utils.entity.Option
import org.akvo.flow.utils.entity.Question
import org.akvo.flow.utils.entity.QuestionGroup
import org.akvo.flow.utils.entity.QuestionHelp
import java.util.HashMap
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

    fun mapForm(dataForm: DataForm, parseForm: Form): DomainForm {
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

    private fun mapGroups(groups: List<QuestionGroup>): List<DomainQuestionGroup> {
        val domainGroups = mutableListOf<DomainQuestionGroup>()
        for (group in groups) {
            domainGroups.add(DomainQuestionGroup(group.heading,
                group.repeatable,
                mapQuestions(group.questions)))
        }
        return domainGroups
    }

    private fun mapQuestions(questions: MutableList<Question>): MutableList<DomainQuestion> {
        val domainQuestions = mutableListOf<DomainQuestion>()
        for (question in questions) {
            domainQuestions.add(DomainQuestion(
                question.questionId,
                question.isMandatory,
                question.text,
                question.order,
                question.isAllowOther,
                question.renderType,
                mapHelps(question.questionHelp),
                question.type,
                mapOptions(question.options),
                question.isAllowMultiple,
                question.isLocked,
                mapAltText(question.languageTranslationMap),
                mapDependencies(question.dependencies),
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
                mapLevels(question.levels)
            ))
        }
        return domainQuestions
    }

    private fun mapLevels(levels: MutableList<Level>): MutableList<DomainLevel> {
        val domainLevels = mutableListOf<DomainLevel>()
        for (level in levels) {
            domainLevels.add(DomainLevel(level.text, mapAltText(level.altTextMap)))
        }
        return domainLevels
    }

    private fun mapDependencies(dependencies: MutableList<Dependency>): MutableList<DomainDependency> {
        val domainDependencies = mutableListOf<DomainDependency>()
        for (dependency in dependencies) {
            domainDependencies.add(DomainDependency(dependency.question, dependency.answer))
        }
        return domainDependencies
    }

    private fun mapOptions(options: MutableList<Option>?): MutableList<DomainOption> {
        val domainOptions = mutableListOf<DomainOption>()
        if (options != null) {
            for (option in options) {
                domainOptions.add(DomainOption(option.text, option.code, option.isOther, mapAltText(option.altTextMap)))
            }
        }
        return domainOptions
    }

    private fun mapHelps(questionHelp: MutableList<QuestionHelp>): MutableList<DomainQuestionHelp> {
        val domainHelps = mutableListOf<DomainQuestionHelp>()
        for (help in questionHelp) {
            domainHelps.add(DomainQuestionHelp(mapAltText(help.altTextMap), help.text))
        }
        return domainHelps
    }

    private fun mapAltText(altTextMap: HashMap<String?, AltText>): HashMap<String?, DomainAltText> {
        val domainAltTextMap = HashMap<String?, DomainAltText>()
        for(language in altTextMap.keys) {
            val altText = altTextMap[language]
            if (altText != null) {
                domainAltTextMap[language] = DomainAltText(altText.languageCode, altText.type, altText.text)
            }
        }
        return domainAltTextMap
    }

}
