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

import android.os.Parcel
import android.os.Parcelable
import org.akvo.flow.domain.entity.createStringArrayNonNull
import org.akvo.flow.domain.entity.createTypedArrayNonNull
import org.akvo.flow.domain.entity.readStringNonNull

sealed class ViewQuestionAnswer : Parcelable {

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
        val requireDoubleEntry: Boolean,
    ) : ViewQuestionAnswer(), Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte(),
            parcel.createStringArrayNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.writeByte(if (mandatory) 1 else 0)
            parcel.writeStringList(translations)
            parcel.writeString(answer)
            parcel.writeByte(if (requireDoubleEntry) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<FreeTextViewQuestionAnswer> {
            override fun createFromParcel(parcel: Parcel): FreeTextViewQuestionAnswer {
                return FreeTextViewQuestionAnswer(parcel)
            }

            override fun newArray(size: Int): Array<FreeTextViewQuestionAnswer?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class NumberViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answer: String,
        val requireDoubleEntry: Boolean,
    ) : ViewQuestionAnswer(), Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte(),
            parcel.createStringArrayNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.writeByte(if (mandatory) 1 else 0)
            parcel.writeStringList(translations)
            parcel.writeString(answer)
            parcel.writeByte(if (requireDoubleEntry) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<NumberViewQuestionAnswer> {
            override fun createFromParcel(parcel: Parcel): NumberViewQuestionAnswer {
                return NumberViewQuestionAnswer(parcel)
            }

            override fun newArray(size: Int): Array<NumberViewQuestionAnswer?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class OptionViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val options: List<ViewOption> = emptyList(), //set to empty if no answer
        val allowMultiple: Boolean = false,
        val allowOther: Boolean = false,
    ) : ViewQuestionAnswer(), Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte(),
            parcel.createStringArrayNonNull(),
            parcel.createTypedArrayNonNull(ViewOption),
            parcel.readByte() != 0.toByte(),
            parcel.readByte() != 0.toByte())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.writeByte(if (mandatory) 1 else 0)
            parcel.writeStringList(translations)
            parcel.writeTypedList(options)
            parcel.writeByte(if (allowMultiple) 1 else 0)
            parcel.writeByte(if (allowOther) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<OptionViewQuestionAnswer> {
            override fun createFromParcel(parcel: Parcel): OptionViewQuestionAnswer {
                return OptionViewQuestionAnswer(parcel)
            }

            override fun newArray(size: Int): Array<OptionViewQuestionAnswer?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class CascadeViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answers: List<ViewCascadeLevel> = emptyList(), //empty if no answer
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte(),
            parcel.createStringArrayNonNull(),
            parcel.createTypedArrayNonNull(ViewCascadeLevel))

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.writeByte(if (mandatory) 1 else 0)
            parcel.writeStringList(translations)
            parcel.writeTypedList(answers)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<CascadeViewQuestionAnswer> {
            override fun createFromParcel(parcel: Parcel): CascadeViewQuestionAnswer {
                return CascadeViewQuestionAnswer(parcel)
            }

            override fun newArray(size: Int): Array<CascadeViewQuestionAnswer?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class LocationViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val viewLocation: ViewLocation?,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte(),
            parcel.createStringArrayNonNull(),
            parcel.readParcelable(ViewLocation::class.java.classLoader))

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.writeByte(if (mandatory) 1 else 0)
            parcel.writeStringList(translations)
            parcel.writeParcelable(viewLocation, flags)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<LocationViewQuestionAnswer> {
            override fun createFromParcel(parcel: Parcel): LocationViewQuestionAnswer {
                return LocationViewQuestionAnswer(parcel)
            }

            override fun newArray(size: Int): Array<LocationViewQuestionAnswer?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class PhotoViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val filePath: String,
        val location: ViewLocation?, // TODO use valid and invalid location object?
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte(),
            parcel.createStringArrayNonNull(),
            parcel.readStringNonNull(),
            parcel.readParcelable(ViewLocation::class.java.classLoader))

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.writeByte(if (mandatory) 1 else 0)
            parcel.writeStringList(translations)
            parcel.writeString(filePath)
            parcel.writeParcelable(location, flags)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<PhotoViewQuestionAnswer> {
            override fun createFromParcel(parcel: Parcel): PhotoViewQuestionAnswer {
                return PhotoViewQuestionAnswer(parcel)
            }

            override fun newArray(size: Int): Array<PhotoViewQuestionAnswer?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class VideoViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val filePath: String,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte(),
            parcel.createStringArrayNonNull(),
            parcel.readStringNonNull())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.writeByte(if (mandatory) 1 else 0)
            parcel.writeStringList(translations)
            parcel.writeString(filePath)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<VideoViewQuestionAnswer> {
            override fun createFromParcel(parcel: Parcel): VideoViewQuestionAnswer {
                return VideoViewQuestionAnswer(parcel)
            }

            override fun newArray(size: Int): Array<VideoViewQuestionAnswer?> {
                return arrayOfNulls(size)
            }
        }
    }

    //TODO: date answer has to be formatted correctly
    data class DateViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answer: String,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte(),
            parcel.createStringArrayNonNull(),
            parcel.readStringNonNull())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.writeByte(if (mandatory) 1 else 0)
            parcel.writeStringList(translations)
            parcel.writeString(answer)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<DateViewQuestionAnswer> {
            override fun createFromParcel(parcel: Parcel): DateViewQuestionAnswer {
                return DateViewQuestionAnswer(parcel)
            }

            override fun newArray(size: Int): Array<DateViewQuestionAnswer?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class BarcodeViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answers: List<String>,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte(),
            parcel.createStringArrayNonNull(),
            parcel.createStringArrayNonNull())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.writeByte(if (mandatory) 1 else 0)
            parcel.writeStringList(translations)
            parcel.writeStringList(answers)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<BarcodeViewQuestionAnswer> {
            override fun createFromParcel(parcel: Parcel): BarcodeViewQuestionAnswer {
                return BarcodeViewQuestionAnswer(parcel)
            }

            override fun newArray(size: Int): Array<BarcodeViewQuestionAnswer?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class GeoShapeViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val geojsonAnswer: String,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte(),
            parcel.createStringArrayNonNull(),
            parcel.readStringNonNull())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.writeByte(if (mandatory) 1 else 0)
            parcel.writeStringList(translations)
            parcel.writeString(geojsonAnswer)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<GeoShapeViewQuestionAnswer> {
            override fun createFromParcel(parcel: Parcel): GeoShapeViewQuestionAnswer {
                return GeoShapeViewQuestionAnswer(parcel)
            }

            override fun newArray(size: Int): Array<GeoShapeViewQuestionAnswer?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class SignatureViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val base64ImageString: String,
        val name: String,
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte(),
            parcel.createStringArrayNonNull(),
            parcel.readStringNonNull(),
            parcel.readStringNonNull())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.writeByte(if (mandatory) 1 else 0)
            parcel.writeStringList(translations)
            parcel.writeString(base64ImageString)
            parcel.writeString(name)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SignatureViewQuestionAnswer> {
            override fun createFromParcel(parcel: Parcel): SignatureViewQuestionAnswer {
                return SignatureViewQuestionAnswer(parcel)
            }

            override fun newArray(size: Int): Array<SignatureViewQuestionAnswer?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class CaddisflyViewQuestionAnswer(
        override val questionId: String,
        override val title: String,
        override val mandatory: Boolean,
        override val translations: List<String>,
        val answers: List<String>, //name +": " +value +" " +unit;
    ) : ViewQuestionAnswer() {
        constructor(parcel: Parcel) : this(
            parcel.readStringNonNull(),
            parcel.readStringNonNull(),
            parcel.readByte() != 0.toByte(),
            parcel.createStringArrayNonNull(),
            parcel.createStringArrayNonNull())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(questionId)
            parcel.writeString(title)
            parcel.writeByte(if (mandatory) 1 else 0)
            parcel.writeStringList(translations)
            parcel.writeStringList(answers)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<CaddisflyViewQuestionAnswer> {
            override fun createFromParcel(parcel: Parcel): CaddisflyViewQuestionAnswer {
                return CaddisflyViewQuestionAnswer(parcel)
            }

            override fun newArray(size: Int): Array<CaddisflyViewQuestionAnswer?> {
                return arrayOfNulls(size)
            }
        }
    }

}

