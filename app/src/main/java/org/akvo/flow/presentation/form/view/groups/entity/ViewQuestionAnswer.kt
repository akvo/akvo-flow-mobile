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

package org.akvo.flow.presentation.form.view.groups.entity

sealed class ViewQuestionAnswer {

    abstract val questionId: String
    abstract val title: String // title is composed of order + . title ej: 1. Question one
    abstract val mandatory: Boolean
    abstract val translations: List<String>
    //TODO: add tooltip
    //TODO: dependencies?

    data class FreeTextViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answer: String,
        val requireDoubleEntry: Boolean
    ) : ViewQuestionAnswer()

    data class NumberViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answer: String,
        val requireDoubleEntry: Boolean
    ) : ViewQuestionAnswer()

    data class OptionViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val options: List<ViewOption> = emptyList(), //set to empty if no answer
        val allowMultiple: Boolean = false,
        val allowOther: Boolean = false
    ) : ViewQuestionAnswer()

    data class CascadeViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answers: List<ViewCascadeLevel> = emptyList() //empty if no answer
    ) : ViewQuestionAnswer()

    data class LocationViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val viewLocation: ViewLocation
    ) : ViewQuestionAnswer()

    data class PhotoViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val filePath: String,
        val location: ViewLocation? // TODO use valid and invalid location object?
    ) : ViewQuestionAnswer()

    data class VideoViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val filePath: String
    ) : ViewQuestionAnswer()

    //TODO: date answer has to be formatted correctly
    data class DateViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answer: String
    ) : ViewQuestionAnswer()

    data class BarcodeViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answers: List<String>
    ) : ViewQuestionAnswer()

    data class GeoShapeViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val geojsonAnswer: String
    ) : ViewQuestionAnswer()

    data class SignatureViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val base64ImageString: String,
        val name: String
    ) : ViewQuestionAnswer()

    data class CaddisflyViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answers: List<String> //name +": " +value +" " +unit;
    ) : ViewQuestionAnswer()
}

data class ViewLocation(
    val latitude: String,
    val longitude: String,
    val altitude: String = "",
    val accuracy: String = "" //formatted accuracy for displaying
)
{
    fun isValid(): Boolean {
        return latitude.isNotEmpty() && longitude.isNotEmpty()
    }
}

data class ViewOption(val name: String, val code: String = "", val isOther: Boolean = false, val selected: Boolean)

data class ViewCascadeLevel(val level: String, val answer: String)

val <T> T.exhaustive: T
    get() = this
