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

package org.akvo.flow.domain.entity

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import java.util.ArrayList

interface KParcelable : Parcelable {
    override fun describeContents() = 0
    override fun writeToParcel(parcel: Parcel, flags: Int)
}

inline fun <reified T> parcelableCreator(
    crossinline create: (Parcel) -> T) =
    object : Parcelable.Creator<T> {
        override fun createFromParcel(source: Parcel) = create(source)
        override fun newArray(size: Int) = arrayOfNulls<T>(size)
    }

// Parcel extensions
fun Parcel.safeReadBoolean() = readInt() != 0

fun Parcel.safeWriteBoolean(value: Boolean) = writeInt(if (value) 1 else 0)

fun Parcel.readStringNonNull(): String {
    val unParceled = readString()
    return unParceled ?: unParceled ?: ""
}

fun Parcel.createStringArrayNonNull(): ArrayList<String> {
    val unParceled = createStringArrayList()
    return unParceled ?: unParceled ?: arrayListOf()
}

fun <T> Parcel.createTypedArrayNonNull(creator: Parcelable.Creator<T>): MutableList<T> {
    val unParceled = createTypedArray(creator)
    return unParceled?.toMutableList() ?: mutableListOf()
}

fun Parcel.safeReadBundle(): Bundle {
    return readBundle(Bundle::class.java.classLoader) ?: Bundle()
}
