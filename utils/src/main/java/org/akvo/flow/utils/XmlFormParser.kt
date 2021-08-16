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
package org.akvo.flow.utils

import org.akvo.flow.utils.entity.AltText
import org.akvo.flow.utils.entity.Dependency
import org.akvo.flow.utils.entity.Form
import org.akvo.flow.utils.entity.Level
import org.akvo.flow.utils.entity.Option
import org.akvo.flow.utils.entity.Question
import org.akvo.flow.utils.entity.QuestionGroup
import org.akvo.flow.utils.entity.QuestionHelp
import org.akvo.flow.utils.entity.SurveyGroup
import org.akvo.flow.utils.entity.SurveyMetadata
import org.akvo.flow.utils.entity.ValidationRule
import org.akvo.flow.utils.entity.ValidationRule.DEFAULT_MAX_LENGTH
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.util.LinkedHashSet
import javax.inject.Inject

class XmlFormParser @Inject constructor(private val helper: FileHelper) {

    fun parseLanguages(input: InputStream?): Set<String> {
        val languageCodes: MutableSet<String> = LinkedHashSet()
        val questionLanguagesCodes: MutableSet<String> = LinkedHashSet()
        var defaultLanguage = ""
        val parserFactory: XmlPullParserFactory
        try {
            parserFactory = XmlPullParserFactory.newInstance()
            val parser = parserFactory.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(input, null)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                var eltName: String
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        eltName = parser.name
                        if (SURVEY == eltName) {
                            val parsedAttribute = parser.getAttributeValue(null, DEFAULT_LANG)
                            if (!parsedAttribute.isNullOrEmpty()) {
                                defaultLanguage = parsedAttribute
                            }
                        } else if (ALT_TEXT == eltName) {
                            val language = parser.getAttributeValue(null, LANG)
                            if (!language.isNullOrEmpty()) {
                                questionLanguagesCodes.add(language)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            Timber.e(e)
        } catch (e: IOException) {
            Timber.e(e)
        } finally {
            helper.close(input)
        }
        if (defaultLanguage.isEmpty()) {
            defaultLanguage = "en"
        }
        languageCodes.add(defaultLanguage)
        languageCodes.addAll(questionLanguagesCodes)
        return languageCodes
    }

    @JvmOverloads
    fun parseXmlForm(inputStream: InputStream, backUpVersion: Double? = null): Form {
        val groups: MutableList<QuestionGroup> = mutableListOf()
        var version = backUpVersion ?: 0.0
        var name = ""
        var formId = -1
        var surveyId = -1L
        var defaultLanguage = ""
        val parserFactory: XmlPullParserFactory
        var currentQuestionGroup: QuestionGroup? = null
        var currentQuestion: Question? = null
        var currentOptions = mutableListOf<Option>()
        var currentOption: Option? = null
        var currentLevels = mutableListOf<Level>()
        var currentLevel: Level? = null
        var currentAltText: AltText? = null
        var currentHelp: QuestionHelp? = null
        var lastText: String? = null
        var groupOrder = 0
        try {
            parserFactory = XmlPullParserFactory.newInstance()
            val parser = parserFactory.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {

                when (eventType) {
                    /**
                     * Beginning of a tag for example <question>
                     */
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            SURVEY -> {
                                version = getDoubleAttribute(parser, VERSION)
                                name = getNonNullStringAttribute(parser, NAME)
                                formId = getIntAttribute(parser, FORM_ID, -1)
                                surveyId = getNonNullLongAttribute(parser, SURVEY_ID)
                                val parsedAttribute = parser.getAttributeValue(null, DEFAULT_LANG)
                                if (!parsedAttribute.isNullOrEmpty()) {
                                    defaultLanguage = parsedAttribute
                                }
                            }
                            QUESTION_GROUP -> {
                                groupOrder++
                                val repeatable = "true" == getStringAttribute(parser, REPEATABLE)
                                val groupId = getLongAttribute(parser, "groupId")
                                currentQuestionGroup = QuestionGroup(groupId,
                                    "",
                                    repeatable = repeatable,
                                    formId.toString(), groupOrder)
                            }
                            QUESTION_GROUP_HEADING -> {
                                currentQuestionGroup?.heading = parser.nextText()
                            }
                            QUESTION -> {
                                val resource = getStringAttribute(parser, CASCADE_RESOURCE)
                                var order = getIntAttribute(parser, ORDER, -1)
                                if (order == -1) {
                                    order = 1
                                    if (currentQuestionGroup != null) {
                                        order = currentQuestionGroup.questions.size + 1
                                    }
                                }

                                currentQuestion = Question(
                                    cascadeResource = resource,
                                    order = order,
                                    isMandatory = getBooleanAttribute(parser, MANDATORY),
                                    isLocked = getBooleanAttribute(parser, LOCKED),
                                    isDoubleEntry = getBooleanAttribute(parser, DOUBLE_ENTRY),
                                    isAllowMultiple = getBooleanAttribute(parser, ALLOW_MULT),
                                    type = parser.getAttributeValue(null, TYPE),
                                    questionId = parser.getAttributeValue(null, ID),
                                    isLocaleName = getBooleanAttribute(parser, LOCALE_NAME),
                                    isLocaleLocation = getBooleanAttribute(parser, LOCALE_LOCATION),
                                    caddisflyRes = getStringAttribute(parser, CADDISFLY_RESOURCE),
                                    isAllowPoints = getBooleanAttribute(parser, ALLOW_POINTS),
                                    isAllowLine = getBooleanAttribute(parser, ALLOW_LINE),
                                    isAllowPolygon = getBooleanAttribute(parser, ALLOW_POLYGON)
                                )
                            }
                            OPTIONS -> {
                                currentOptions = mutableListOf()
                                if (currentQuestion != null) {
                                    currentQuestion.isAllowOther = getBooleanAttribute(
                                        parser,
                                        ALLOW_OTHER
                                    )
                                    currentQuestion.isAllowMultiple = getBooleanAttribute(
                                        parser,
                                        ALLOW_MULT
                                    )
                                }
                            }
                            OPTION -> {
                                currentOption = Option(code = getStringAttribute(parser, CODE))
                            }
                            LEVELS -> {
                                currentLevels = mutableListOf()
                            }
                            LEVEL -> {
                                currentLevel = Level()
                            }
                            DEPENDENCY -> {
                                currentQuestion?.dependencies?.add(
                                    Dependency(
                                        question = getStringAttribute(parser, QUESTION),
                                        answer = getStringAttribute(parser, ANSWER)
                                    )
                                )
                            }
                            VALIDATION_RULE -> {
                                currentQuestion?.validationRule = parseValidationValue(parser)
                            }
                            ALT_TEXT -> {
                                currentAltText = AltText(
                                    languageCode = getStringAttribute(parser, LANG),
                                    type = getStringAttribute(parser, TYPE)
                                )
                            }
                            HELP -> {
                                currentHelp = QuestionHelp()
                            }

                        }
                    }
                    /**
                     * End of a tag for example </question>
                     */
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            QUESTION_GROUP -> {
                                currentQuestionGroup?.let {
                                    groups.add(it)
                                }
                            }
                            QUESTION -> {
                                if (currentQuestionGroup != null && currentQuestion != null) {
                                    if (lastText != null) {
                                        currentQuestion.text = lastText
                                        lastText = null
                                    }
                                    currentQuestionGroup.questions.add(currentQuestion)
                                    currentQuestion = null
                                }
                            }
                            OPTIONS -> {
                                if (currentQuestion != null) {
                                    if (currentQuestion.options == null) {
                                        currentQuestion.options = mutableListOf()
                                    }
                                    currentQuestion.options?.addAll(currentOptions)
                                    currentOptions = mutableListOf()
                                }
                            }
                            OPTION -> {
                                if (currentOption != null) {
                                    if (lastText != null) {
                                        currentOption.text = lastText
                                        lastText = null
                                    }
                                    currentOptions.add(currentOption)
                                    currentOption = null
                                }
                            }
                            LEVELS -> {
                                currentQuestion?.levels?.addAll(currentLevels)
                                currentLevels = mutableListOf()
                            }
                            LEVEL -> {
                                if (currentLevel != null) {
                                    if (lastText != null) {
                                        currentLevel.text = lastText
                                        lastText = null
                                    }
                                    currentLevels.add(currentLevel)
                                    currentLevel = null
                                }
                            }
                            DEPENDENCY -> {
                                //nothing to do
                            }
                            ALT_TEXT -> {
                                //can be inside question, help or option
                                if (currentAltText != null) {
                                    when {
                                        currentOption != null -> {
                                            currentOption.addAltText(currentAltText)
                                        }
                                        currentHelp != null -> {
                                            currentHelp.addAltText(currentAltText)
                                        }
                                        currentQuestion != null -> {
                                            currentQuestion.addAltText(currentAltText)
                                        }
                                    }
                                    currentAltText = null
                                }
                            }
                            HELP -> {
                                if (currentHelp != null) {
                                    if (lastText != null) {
                                        currentHelp.text = lastText
                                        lastText = null
                                    }
                                    currentQuestion?.questionHelp?.add(currentHelp)
                                    currentHelp = null
                                }

                            }
                        }
                    }
                    /**
                     * this is the <text>some text</text> content
                     * <text> can appear in multiple places: inside question, option, level, help
                     */
                    XmlPullParser.TEXT -> {
                        lastText = parser.text
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            Timber.e(e)
        } catch (e: IOException) {
            Timber.e(e)
        } finally {
            helper.close(inputStream)
        }
        return Form(formId, formId.toString(),
            surveyId,
            name = name,
            version = version,
            filename = "$formId.xml",
            language = defaultLanguage,
            groups = groups)
    }

    fun parseXmlQuestions(inputStream: InputStream): HashMap<Int, MutableList<Question>> {
        val parserFactory: XmlPullParserFactory
        var currentQuestion: Question? = null
        var currentOptions = mutableListOf<Option>()
        var currentOption: Option? = null
        var currentLevels = mutableListOf<Level>()
        var currentLevel: Level? = null
        var currentAltText: AltText? = null
        var currentHelp: QuestionHelp? = null
        var lastText: String? = null
        var groupOrder = 0
        var currentQuestions = mutableListOf<Question>()
        val map = HashMap<Int, MutableList<Question>>()
        try {
            parserFactory = XmlPullParserFactory.newInstance()
            val parser = parserFactory.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {

                when (eventType) {
                    /**
                     * Beginning of a tag for example <question>
                     */
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            QUESTION_GROUP -> {
                                groupOrder++
                                currentQuestions = mutableListOf()
                            }
                            QUESTION -> {
                                val resource = getStringAttribute(parser, CASCADE_RESOURCE)
                                var order = getIntAttribute(parser, ORDER, -1)
                                if (order == -1) {
                                    order = 1
                                }

                                currentQuestion = Question(
                                    cascadeResource = resource,
                                    order = order,
                                    isMandatory = getBooleanAttribute(parser, MANDATORY),
                                    isLocked = getBooleanAttribute(parser, LOCKED),
                                    isDoubleEntry = getBooleanAttribute(parser, DOUBLE_ENTRY),
                                    isAllowMultiple = getBooleanAttribute(parser, ALLOW_MULT),
                                    type = parser.getAttributeValue(null, TYPE),
                                    questionId = parser.getAttributeValue(null, ID),
                                    isLocaleName = getBooleanAttribute(parser, LOCALE_NAME),
                                    isLocaleLocation = getBooleanAttribute(parser, LOCALE_LOCATION),
                                    caddisflyRes = getStringAttribute(parser, CADDISFLY_RESOURCE),
                                    isAllowPoints = getBooleanAttribute(parser, ALLOW_POINTS),
                                    isAllowLine = getBooleanAttribute(parser, ALLOW_LINE),
                                    isAllowPolygon = getBooleanAttribute(parser, ALLOW_POLYGON)
                                )
                            }
                            OPTIONS -> {
                                currentOptions = mutableListOf()
                                if (currentQuestion != null) {
                                    currentQuestion.isAllowOther = getBooleanAttribute(
                                        parser,
                                        ALLOW_OTHER
                                    )
                                    currentQuestion.isAllowMultiple = getBooleanAttribute(
                                        parser,
                                        ALLOW_MULT
                                    )
                                }
                            }
                            OPTION -> {
                                currentOption = Option(code = getStringAttribute(parser, CODE))
                            }
                            LEVELS -> {
                                currentLevels = mutableListOf()
                            }
                            LEVEL -> {
                                currentLevel = Level()
                            }
                            DEPENDENCY -> {
                                currentQuestion?.dependencies?.add(
                                    Dependency(
                                        question = getStringAttribute(parser, QUESTION),
                                        answer = getStringAttribute(parser, ANSWER)
                                    )
                                )
                            }
                            VALIDATION_RULE -> {
                                currentQuestion?.validationRule = parseValidationValue(parser)
                            }
                            ALT_TEXT -> {
                                currentAltText = AltText(
                                    languageCode = getStringAttribute(parser, LANG),
                                    type = getStringAttribute(parser, TYPE)
                                )
                            }
                            HELP -> {
                                currentHelp = QuestionHelp()
                            }
                        }
                    }
                    /**
                     * End of a tag for example </question>
                     */
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            QUESTION_GROUP -> {
                                map[groupOrder] = currentQuestions
                            }
                            QUESTION -> {
                                if (currentQuestion != null) {
                                    if (lastText != null) {
                                        currentQuestion.text = lastText
                                        lastText = null
                                    }
                                    currentQuestions.add(currentQuestion)
                                    currentQuestion = null
                                }
                            }
                            OPTIONS -> {
                                if (currentQuestion != null) {
                                    if (currentQuestion.options == null) {
                                        currentQuestion.options = mutableListOf()
                                    }
                                    currentQuestion.options?.addAll(currentOptions)
                                    currentOptions = mutableListOf()
                                }
                            }
                            OPTION -> {
                                if (currentOption != null) {
                                    if (lastText != null) {
                                        currentOption.text = lastText
                                        lastText = null
                                    }
                                    currentOptions.add(currentOption)
                                    currentOption = null
                                }
                            }
                            LEVELS -> {
                                currentQuestion?.levels?.addAll(currentLevels)
                                currentLevels = mutableListOf()
                            }
                            LEVEL -> {
                                if (currentLevel != null) {
                                    if (lastText != null) {
                                        currentLevel.text = lastText
                                        lastText = null
                                    }
                                    currentLevels.add(currentLevel)
                                    currentLevel = null
                                }
                            }
                            DEPENDENCY -> {
                                //TODO: add dependency
                            }
                            ALT_TEXT -> {
                                //can be inside question, help or option
                                if (currentAltText != null) {
                                    when {
                                        currentOption != null -> {
                                            currentOption.addAltText(currentAltText)
                                        }
                                        currentHelp != null -> {
                                            currentHelp.addAltText(currentAltText)
                                        }
                                        currentQuestion != null -> {
                                            currentQuestion.addAltText(currentAltText)
                                        }
                                    }
                                    currentAltText = null
                                }
                            }
                            HELP -> {
                                if (currentHelp != null) {
                                    if (lastText != null) {
                                        currentHelp.text = lastText
                                        lastText = null
                                    }
                                    currentQuestion?.questionHelp?.add(currentHelp)
                                    currentHelp = null
                                }

                            }
                        }
                    }
                    /**
                     * this is the <text>some text</text> content
                     * <text> can appear in multiple places: inside question, option, level, help
                     */
                    XmlPullParser.TEXT -> {
                        lastText = parser.text
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            Timber.e(e)
        } catch (e: IOException) {
            Timber.e(e)
        } finally {
            helper.close(inputStream)
        }
        return map
    }

