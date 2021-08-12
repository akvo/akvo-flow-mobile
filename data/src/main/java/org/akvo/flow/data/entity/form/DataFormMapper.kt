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

import android.database.Cursor
import org.akvo.flow.database.SurveyColumns
import org.akvo.flow.domain.entity.question.DomainAltText
import org.akvo.flow.domain.entity.question.DomainDependency
import org.akvo.flow.domain.entity.question.DomainLevel
import org.akvo.flow.domain.entity.question.DomainOption
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

class DataFormMapper @Inject constructor() {

    fun mapForm(xmlForm: Form): DataForm {
        return DataForm(
            xmlForm.id,
            xmlForm.formId,
            xmlForm.surveyId,
            xmlForm.name,
            xmlForm.version,
            xmlForm.type,
            xmlForm.location,
            xmlForm.filename,
            xmlForm.language,
            xmlForm.cascadeDownloaded,
            xmlForm.deleted,
            mapGroups(xmlForm.groups)
        )
    }

    private fun mapGroups(groups: MutableList<QuestionGroup>): MutableList<DataQuestionGroup> {
        val parsedGroups = mutableListOf<DataQuestionGroup>()
        for (group in groups) {
            parsedGroups.add(DataQuestionGroup(group.groupId,
                group.heading,
                group.repeatable,
                group.formId,
                group.order,
                mapQuestions(group.questions)))
        }
        return parsedGroups
    }

    private fun mapQuestions(questions: MutableList<Question>): MutableList<DataQuestion> {
        val parsedQuestions = mutableListOf<DataQuestion>()
        for (question in questions) {
            parsedQuestions.add(DataQuestion(
                question.questionId,
                question.isMandatory,
                question.text,
                question.order,
                question.isAllowOther,
                mapQuestionHelp(question.questionHelp),
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
                mapLevels(question.levels)))
        }
        return parsedQuestions
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

    private fun mapOptions(options: MutableList<Option>?): MutableList<DomainOption>? {
        val domainOptions = mutableListOf<DomainOption>()
        if (options != null) {
            for (option in options) {
                domainOptions.add(DomainOption(option.text, option.code, option.isOther, mapAltText(option.altTextMap)))
            }
        }
        return domainOptions
    }

    private fun mapQuestionHelp(questionHelp: MutableList<QuestionHelp>): MutableList<DomainQuestionHelp> {
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

    fun mapForms(cursor: Cursor?): List<DataForm> {
        val forms = mutableListOf<DataForm>()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val id = getIntColumnValue(cursor, SurveyColumns._ID)
                val formId = getStringColumnValue(cursor, SurveyColumns.SURVEY_ID)
                val surveyId = getIntColumnValue(cursor, SurveyColumns.SURVEY_GROUP_ID)
                val formVersion = getDoubleColumnValue(cursor, SurveyColumns.VERSION)
                val name = getStringColumnValue(cursor, SurveyColumns.NAME)
                val type = getStringColumnValue(cursor, SurveyColumns.TYPE)
                val location = getStringColumnValue(cursor, SurveyColumns.LOCATION)
                val filename = getStringColumnValue(cursor, SurveyColumns.FILENAME)
                val language = getStringColumnValue(cursor, SurveyColumns.LANGUAGE)
                val resourcesDownloaded =
                    getIntColumnValue(cursor, SurveyColumns.HELP_DOWNLOADED) == 1
                val deleted = getIntColumnValue(cursor, SurveyColumns.DELETED) == 1
                val dataForm = DataForm(
                    id,
                    formId,
                    surveyId,
                    name,
                    formVersion,
                    type,
                    location,
                    filename,
                    language,
                    resourcesDownloaded,
                    deleted
                )
                forms.add(dataForm)
            } while (cursor.moveToNext())
        }
        cursor?.close()
        return forms
    }


    fun mapForm(cursor: Cursor?): DataForm? {
        if (cursor != null && cursor.moveToFirst()) {
            val id = getIntColumnValue(cursor, SurveyColumns._ID)
            val formId = getStringColumnValue(cursor, SurveyColumns.SURVEY_ID)
            val surveyId = getIntColumnValue(cursor, SurveyColumns.SURVEY_GROUP_ID)
            val formVersion = getDoubleColumnValue(cursor, SurveyColumns.VERSION)
            val name = getStringColumnValue(cursor, SurveyColumns.NAME)
            val type = getStringColumnValue(cursor, SurveyColumns.TYPE)
            val location = getStringColumnValue(cursor, SurveyColumns.LOCATION)
            val filename = getStringColumnValue(cursor, SurveyColumns.FILENAME)
            val language = getStringColumnValue(cursor, SurveyColumns.LANGUAGE)
            val resourcesDownloaded = getIntColumnValue(cursor, SurveyColumns.HELP_DOWNLOADED) == 1
            val deleted = getIntColumnValue(cursor, SurveyColumns.DELETED) == 1
            cursor.close()
            return DataForm(
                id,
                formId,
                surveyId,
                name,
                formVersion,
                type,
                location,
                filename,
                language,
                resourcesDownloaded,
                deleted
            )
        } else {
            cursor?.close()
            return null
        }
    }

    private fun getDoubleColumnValue(cursor: Cursor, columnName: String) =
        cursor.getDouble(cursor.getColumnIndexOrThrow(columnName))

    private fun getStringColumnValue(cursor: Cursor, columnName: String) =
        cursor.getString(cursor.getColumnIndexOrThrow(columnName))

    private fun getIntColumnValue(cursor: Cursor, columnName: String) =
        cursor.getInt(cursor.getColumnIndexOrThrow(columnName))
}
