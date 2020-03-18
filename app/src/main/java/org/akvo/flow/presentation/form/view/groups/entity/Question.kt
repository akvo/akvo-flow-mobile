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

sealed class Question {

    abstract val questionId: String
    abstract val title: String // title is composed of order + . title ej: 1. Question one
    abstract val mandatory: Boolean
    abstract val translations: List<String>
    //TODO: add tooltip
    //TODO: dependencies?

    data class FreeTextQuestion(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val requireDoubleEntry: Boolean
    ) : Question()

    data class NumberQuestion(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val requireDoubleEntry: Boolean,
        val allowSign: Boolean,
        val allowDecimalPoint: Boolean,
        val minimumValue: Double,
        val maximumValue: Double
    ) : Question()

    data class OptionQuestion(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val allowMultiple: Boolean,
        val allowOther: Boolean
    //TODO: add actual options
    ) : Question()

    data class CascadeQuestion(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val cascadeResource: String
    ) : Question()

    data class LocationQuestion(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val locked: Boolean = false
    ) : Question()

    data class PhotoQuestion(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>
        //TODO: do we need fileName?
    ) : Question()

    data class VideoQuestion(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>
        //TODO: do we need fileName?
    ) : Question()

    //TODO: date answer has to be formatted correctly
    data class DateQuestion(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>
    ) : Question()

    data class BarcodeQuestion(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val enableMultiple: Boolean = false,
        val locked: Boolean = false
    ) : Question()

    data class GeoShapeQuestion(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val enablePoint: Boolean,
        val enableLine: Boolean,
        val enableArea: Boolean,
        val locked: Boolean = false
    ) : Question()

    data class SignatureQuestion(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>
    ) : Question()

    data class CaddisflyQuestion(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val caddisflyResourceUuid: String
    ) : Question()
}

val <T> T.exhaustive: T
    get() = this