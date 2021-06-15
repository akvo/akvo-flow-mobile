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

package org.akvo.flow.domain.entity

import android.os.Parcel
import android.os.Parcelable

data class DomainFormInstance(
    val formId: String,
    val dataPointId: String,
    val formVersion: String,
    val userId: String,
    val userName: String,
    val status: Int,
    val uuid: String,
    val startDate: Long,
    val savedDate: Long
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readStringNonNull(),
        parcel.readStringNonNull(),
        parcel.readStringNonNull(),
        parcel.readStringNonNull(),
        parcel.readStringNonNull(),
        parcel.readInt(),
        parcel.readStringNonNull(),
        parcel.readLong(),
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(formId)
        parcel.writeString(dataPointId)
        parcel.writeString(formVersion)
        parcel.writeString(userId)
        parcel.writeString(userName)
        parcel.writeInt(status)
        parcel.writeString(uuid)
        parcel.writeLong(startDate)
        parcel.writeLong(savedDate)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DomainFormInstance> {
        override fun createFromParcel(parcel: Parcel): DomainFormInstance {
            return DomainFormInstance(parcel)
        }

        override fun newArray(size: Int): Array<DomainFormInstance?> {
            return arrayOfNulls(size)
        }
    }
}
