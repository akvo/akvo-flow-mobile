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

package org.akvo.flow.data.entity.time

import android.text.TextUtils
import java.text.DateFormat
import javax.inject.Inject

class TimeMapper @Inject constructor(private val dateFormat: DateFormat) {

    fun formatTime(time: String): Long {
        return if (isValid(time)) {
            dateFormat.parse(time)?.time ?: -1
        } else {
            -1
        }
    }

    fun isValid(value: String?): Boolean {
        return !TextUtils.isEmpty(value) && !value.equals("null", ignoreCase = true)
    }
}