/*
 *  Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.akvo.flow.utils.entity

/**
 * data structure representing a dependency between questions. A dependency
 * consists of two values: a question ID and an answer value. When a question
 * contains a dependency, it will not be shown unless the question referenced by
 * the dependency's questionID has an answer that matches the answerValue in the
 * A question can have 0 or 1 dependencies.
 *
 */
data class Dependency(val question: String?, var answer: String?) {

    fun isMatch(value: String?): Boolean {
        if (answer == null || value == null) {
            return answer === value
        }
        val values = OptionValue.deserialize(value)
        for (o in values) {
            for (a in answer!!.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()) {
                if (o.text!!.trim { it <= ' ' } == a.trim { it <= ' ' }) {
                    return true
                }
            }
        }
        return false
    }
}