data class ViewLocation(
    val latitude: String,
    val longitude: String,
    val altitude: String = "",
    val accuracy: String = "", //formatted accuracy for displaying
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readStringNonNull(),
        parcel.readStringNonNull(),
        parcel.readStringNonNull(),
        parcel.readStringNonNull())

    fun isValid(): Boolean {
        return latitude.isNotEmpty() && longitude.isNotEmpty()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(latitude)
        parcel.writeString(longitude)
        parcel.writeString(altitude)
        parcel.writeString(accuracy)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ViewLocation> {
        override fun createFromParcel(parcel: Parcel): ViewLocation {
            return ViewLocation(parcel)
        }

        override fun newArray(size: Int): Array<ViewLocation?> {
            return arrayOfNulls(size)
        }
    }
}

data class ViewOption(
    val name: String,
    val code: String = "",
    val isOther: Boolean = false,
    val selected: Boolean,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readStringNonNull(),
        parcel.readStringNonNull(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(code)
        parcel.writeByte(if (isOther) 1 else 0)
        parcel.writeByte(if (selected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ViewOption> {
        override fun createFromParcel(parcel: Parcel): ViewOption {
            return ViewOption(parcel)
        }

        override fun newArray(size: Int): Array<ViewOption?> {
            return arrayOfNulls(size)
        }
    }
}

data class ViewCascadeLevel(val level: String, val answer: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readStringNonNull(),
        parcel.readStringNonNull())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(level)
        parcel.writeString(answer)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ViewCascadeLevel> {
        override fun createFromParcel(parcel: Parcel): ViewCascadeLevel {
            return ViewCascadeLevel(parcel)
        }

        override fun newArray(size: Int): Array<ViewCascadeLevel?> {
            return arrayOfNulls(size)
        }
    }
}

val <T> T.exhaustive: T
    get() = this