    fun parseXmlFormWithMeta(inputStream: InputStream, backUpVersion: Double? = null): Pair<Form, SurveyMetadata> {
        val groups: MutableList<QuestionGroup> = mutableListOf()
        var version = backUpVersion ?: 0.0
        var name = ""
        var formId = -1
        var surveyId = -1L
        var defaultLanguage = ""
        val parserFactory: XmlPullParserFactory
        var currentQuestionGroup: QuestionGroup? = null
        var currentQuestion: Question? = null
        var currentOptions = mutableListOf<Option>()
        var currentOption: Option? = null
        var currentLevels = mutableListOf<Level>()
        var currentLevel: Level? = null
        var currentAltText: AltText? = null
        var currentHelp: QuestionHelp? = null
        var lastText: String? = null
        var groupOrder = 0
        var surveyMetadata = SurveyMetadata()
        try {
            parserFactory = XmlPullParserFactory.newInstance()
            val parser = parserFactory.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {

                when (eventType) {
                    /**
                     * Beginning of a tag for example <question>
                     */
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            SURVEY -> {
                                val surveyName = getStringAttribute(parser, SURVEY_GROUP_NAME)
                                val registrationFormId = getStringAttribute(parser, REGISTRATION_SURVEY)
                                val app = getStringAttribute(parser, APP)
                                val alias = getNonNullStringAttribute(parser, ALIAS)

                                version = getDoubleAttribute(parser, VERSION)
                                name = getNonNullStringAttribute(parser, NAME)
                                formId = getIntAttribute(parser, FORM_ID, -1)
                                surveyId = getNonNullLongAttribute(parser, SURVEY_ID)
                                val parsedAttribute = parser.getAttributeValue(null, DEFAULT_LANG)
                                if (!parsedAttribute.isNullOrEmpty()) {
                                    defaultLanguage = parsedAttribute
                                }
                                val surveyGroup = SurveyGroup(surveyId,
                                    surveyName,
                                    registrationFormId,
                                    registrationFormId != null)
                                surveyMetadata = SurveyMetadata(app = app,
                                    alias = alias,
                                    surveyGroup = surveyGroup)
                            }
                            QUESTION_GROUP -> {
                                groupOrder++
                                val repeatable = "true" == getStringAttribute(parser, REPEATABLE)
                                val groupId = getLongAttribute(parser, "groupId")
                                currentQuestionGroup = QuestionGroup(groupId,
                                    "",
                                    repeatable = repeatable,
                                    formId.toString(), groupOrder)
                            }
                            QUESTION_GROUP_HEADING -> {
                                currentQuestionGroup?.heading = parser.nextText()
                            }
                            QUESTION -> {
                                val resource = getStringAttribute(parser, CASCADE_RESOURCE)
                                var order = getIntAttribute(parser, ORDER, -1)
                                if (order == -1) {
                                    order = 1
                                    if (currentQuestionGroup != null) {
                                        order = currentQuestionGroup.questions.size + 1
                                    }
                                }

                                currentQuestion = Question(
                                    cascadeResource = resource,
                                    order = order,
                                    isMandatory = getBooleanAttribute(parser, MANDATORY),
                                    isLocked = getBooleanAttribute(parser, LOCKED),
                                    isDoubleEntry = getBooleanAttribute(parser, DOUBLE_ENTRY),
                                    isAllowMultiple = getBooleanAttribute(parser, ALLOW_MULT),
                                    type = parser.getAttributeValue(null, TYPE),
                                    questionId = parser.getAttributeValue(null, ID),
                                    isLocaleName = getBooleanAttribute(parser, LOCALE_NAME),
                                    isLocaleLocation = getBooleanAttribute(parser, LOCALE_LOCATION),
                                    caddisflyRes = getStringAttribute(parser, CADDISFLY_RESOURCE),
                                    isAllowPoints = getBooleanAttribute(parser, ALLOW_POINTS),
                                    isAllowLine = getBooleanAttribute(parser, ALLOW_LINE),
                                    isAllowPolygon = getBooleanAttribute(parser, ALLOW_POLYGON)
                                )
                            }
                            OPTIONS -> {
                                currentOptions = mutableListOf()
                                if (currentQuestion != null) {
                                    currentQuestion.isAllowOther = getBooleanAttribute(
                                        parser,
                                        ALLOW_OTHER
                                    )
                                    currentQuestion.isAllowMultiple = getBooleanAttribute(
                                        parser,
                                        ALLOW_MULT
                                    )
                                }
                            }
                            OPTION -> {
                                currentOption = Option(code = getStringAttribute(parser, CODE))
                            }
                            LEVELS -> {
                                currentLevels = mutableListOf()
                            }
                            LEVEL -> {
                                currentLevel = Level()
                            }
                            DEPENDENCY -> {
                                currentQuestion?.dependencies?.add(
                                    Dependency(
                                        question = getStringAttribute(parser, QUESTION),
                                        answer = getStringAttribute(parser, ANSWER)
                                    )
                                )
                            }
                            VALIDATION_RULE -> {
                                currentQuestion?.validationRule = parseValidationValue(parser)
                            }
                            ALT_TEXT -> {
                                currentAltText = AltText(
                                    languageCode = getStringAttribute(parser, LANG),
                                    type = getStringAttribute(parser, TYPE)
                                )
                            }
                            HELP -> {
                                currentHelp = QuestionHelp()
                            }

                        }
                    }
                    /**
                     * End of a tag for example </question>
                     */
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            QUESTION_GROUP -> {
                                currentQuestionGroup?.let {
                                    groups.add(it)
                                }
                            }
                            QUESTION -> {
                                if (currentQuestionGroup != null && currentQuestion != null) {
                                    if (lastText != null) {
                                        currentQuestion.text = lastText
                                        lastText = null
                                    }
                                    currentQuestionGroup.questions.add(currentQuestion)
                                    currentQuestion = null
                                }
                            }
                            OPTIONS -> {
                                if (currentQuestion != null) {
                                    if (currentQuestion.options == null) {
                                        currentQuestion.options = mutableListOf()
                                    }
                                    currentQuestion.options?.addAll(currentOptions)
                                    currentOptions = mutableListOf()
                                }
                            }
                            OPTION -> {
                                if (currentOption != null) {
                                    if (lastText != null) {
                                        currentOption.text = lastText
                                        lastText = null
                                    }
                                    currentOptions.add(currentOption)
                                    currentOption = null
                                }
                            }
                            LEVELS -> {
                                currentQuestion?.levels?.addAll(currentLevels)
                                currentLevels = mutableListOf()
                            }
                            LEVEL -> {
                                if (currentLevel != null) {
                                    if (lastText != null) {
                                        currentLevel.text = lastText
                                        lastText = null
                                    }
                                    currentLevels.add(currentLevel)
                                    currentLevel = null
                                }
                            }
                            DEPENDENCY -> {
                                //nothing to do
                            }
                            ALT_TEXT -> {
                                //can be inside question, help or option
                                if (currentAltText != null) {
                                    when {
                                        currentOption != null -> {
                                            currentOption.addAltText(currentAltText)
                                        }
                                        currentHelp != null -> {
                                            currentHelp.addAltText(currentAltText)
                                        }
                                        currentQuestion != null -> {
                                            currentQuestion.addAltText(currentAltText)
                                        }
                                    }
                                    currentAltText = null
                                }
                            }
                            HELP -> {
                                if (currentHelp != null) {
                                    if (lastText != null) {
                                        currentHelp.text = lastText
                                        lastText = null
                                    }
                                    currentQuestion?.questionHelp?.add(currentHelp)
                                    currentHelp = null
                                }

                            }
                        }
                    }
                    /**
                     * this is the <text>some text</text> content
                     * <text> can appear in multiple places: inside question, option, level, help
                     */
                    XmlPullParser.TEXT -> {
                        lastText = parser.text
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            Timber.e(e)
        } catch (e: IOException) {
            Timber.e(e)
        } finally {
            helper.close(inputStream)
        }
        val form = Form(formId, formId.toString(),
            surveyId,
            name = name,
            version = version,
            filename = "$formId.xml",
            language = defaultLanguage,
            groups = groups)
        return Pair(form, surveyMetadata)
    }

    private fun parseValidationValue(parser: XmlPullParser): ValidationRule {
       val currentValidation = ValidationRule(getStringAttribute(parser, VALIDATION_TYPE))
        currentValidation.allowDecimal = getBooleanAttribute(parser, ALLOW_DEC)
        currentValidation.allowSigned = getBooleanAttribute(parser, ALLOW_SIGN)
        currentValidation.maxLength = getIntAttribute(parser, MAX_LENGTH, DEFAULT_MAX_LENGTH)
        val minValue: Double? = parser.getAttributeValue(null, MIN_VAL)?.toDouble()
        val maxValue: Double? = parser.getAttributeValue(null, MAX_VAL)?.toDouble()
        if (minValue != null) {
            currentValidation.minVal = minValue
        }
        if (maxValue != null) {
            currentValidation.maxVal = maxValue
        }
        return currentValidation
    }

    private fun getIntAttribute(parser: XmlPullParser, attributeName: String, defaultValue: Int) =
        parser.getAttributeValue(null, attributeName)?.toInt() ?: defaultValue

    private fun getStringAttribute(parser: XmlPullParser, attributeName: String) =
        parser.getAttributeValue(null, attributeName)

    private fun getNonNullStringAttribute(parser: XmlPullParser, attributeName: String): String {
        return parser.getAttributeValue(null, attributeName) ?: ""
    }

    private fun getDoubleAttribute(parser: XmlPullParser, attributeName: String) =
        parser.getAttributeValue(null, attributeName)?.toDouble() ?: 0.0

    private fun getLongAttribute(parser: XmlPullParser, attributeName: String): Long? =
        parser.getAttributeValue(null, attributeName)?.toLong()

    private fun getNonNullLongAttribute(parser: XmlPullParser, attributeName: String): Long =
        getLongAttribute(parser, attributeName) ?: -1L

    private fun getBooleanAttribute(parser: XmlPullParser, attributeName: String) =
        parser.getAttributeValue(null, attributeName)?.toBoolean() ?: false

    companion object {
        private const val OPTIONS = "options"
        private const val QUESTION = "question"
        private const val QUESTION_GROUP = "questionGroup"
        private const val QUESTION_GROUP_HEADING = "heading"
        private const val SURVEY = "survey"
        private const val CASCADE_RESOURCE = "cascadeResource"
        private const val ORDER = "order"
        private const val MANDATORY = "mandatory"
        private const val LOCKED = "locked"
        private const val DOUBLE_ENTRY = "requireDoubleEntry"
        private const val LOCALE_NAME = "localeNameFlag"
        private const val LOCALE_LOCATION = "localeLocationFlag"
        private const val CADDISFLY_RESOURCE = "caddisflyResourceUuid"
        private const val ALLOW_OTHER = "allowOther"
        private const val ALLOW_MULT = "allowMultiple"
        private const val OPTION = "option"
        private const val CODE = "code"
        private const val ALLOW_POINTS = "allowPoints"
        private const val ALLOW_LINE = "allowLine"
        private const val ALLOW_POLYGON = "allowPolygon"
        private const val TYPE = "type"
        private const val ID = "id"
        private const val VERSION = "version"
        private const val DEFAULT_LANG = "defaultLanguageCode"
        private const val ALT_TEXT = "altText"
        private const val LANG = "language"
        private const val NAME = "name"
        private const val REPEATABLE = "repeatable"
        private const val LEVELS = "levels"
        private const val LEVEL = "level"
        private const val DEPENDENCY = "dependency"
        private const val ANSWER = "answer-value"
        private const val VALIDATION_RULE = "validationRule"
        private const val MIN_VAL = "minVal"
        private const val MAX_VAL = "maxVal"
        private const val VALIDATION_TYPE = "validationType"
        private const val MAX_LENGTH = "maxLength"
        private const val ALLOW_DEC = "allowDecimal"
        private const val ALLOW_SIGN = "signed"
        private const val HELP = "help"
        private const val FORM_ID = "surveyId"
        private const val SURVEY_ID = "surveyGroupId"
        private const val APP = "app"
        private const val SURVEY_GROUP_NAME = "surveyGroupName"
        private const val REGISTRATION_SURVEY = "registrationSurvey"
        private const val ALIAS = "alias"
    }
}
