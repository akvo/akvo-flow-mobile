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
        val requireDoubleEntry: Boolean,
        val allowSign: Boolean,
        val allowDecimalPoint: Boolean,
        val minimumValue: Double,
        val maximumValue: Double
    ) : ViewQuestionAnswer()

    data class OptionViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answer: String,
        val allowMultiple: Boolean,
        val allowOther: Boolean
    //TODO: add actual options
    ) : ViewQuestionAnswer()

    data class CascadeViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answer: String,
        val cascadeResource: String
    ) : ViewQuestionAnswer()

    data class LocationViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answer: String,
        val locked: Boolean = false
    ) : ViewQuestionAnswer()

    data class PhotoViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val filePath: String,
        val location: ImageLocation? // TODO use valid and invalid location object?
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
        val enableMultiple: Boolean = false,
        val locked: Boolean = false
    //TODO: list of answers
    ) : ViewQuestionAnswer()

    data class GeoShapeViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val enablePoint: Boolean,
        val enableLine: Boolean,
        val enableArea: Boolean,
        val locked: Boolean = false
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
        val caddisflyResourceUuid: String
    ) : ViewQuestionAnswer()
}

class ImageLocation(val latitude: String, val longitude: String)

val <T> T.exhaustive: T
    get() = this