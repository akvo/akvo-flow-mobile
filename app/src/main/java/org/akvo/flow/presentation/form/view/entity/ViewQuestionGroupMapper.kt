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

package org.akvo.flow.presentation.form.view.entity

import android.os.Bundle
import android.text.TextUtils
import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.akvo.flow.domain.entity.DomainQuestionGroup
import org.akvo.flow.domain.entity.Response
import org.akvo.flow.domain.entity.question.AltText
import org.akvo.flow.domain.entity.question.Level
import org.akvo.flow.domain.entity.question.Option
import org.akvo.flow.domain.entity.question.Question
import org.akvo.flow.domain.util.GsonMapper
import org.akvo.flow.presentation.form.view.groups.entity.ViewCascadeLevel
import org.akvo.flow.presentation.form.view.groups.entity.ViewLocation
import org.akvo.flow.presentation.form.view.groups.entity.ViewOption
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer
import org.akvo.flow.util.ConstantUtil.CASCADE_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.DATE_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.FREE_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.GEO_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.OPTION_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.PHOTO_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.VIDEO_QUESTION_TYPE
import org.json.JSONArray
import org.json.JSONException
import timber.log.Timber
import java.util.ArrayList
import java.util.HashMap
import javax.inject.Inject

class ViewQuestionGroupMapper @Inject constructor() {

    fun transform(
        groups: List<DomainQuestionGroup>,
        responses: List<Response>
    ): List<ViewQuestionGroup> {
        val viewGroups: MutableList<ViewQuestionGroup> = mutableListOf()
        for (g in groups) {
            viewGroups.add(transform(g, responses))
        }
        return viewGroups
    }

    private fun transform(
        group: DomainQuestionGroup,
        responses: List<Response>
    ): ViewQuestionGroup {
        return ViewQuestionGroup(
            group.heading, group.isRepeatable, listOfAnswers(
                group.questions,
                responses
            )
        )
    }

    private fun listOfAnswers(
        questions: MutableList<Question>,
        responses: List<Response>
    ): ArrayList<ViewQuestionAnswer> {
        val answers = arrayListOf<ViewQuestionAnswer>()
        questions.forEach { question ->
            val listOfResponses: List<Response> = getResponsesForQuestion(
                question.questionId
            )
            answers.add(createQuestionAnswer(question, listOfResponses))
        }
        return answers
    }

    //TODO: consider how to display repeatable question groups
    //Add repetition object and inside all the questions
    private fun createQuestionAnswer(
        question: Question,
        listOfResponses: List<Response>
    ): ViewQuestionAnswer {
        //first response only for now //TODO: check if works for repeatable ones
        val answer = if (listOfResponses.isNotEmpty()) {
            listOfResponses[0].value
        } else {
            ""
        }
        when (question.type) {
            FREE_QUESTION_TYPE -> {
                return ViewQuestionAnswer.FreeTextViewQuestionAnswer(
                    question.questionId ?: "",
                    question.order.toString() + question.text,
                    question.isMandatory,
                    mapToBundle(question.languageTranslationMap),
                    answer,
                    question.isDoubleEntry
                )
            }
            OPTION_QUESTION_TYPE -> {
                return ViewQuestionAnswer.OptionViewQuestionAnswer(
                    question.questionId ?: "",
                    question.order.toString() + question.text,
                    question.isMandatory,
                    mapToBundle(question.languageTranslationMap),
                    mapToViewOption(question.options, answer),
                    question.isDoubleEntry
                )
            }

            CASCADE_QUESTION_TYPE -> {
                return ViewQuestionAnswer.CascadeViewQuestionAnswer(
                    question.questionId ?: "",
                    question.order.toString() + question.text,
                    question.isMandatory,
                    mapToBundle(question.languageTranslationMap),
                    mapToCascadeResponse(answer, question.levels)
                )
            }
            GEO_QUESTION_TYPE -> {
                return ViewQuestionAnswer.LocationViewQuestionAnswer(
                    question.questionId ?: "",
                    question.order.toString() + question.text,
                    question.isMandatory,
                    mapToBundle(question.languageTranslationMap),
                    mapToLocationResponse(answer)
                )
            }
            PHOTO_QUESTION_TYPE -> {
                val media = deserializeMedia(answer)
                val viewLocation = if (media.location != null) {
                    ViewLocation(
                        media.location.latitude.toString(),
                        media.location.longitude.toString(),
                        media.location.altitude.toString(),
                        media.location.accuracy.toString()
                    )
                } else {
                    null
                }
                return ViewQuestionAnswer.PhotoViewQuestionAnswer(
                    question.questionId ?: "",
                    question.order.toString() + question.text,
                    question.isMandatory,
                    mapToBundle(question.languageTranslationMap),
                    media.filename,
                    viewLocation
                )
            }
            VIDEO_QUESTION_TYPE -> {
                val media = deserializeMedia(answer)
                return ViewQuestionAnswer.VideoViewQuestionAnswer(
                    question.questionId ?: "",
                    question.order.toString() + question.text,
                    question.isMandatory,
                    mapToBundle(question.languageTranslationMap),
                    media.filename
                )
            }
            DATE_QUESTION_TYPE -> {
                return ViewQuestionAnswer.DateViewQuestionAnswer(
                    question.questionId ?: "",
                    question.order.toString() + question.text,
                    question.isMandatory,
                    mapToBundle(question.languageTranslationMap),
                    answer
                )
            }
        }
    }

