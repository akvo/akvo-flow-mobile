/*
 * Copyright (C) 2017,2020 Stichting Akvo (Akvo Foundation)
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
 *
 */
package org.akvo.flow.presentation.navigation

import android.os.Parcel
import android.os.Parcelable

data class ViewSurvey(
    val id: Long,
    val name: String?,
    val isMonitored: Boolean,
    val registrationSurveyId: String?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readString()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(name)
        dest.writeByte((if (isMonitored) 1 else 0).toByte())
        dest.writeString(registrationSurveyId)
    }

    companion object CREATOR : Parcelable.Creator<ViewSurvey> {
        override fun createFromParcel(parcel: Parcel): ViewSurvey {
            return ViewSurvey(parcel)
        }

        override fun newArray(size: Int): Array<ViewSurvey?> {
            return arrayOfNulls(size)
        }
    }
}
