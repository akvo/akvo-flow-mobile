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

import android.os.Bundle
import android.os.Parcel
import org.akvo.flow.domain.entity.KParcelable
import org.akvo.flow.domain.entity.createStringArrayNonNull
import org.akvo.flow.domain.entity.createTypedArrayNonNull
import org.akvo.flow.domain.entity.parcelableCreator
import org.akvo.flow.domain.entity.readStringNonNull
import org.akvo.flow.domain.entity.safeReadBoolean
import org.akvo.flow.domain.entity.safeReadBundle
import org.akvo.flow.domain.entity.safeWriteBoolean

sealed class ViewQuestionAnswer : KParcelable {

    abstract val questionId: String
    abstract val title: String // title is composed of order + . title ej: 1. Question one
    abstract val mandatory: Boolean
    abstract val translations: Bundle

    //TODO: add tooltip
    //TODO: dependencies?

    data class FreeTextViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: Bundle = Bundle(),
        val answer: String,
        val requireDoubleEntry: Boolean,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean(),
            parcel.safeReadBundle(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.safeWriteBoolean(mandatory)
            parcel.writeBundle(translations)
            parcel.writeString(answer)
            parcel.safeWriteBoolean(requireDoubleEntry)
        }

        companion object {
            @JvmField
            val CREATOR = parcelableCreator(::FreeTextViewQuestionAnswer)
        }
    }

    //number has fields like allow signed, min max etc...
    //number question type is not defined
    data class NumberViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: Bundle = Bundle(),
        val answer: String,
        val requireDoubleEntry: Boolean,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean(),
            parcel.safeReadBundle(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.safeWriteBoolean(mandatory)
            parcel.writeBundle(translations)
            parcel.writeString(answer)
            parcel.safeWriteBoolean(requireDoubleEntry)
        }

        companion object {
            @JvmField
            val CREATOR = parcelableCreator(::NumberViewQuestionAnswer)
        }
    }

    data class OptionViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: Bundle = Bundle(),
        val options: MutableList<ViewOption> = mutableListOf(), //set to empty if no answer
        val allowMultiple: Boolean = false,
        val allowOther: Boolean = false,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean(),
            parcel.safeReadBundle(),
            parcel.createTypedArrayNonNull(ViewOption.CREATOR),
            parcel.safeReadBoolean(),
            parcel.safeReadBoolean()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.safeWriteBoolean(mandatory)
            parcel.writeBundle(translations)
            parcel.writeTypedList(options)
            parcel.safeWriteBoolean(allowMultiple)
            parcel.safeWriteBoolean(allowOther)
        }

        companion object {
            @JvmField
            val CREATOR = parcelableCreator(::OptionViewQuestionAnswer)
        }
    }

    data class CascadeViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: Bundle = Bundle(),
        val answers: List<ViewCascadeLevel> = emptyList(), //empty if no answer
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean(),
            parcel.safeReadBundle(),
            parcel.createTypedArrayNonNull(ViewCascadeLevel.CREATOR)
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.safeWriteBoolean(mandatory)
            parcel.writeBundle(translations)
            parcel.writeTypedList(answers)
        }

        companion object {
            @JvmField
            val CREATOR = parcelableCreator(::CascadeViewQuestionAnswer)
        }
    }

    data class LocationViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: Bundle = Bundle(),
        val viewLocation: ViewLocation?,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean(),
            parcel.safeReadBundle(),
            parcel.readParcelable(ViewLocation::class.java.classLoader)
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.safeWriteBoolean(mandatory)
            parcel.writeBundle(translations)
            parcel.writeParcelable(viewLocation, flags)
        }

        companion object {
            @JvmField
            val CREATOR = parcelableCreator(::LocationViewQuestionAnswer)
        }
    }

    data class PhotoViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: Bundle = Bundle(),
        val filePath: String,
        val location: ViewLocation?, // TODO use valid and invalid location object?
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean(),
            parcel.safeReadBundle(),
            parcel.readStringNonNull(),
            parcel.readParcelable(ViewLocation::class.java.classLoader)
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.safeWriteBoolean(mandatory)
            parcel.writeBundle(translations)
            parcel.writeString(filePath)
            parcel.writeParcelable(location, flags)
        }

        companion object {
            @JvmField
            val CREATOR = parcelableCreator(::PhotoViewQuestionAnswer)
        }
    }

    data class VideoViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: Bundle = Bundle(),
        val filePath: String,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean(),
            parcel.safeReadBundle(),
            parcel.readStringNonNull()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.safeWriteBoolean(mandatory)
            parcel.writeBundle(translations)
            parcel.writeString(filePath)
        }

        companion object {
            @JvmField
            val CREATOR = parcelableCreator(::VideoViewQuestionAnswer)
        }
    }

    //TODO: date answer has to be formatted correctly
    data class DateViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: Bundle = Bundle(),
        val answer: String,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean(),
            parcel.safeReadBundle(),
            parcel.readStringNonNull()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.safeWriteBoolean(mandatory)
            parcel.writeBundle(translations)
            parcel.writeString(answer)
        }

        companion object {
            @JvmField
            val CREATOR = parcelableCreator(::DateViewQuestionAnswer)
        }
    }

    data class BarcodeViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: Bundle = Bundle(),
        val answers: List<String>,
        val allowMultiple: Boolean
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean(),
            parcel.safeReadBundle(),
            parcel.createStringArrayNonNull(),
            parcel.safeReadBoolean(),
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.safeWriteBoolean(mandatory)
            parcel.writeBundle(translations)
            parcel.writeStringList(answers)
            parcel.safeWriteBoolean(allowMultiple)
        }

        companion object {
            @JvmField
            val CREATOR = parcelableCreator(::BarcodeViewQuestionAnswer)
        }
    }

    data class GeoShapeViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: Bundle = Bundle(),
        val geojsonAnswer: String,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean(),
            parcel.safeReadBundle(),
            parcel.readStringNonNull()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.safeWriteBoolean(mandatory)
            parcel.writeBundle(translations)
            parcel.writeString(geojsonAnswer)
        }

        companion object {
            @JvmField
            val CREATOR = parcelableCreator(::GeoShapeViewQuestionAnswer)
        }
    }

    data class SignatureViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: Bundle = Bundle(),
        val base64ImageString: String,
        val name: String,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean(),
            parcel.safeReadBundle(),
            parcel.readStringNonNull(),
            parcel.readStringNonNull()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.safeWriteBoolean(mandatory)
            parcel.writeBundle(translations)
            parcel.writeString(base64ImageString)
            parcel.writeString(name)
        }

        companion object {
            @JvmField
            val CREATOR = parcelableCreator(::SignatureViewQuestionAnswer)
        }
    }

    data class CaddisflyViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: Bundle = Bundle(),
        val answers: List<String>, //name +": " +value +" " +unit;
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.safeReadBoolean(),
            parcel.safeReadBundle(),
            parcel.createStringArrayNonNull()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.safeWriteBoolean(mandatory)
            parcel.writeBundle(translations)
            parcel.writeStringList(answers)
        }

        companion object {
            @JvmField
            val CREATOR = parcelableCreator(::CaddisflyViewQuestionAnswer)
        }
    }
}

