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

package org.akvo.flow.activity

import android.view.View
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class CustomMatchers {

    companion object {

        @JvmStatic
        fun hasTextInputLayoutHintText(expectedErrorText: String): Matcher<View> =
            object : TypeSafeMatcher<View>() {

                override fun describeTo(description: Description?) {}

                override fun matchesSafely(item: View?): Boolean {
                    if (item !is TextInputLayout) return false
                    val error = item.hint ?: return false
                    val hint = error.toString()
                    return expectedErrorText == hint
                }
            }
    }
}
