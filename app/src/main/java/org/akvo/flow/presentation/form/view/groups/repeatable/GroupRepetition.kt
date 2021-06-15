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

package org.akvo.flow.presentation.form.view.groups.repeatable

import android.os.Parcel
import org.akvo.flow.domain.entity.KParcelable
import org.akvo.flow.domain.entity.parcelableCreator
import org.akvo.flow.domain.entity.readStringNonNull
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer

data class GroupRepetition(val header: String, val questionAnswers: List<ViewQuestionAnswer>) :
    KParcelable {

    constructor(parcel: Parcel) : this(
        parcel.readStringNonNull(),
        parcel.createParcelableItemList())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(header)
        parcel.writeParcelableItemList(questionAnswers, flags)
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::GroupRepetition)
    }
}

private fun Parcel.writeParcelableItemList(input: List<ViewQuestionAnswer>, flags: Int) {
    writeInt(input.size) // Save number of elements.
    input.forEach { this.writeParcelable(it, flags) }
}

private fun Parcel.createParcelableItemList(): List<ViewQuestionAnswer> {
    val size = readInt()
    val output = ArrayList<ViewQuestionAnswer>(size)
    for (i in 0 until size) {
        output.add(readParcelable(ViewQuestionAnswer::class.java.classLoader)!!)
    }
    return output
}