    private fun deserializeMedia(data: String): Media {
        if (TextUtils.isEmpty(data)) {
            return Media("")
        }
        try {
            val mapper = GsonMapper(GsonBuilder().create())
            return mapper.read(data, Media::class.java)
        } catch (e: JsonIOException) {
            Timber.e("Value is not a valid JSON response: $data")
        } catch (e: JsonSyntaxException) {
            Timber.e("Value is not a valid JSON response: $data")
        }

        // Assume old format - plain image
        return Media(data)
    }

    private fun mapToLocationResponse(value: String): ViewLocation {
        val tokens = value.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val latitude = getLatitudeFromResponseToken(tokens)
        val longitude = getLongitudeFromToken(tokens)
        val altitude = getAltitudeFromToken(tokens)
        return ViewLocation(latitude, longitude, altitude)
    }

    private fun getLatitudeFromResponseToken(token: Array<String>?): String {
        return if (token == null || token.isEmpty()) {
            ""
        } else token[POSITION_LATITUDE]
    }

    private fun getLongitudeFromToken(token: Array<String>?): String {
        return if (token == null || token.size <= POSITION_LONGITUDE) {
            ""
        } else token[POSITION_LONGITUDE]
    }

    private fun getAltitudeFromToken(token: Array<String>?): String {
        return if (token == null || token.size <= POSITION_ALTITUDE) {
            ""
        } else token[POSITION_ALTITUDE]
    }

    private fun mapToViewOption(
        options: MutableList<Option>?,
        response: String
    ): MutableList<ViewOption> {
        val viewOptions = mutableListOf<ViewOption>()
        val selectedOptions = deserializeToOptions(response)
        if (options != null) {
            for (option in options) {
                viewOptions.add(
                    ViewOption(
                        option.text ?: "",
                        option.code ?: "",
                        option.isOther,
                        selectedOptions.contains(option.text)
                    )
                )
            }
        }
        return viewOptions
    }

    private fun mapToBundle(languageTranslationMap: HashMap<String?, AltText>): Bundle {
        val bundle = Bundle()
        for (key in languageTranslationMap.keys) {
            val text = languageTranslationMap[key]?.text
            if (text != null) {
                bundle.putString(key, text)
            }
        }
        return bundle
    }

    private fun deserializeToOptions(data: String): MutableList<String> {
        try {
            val options: MutableList<String> = ArrayList()
            val jOptions = JSONArray(data)
            for (i in 0 until jOptions.length()) {
                val jOption = jOptions.getJSONObject(i)
                options.add(jOption.optString(TEXT))
            }
            return options
        } catch (e: JSONException) {
            Timber.e(e)
        }

        // Default to old format
        val options: MutableList<String> = ArrayList()
        val tokens = data.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (token in tokens) {
            options.add(token)
        }
        return options
    }

    private fun mapToCascadeResponse(
        data: String,
        levels: MutableList<Level>
    ): List<ViewCascadeLevel> {
        val values: MutableList<ViewCascadeLevel> = ArrayList()
        val cascadeLevels: ArrayList<CascadeLevel> = ArrayList()
        try {
            val listType = object : TypeToken<ArrayList<CascadeLevel>>() {}.type
            val mapper = GsonMapper(GsonBuilder().create())
            cascadeLevels.addAll(mapper.read(data, listType))
        } catch (e: JsonSyntaxException) {
            Timber.e("Value is not a valid JSON response: $data")
            // Default to old format
            val tokens = data.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (token in tokens) {
                val v = CascadeLevel("", token)
                cascadeLevels.add(v)
            }
        }
        for ((index, l) in levels.withIndex()) {
            val answer = if (cascadeLevels.size > index) {
                cascadeLevels[index].name
            } else {
                ""
            }
            values.add(ViewCascadeLevel(l.text ?: "", answer))
        }
        return values
    }

    /**
     * There may be a list of responses if the question is repeatable
     */
    private fun getResponsesForQuestion(
        questionId: String?
    ): List<Response> {
        val questionResponses = mutableListOf<Response>()
        for (r in questionResponses) {
            if (r.questionId == questionId) {
                questionResponses.add(r)
            }
        }
        return questionResponses
    }

    companion object {
        private const val TEXT = "text"
        private const val RESPONSE_DELIMITER = "|"
        private const val POSITION_LATITUDE = 0
        private const val POSITION_LONGITUDE = 1
        private const val POSITION_ALTITUDE = 2
    }

    //these should come from domain already mapped
    data class CascadeLevel(val code: String, val name: String)

    data class Media(val filename: String, val location: Location? = null)

    data class Location(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val accuracy: Float
    )
}
