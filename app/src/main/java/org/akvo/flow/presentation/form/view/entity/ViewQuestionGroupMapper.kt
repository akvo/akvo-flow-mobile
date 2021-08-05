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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.akvo.flow.domain.entity.DomainQuestionGroup
import org.akvo.flow.domain.entity.Response
import org.akvo.flow.domain.entity.question.AltText
import org.akvo.flow.domain.entity.question.Level
import org.akvo.flow.domain.entity.question.Option
import org.akvo.flow.domain.entity.question.DomainQuestion
import org.akvo.flow.domain.util.GsonMapper
import org.akvo.flow.presentation.form.view.groups.entity.ViewCascadeLevel
import org.akvo.flow.presentation.form.view.groups.entity.ViewLocation
import org.akvo.flow.presentation.form.view.groups.entity.ViewOption
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer
import org.akvo.flow.presentation.form.view.groups.repeatable.GroupRepetition
import org.akvo.flow.util.ConstantUtil.CADDISFLY_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.CASCADE_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.DATE_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.GEOSHAPE_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.GEO_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.OPTION_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.PHOTO_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.SCAN_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.SIGNATURE_QUESTION_TYPE
import org.akvo.flow.util.ConstantUtil.VIDEO_QUESTION_TYPE
import org.json.JSONArray
import org.json.JSONException
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.HashMap
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class ViewQuestionGroupMapper @Inject constructor() {
    //TODO: inject
    private val localCalendar: Calendar = GregorianCalendar.getInstance(Locale.getDefault())
    private val userDisplayedDateFormat: DateFormat = SimpleDateFormat.getDateInstance()

    init {
        userDisplayedDateFormat.timeZone = TimeZone.getDefault()
    }

    fun transform(groups: List<DomainQuestionGroup>, responses: List<Response>): List<ViewQuestionGroup> {
        val viewGroups: MutableList<ViewQuestionGroup> = mutableListOf()
        for (g in groups) {
            viewGroups.add(transform(g, responses))
        }
        return viewGroups
    }

    private fun transform(group: DomainQuestionGroup, responses: List<Response>): ViewQuestionGroup {
        if (group.isRepeatable) {
            val maxRepsNumber: Int = countRepetitions(group.domainQuestions, responses)
            return ViewQuestionGroup(
                group.heading, group.isRepeatable, repetitions = listOfRepetitions(
                    group.domainQuestions,
                    responses,
                    maxRepsNumber
                )
            )
        } else {
            return ViewQuestionGroup(
                group.heading, group.isRepeatable, questionAnswers = listOfAnswers(
                    group.domainQuestions,
                    responses,
                    0
                )
            )
        }
    }

    private fun listOfRepetitions(
        domainQuestions: MutableList<DomainQuestion>,
        responses: List<Response>,
        maxRepsNumber: Int,
    ): ArrayList<GroupRepetition> {
        val groupRepetitions = arrayListOf<GroupRepetition>()
        for (repetition in 0 until maxRepsNumber) {
            val header = "Repetition " + (repetition + 1)
            Timber.d(header)
            groupRepetitions.add(GroupRepetition(header,
                listOfAnswers(domainQuestions, responses, repetition)))
        }
        return groupRepetitions
    }

    private fun countRepetitions(domainQuestions: MutableList<DomainQuestion>, responses: List<Response>): Int {
        var repetitionsCount = 1
        domainQuestions.forEach { question ->
            val listOfResponses: List<Response> = getResponsesForQuestion(
                question.questionId, responses
            )
            if (listOfResponses.size > repetitionsCount) {
                repetitionsCount = listOfResponses.size
            }
        }
        return repetitionsCount
    }

    private fun listOfAnswers(
        domainQuestions: MutableList<DomainQuestion>,
        responses: List<Response>,
        repetition: Int
    ): ArrayList<ViewQuestionAnswer> {
        val answers = arrayListOf<ViewQuestionAnswer>()
        domainQuestions.forEach { question ->
            val listOfResponses: List<Response> = getResponsesForQuestion(
                question.questionId, responses
            )
            answers.add(createQuestionAnswer(question, listOfResponses, repetition))
        }
        return answers
    }

    private fun createQuestionAnswer(
        domainQuestion: DomainQuestion,
        listOfResponses: List<Response>,
        repetition: Int
    ): ViewQuestionAnswer {
        val answer = if (listOfResponses.isNotEmpty() && listOfResponses.size > repetition) {
            listOfResponses[repetition].value
        } else {
            ""
        }
        val title = """${domainQuestion.order}. ${domainQuestion.text}"""
        return when (domainQuestion.type) {
            OPTION_QUESTION_TYPE -> {
                ViewQuestionAnswer.OptionViewQuestionAnswer(
                    domainQuestion.questionId ?: "",
                    title,
                    domainQuestion.isMandatory,
                    mapToBundle(domainQuestion.languageTranslationMap),
                    mapToViewOption(domainQuestion.options, answer, domainQuestion.isAllowOther),
                    domainQuestion.isAllowMultiple,
                    domainQuestion.isAllowOther
                )
            }

            CASCADE_QUESTION_TYPE -> {
                ViewQuestionAnswer.CascadeViewQuestionAnswer(
                    domainQuestion.questionId ?: "",
                    title,
                    domainQuestion.isMandatory,
                    mapToBundle(domainQuestion.languageTranslationMap),
                    mapToCascadeResponse(answer, domainQuestion.levels)
                )
            }
            GEO_QUESTION_TYPE -> {
                ViewQuestionAnswer.LocationViewQuestionAnswer(
                    domainQuestion.questionId ?: "",
                    title,
                    domainQuestion.isMandatory,
                    mapToBundle(domainQuestion.languageTranslationMap),
                    mapToLocationResponse(answer)
                )
            }
            PHOTO_QUESTION_TYPE -> {
                val (media, viewLocation) = mapMediaLocation(answer)
                ViewQuestionAnswer.PhotoViewQuestionAnswer(
                    domainQuestion.questionId ?: "",
                    title,
                    domainQuestion.isMandatory,
                    mapToBundle(domainQuestion.languageTranslationMap),
                    media.filename,
                    viewLocation
                )
            }
            VIDEO_QUESTION_TYPE -> {
                val media = deserializeMedia(answer)
                ViewQuestionAnswer.VideoViewQuestionAnswer(
                    domainQuestion.questionId ?: "",
                    title,
                    domainQuestion.isMandatory,
                    mapToBundle(domainQuestion.languageTranslationMap),
                    media.filename
                )
            }
            DATE_QUESTION_TYPE -> {
                ViewQuestionAnswer.DateViewQuestionAnswer(
                    domainQuestion.questionId ?: "",
                    title,
                    domainQuestion.isMandatory,
                    mapToBundle(domainQuestion.languageTranslationMap),
                    formatDate(answer)
                )
            }
            SCAN_QUESTION_TYPE -> {
                ViewQuestionAnswer.BarcodeViewQuestionAnswer(
                    domainQuestion.questionId ?: "",
                    title,
                    domainQuestion.isMandatory,
                    mapToBundle(domainQuestion.languageTranslationMap),
                    formatBarcode(answer),
                    domainQuestion.isAllowMultiple
                )
            }
            GEOSHAPE_QUESTION_TYPE -> {
                ViewQuestionAnswer.GeoShapeViewQuestionAnswer(
                    domainQuestion.questionId ?: "",
                    title,
                    domainQuestion.isMandatory,
                    mapToBundle(domainQuestion.languageTranslationMap),
                    answer, //Unformatted answer
                )
            }
            SIGNATURE_QUESTION_TYPE -> {
                val signature: Signature = mapToSignature(answer)
                ViewQuestionAnswer.SignatureViewQuestionAnswer(
                    domainQuestion.questionId ?: "",
                    title,
                    domainQuestion.isMandatory,
                    mapToBundle(domainQuestion.languageTranslationMap),
                    signature.image,
                    signature.name
                )
            }
            CADDISFLY_QUESTION_TYPE -> {
                ViewQuestionAnswer.CaddisflyViewQuestionAnswer(
                    domainQuestion.questionId ?: "",
                    title,
                    domainQuestion.isMandatory,
                    mapToBundle(domainQuestion.languageTranslationMap),
                    mapCaddisfly(answer)
                )
            }
            else -> {
                //free text or number
                ViewQuestionAnswer.FreeTextViewQuestionAnswer(
                    domainQuestion.questionId ?: "",
                    title,
                    domainQuestion.isMandatory,
                    mapToBundle(domainQuestion.languageTranslationMap),
                    answer,
                    domainQuestion.isDoubleEntry
                )
            }
        }
    }

    private fun mapCaddisfly(answer: String?): List<String> {
        val resultsToDisplay = mutableListOf<String>()
        val results = mutableListOf<CaddisflyTestResult>()
        if (!TextUtils.isEmpty(answer)) {
            try {
                val gson = Gson()
                val caddisflyResult = gson.fromJson(answer, CaddisflyResult::class.java)
                if (caddisflyResult != null) {
                    results.addAll(caddisflyResult.results)
                }
            } catch (e: JsonSyntaxException) {
                Timber.e("Unable to parse caddisfly result: %s", answer)
            }
        }
        for (r in results) {
            resultsToDisplay.add(r.buildResultToDisplay())
        }
        return resultsToDisplay
    }

    private fun mapToSignature(answer: String): Signature {
        if (!TextUtils.isEmpty(answer)) {
            try {
                val mapper = GsonMapper(GsonBuilder().create())
                return mapper.read(answer, Signature::class.java)
            } catch (e: JsonSyntaxException) {
                Timber.e("Value is not a valid JSON response: %s", answer)
            }
        }
        return Signature("", "")
    }

    private fun mapMediaLocation(answer: String): Pair<Media, ViewLocation?> {
        val media = deserializeMedia(answer)
        val location = media.location
        val viewLocation = if (location != null) {
            ViewLocation(
                location.latitude.toString(),
                location.longitude.toString(),
                location.altitude.toString(),
                location.accuracy.toString()
            )
        } else {
            null
        }
        return Pair(media, viewLocation)
    }

    private fun formatBarcode(answer: String): List<String> {
        val values = answer.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return values.asList()
    }

    private fun formatDate(answer: String): String {
        return try {
            val timeStamp = answer.toLong()
            localCalendar.timeInMillis = timeStamp
            userDisplayedDateFormat.format(localCalendar.time)
        } catch (e: NumberFormatException) {
            Timber.e(e)
            ""
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
        return ViewLocation(latitude = latitude, longitude = longitude, altitude = altitude)
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
        response: String, allowOther: Boolean
    ): MutableList<ViewOption> {
        val viewOptions = mutableListOf<ViewOption>()
        val selectedOptions: MutableList<String> = deserializeToOptions(response)
        val optionsToRemove = mutableListOf<String>()
        if (options != null) {
            for (option in options) {
                val selected = selectedOptions.contains(option.text)
                viewOptions.add(
                    ViewOption(
                        option.text ?: "",
                        option.code ?: "",
                        option.isOther,
                        selected
                    )
                )
                if (selected && option.text != null) {
                    optionsToRemove.add(option.text!!)
                }
            }
            if (allowOther) {
                selectedOptions.removeAll(optionsToRemove)
                val otherSelected = selectedOptions.size > 0
                val otherName = if (otherSelected) "Other: " + selectedOptions[0] else "Other"
                viewOptions.add(ViewOption(name = otherName, code = "OTHER", isOther = true, otherSelected))
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
            Timber.e("Value is not a valid JSON response: $data")
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
            val elements: ArrayList<CascadeLevel>? = mapper.read(data, listType)
            if (elements != null) {
                cascadeLevels.addAll(elements)
            }
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
    private fun getResponsesForQuestion(questionId: String?, responses: List<Response>): List<Response> {
        val questionResponses = mutableListOf<Response>()
        for (r in responses) {
            if (r.questionId == questionId) {
                questionResponses.add(r)
            }
        }
        return questionResponses
    }

    companion object {
        private const val TEXT = "text"
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

    data class Signature(
        var name: String,
        val image: String
    )

    data class CaddisflyResult(@SerializedName("result") var results: List<CaddisflyTestResult>)

    data class CaddisflyTestResult(
        var id: Int,
        var name: String,
        var value: String,
        var unit: String
    ) {

        fun buildResultToDisplay(): String {
            return name +
                    ": " +
                    value +
                    " " +
                    unit
        }
    }
}
