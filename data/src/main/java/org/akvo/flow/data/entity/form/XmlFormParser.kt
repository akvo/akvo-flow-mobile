/*
 * Copyright (C) 2019-2020 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.data.util.FileHelper
import org.akvo.flow.domain.entity.DomainQuestionGroup
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.LinkedHashSet
import javax.inject.Inject

class XmlFormParser @Inject constructor(private val helper: FileHelper) {

    fun parse(input: InputStream?): Form {
        val resources: MutableList<String> = ArrayList()
        var version = "0.0"
        val parserFactory: XmlPullParserFactory
        try {
            parserFactory = XmlPullParserFactory.newInstance()
            val parser = parserFactory.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(input, null)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                var eltName: String
                if (eventType == XmlPullParser.START_TAG) {
                    eltName = parser.name
                    if (SURVEY == eltName) {
                        version = parser.getAttributeValue(null, VERSION)
                    } else if (QUESTION == eltName) {
                        val resource = parser.getAttributeValue(null, CASCADE_RESOURCE)
                        if (!resource.isNullOrEmpty()) {
                            resources.add(resource)
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
        return Form(resources, version)
    }

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

    fun parseToDomainForm(inputStream: InputStream): XmlDataForm {
        val groups: MutableList<DomainQuestionGroup> = ArrayList()
        var version = "0.0"
        var name = ""
        val parserFactory: XmlPullParserFactory
        var questionGroup: DomainQuestionGroup? = null
        try {
            parserFactory = XmlPullParserFactory.newInstance()
            val parser = parserFactory.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            SURVEY -> {
                                val attributeValue = parser.getAttributeValue(null, VERSION)
                                if (attributeValue != null) {
                                    version = attributeValue
                                }
                                val attributeValue1 = parser.getAttributeValue(null, NAME)
                                if (attributeValue1 != null) {
                                    name = attributeValue1
                                }
                            }
                            QUESTION_GROUP -> {
                                val repeatable = "true" == parser.getAttributeValue(null, REPEATABLE)
                                questionGroup = DomainQuestionGroup("", repeatable)
                            }
                            QUESTION_GROUP_HEADING -> {
                                questionGroup?.heading = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (QUESTION_GROUP == parser.name && questionGroup != null) {
                            groups.add(questionGroup)
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
            helper.close(inputStream)
        }
        return XmlDataForm(name = name, version = version, groups = groups)
    }

    companion object {
        private const val QUESTION = "question"
        private const val QUESTION_GROUP = "questionGroup"
        private const val QUESTION_GROUP_HEADING = "heading"
        private const val SURVEY = "survey"
        private const val CASCADE_RESOURCE = "cascadeResource"
        private const val VERSION = "version"
        private const val DEFAULT_LANG = "defaultLanguageCode"
        private const val ALT_TEXT = "altText"
        private const val LANG = "language"
        private const val NAME = "name"
        private const val REPEATABLE = "repeatable"
    }
}