data class ViewLocation(
    val latitude: String,
    val longitude: String,
    val altitude: String = "",
    val accuracy: String = "", //formatted accuracy for displaying
) : KParcelable {
    constructor(parcel: Parcel) : this(
        parcel.readStringNonNull(),
        parcel.readStringNonNull(),
        parcel.readStringNonNull(),
        parcel.readStringNonNull()
    )

    fun isValid(): Boolean {
        return latitude.isNotEmpty() && longitude.isNotEmpty()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(latitude)
        parcel.writeString(longitude)
        parcel.writeString(altitude)
        parcel.writeString(accuracy)
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::ViewLocation)
    }
}

data class ViewOption(
    val name: String,
    val code: String = "",
    val isOther: Boolean = false,
    val selected: Boolean,
) : KParcelable {
    constructor(parcel: Parcel) : this(
        parcel.readStringNonNull(),
        parcel.readStringNonNull(),
        parcel.safeReadBoolean(),
        parcel.safeReadBoolean()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(code)
        parcel.safeWriteBoolean(isOther)
        parcel.safeWriteBoolean(selected)
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::ViewOption)
    }
}

data class ViewCascadeLevel(val level: String, val answer: String) : KParcelable {
    constructor(parcel: Parcel) : this(
        parcel.readStringNonNull(),
        parcel.readStringNonNull()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(level)
        parcel.writeString(answer)
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::ViewCascadeLevel)
    }
}

val <T> T.exhaustive: T
    get() = this
