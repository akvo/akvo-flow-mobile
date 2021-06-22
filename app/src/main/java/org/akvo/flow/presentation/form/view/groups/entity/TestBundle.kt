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

package org.akvo.flow.presentation.form.view.groups.entity

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

class TestBundle(private val translations: Bundle?) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readBundle(Bundle::class.java.classLoader)) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeBundle(translations)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TestBundle> {
        override fun createFromParcel(parcel: Parcel): TestBundle {
            return TestBundle(parcel)
        }

        override fun newArray(size: Int): Array<TestBundle?> {
            return arrayOfNulls(size)
        }
    }
}