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

package org.akvo.flow.presentation.form

import org.akvo.flow.domain.QuestionGroup
import org.akvo.flow.domain.QuestionResponse
import org.akvo.flow.domain.Survey
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.domain.entity.DomainForm
import org.akvo.flow.domain.entity.DomainQuestionGroup
import org.akvo.flow.domain.entity.Response
import org.akvo.flow.domain.entity.question.DomainAltText
import org.akvo.flow.domain.entity.question.DomainDependency
import org.akvo.flow.domain.entity.question.DomainLevel
import org.akvo.flow.domain.entity.question.DomainOption
import org.akvo.flow.domain.entity.question.DomainQuestion
import org.akvo.flow.domain.entity.question.DomainQuestionHelp
import org.akvo.flow.utils.entity.AltText
import org.akvo.flow.utils.entity.Dependency
import org.akvo.flow.utils.entity.Level
import org.akvo.flow.utils.entity.Option
import org.akvo.flow.utils.entity.Question
import org.akvo.flow.utils.entity.QuestionHelp
import java.util.ArrayList
import java.util.HashMap
import javax.inject.Inject

class OldFormMapper @Inject constructor() {

    fun mapForm(surveyGroup: SurveyGroup, domainForm: DomainForm): Survey {
        val form = Survey()
        form.surveyGroup = surveyGroup
        form.name = domainForm.name
        form.id = domainForm.formId
        form.questionGroups = mapGroups(domainForm.groups)
        form.version = domainForm.version.toDouble()
        form.type = domainForm.type
        form.location = domainForm.location
        form.fileName = domainForm.filename
        form.isHelpDownloaded = domainForm.cascadeDownloaded
        form.language = domainForm.language
        return form
    }

    private fun mapGroups(groups: List<DomainQuestionGroup>): MutableList<QuestionGroup> {
        val mappedGroups = mutableListOf<QuestionGroup>()
        for ((i, group) in groups.withIndex()) {
            val element = QuestionGroup()
            element.order = i
            element.heading = group.heading
            element.isRepeatable = group.isRepeatable
            element.questions.addAll(mapQuestions(group.questions))
            mappedGroups.add(element)
        }
        return mappedGroups
    }

    private fun mapQuestions(domainQuestions: MutableList<DomainQuestion>): ArrayList<Question> {
        val questions = arrayListOf<Question>()
        for (question in domainQuestions) {
            questions.add(Question(
                question.questionId,
                question.isMandatory,
                question.text,
                question.order,
                question.isAllowOther,
                mapHelps(question.questionHelp),
                question.type,
                mapOptions(question.options),
                question.isAllowMultiple,
                question.isLocked,
                mapAltText(question.languageTranslationMap),
                mapDependencies(question.dependencies),
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
        return questions
    }

    private fun mapLevels(domainLevels: MutableList<DomainLevel>): MutableList<Level> {
        val levels = mutableListOf<Level>()
        for (level in domainLevels) {
            levels.add(Level(level.text, mapAltText(level.altTextMap)))
        }
        return levels
    }

    private fun mapDependencies(domainDependencies: MutableList<DomainDependency>): MutableList<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        for (dependency in domainDependencies) {
            dependencies.add(Dependency(dependency.question, dependency.answer))
        }
        return dependencies
    }

    private fun mapOptions(domainOptions: MutableList<DomainOption>?): MutableList<Option> {
        val options = mutableListOf<Option>()
        if (domainOptions != null) {
            for (option in domainOptions) {
                options.add(Option(option.text,
                    option.code,
                    option.isOther,
                    mapAltText(option.altTextMap)))
            }
        }
        return options
    }

    private fun mapHelps(questionHelp: MutableList<DomainQuestionHelp>): MutableList<QuestionHelp> {
        val domainHelps = mutableListOf<QuestionHelp>()
        for (help in questionHelp) {
            domainHelps.add(QuestionHelp(mapAltText(help.altTextMap), help.text))
        }
        return domainHelps
    }

    private fun mapAltText(domainAltText: HashMap<String?, DomainAltText>): HashMap<String?, AltText> {
        val textMap = HashMap<String?, AltText>()
        for (language in domainAltText.keys) {
            val altText = domainAltText[language]
            if (altText != null) {
                textMap[language] = AltText(altText.languageCode, altText.type, altText.text)
            }
        }
        return textMap
    }

    fun mapResponses(
        responses: List<Response>,
        formInstanceId: Long?,
    ): HashMap<String, QuestionResponse> {
        // QuestionId - QuestionResponse
        val questionResponses: HashMap<String, QuestionResponse> = HashMap()
        for (response in responses) {
            //TODO: check if id is needed
            //check if filename is needed
            //check include flag
            val questionResponse = QuestionResponse(response.value, response.answerType, null,
                formInstanceId, response.questionId, null, true, response.iteration)
            questionResponses[questionResponse.responseKey] = questionResponse
        }
        return questionResponses
    }
}
