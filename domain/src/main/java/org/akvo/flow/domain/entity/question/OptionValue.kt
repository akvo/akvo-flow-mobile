/*
 * Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.domain.entity.question

import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.ArrayList

object OptionValue {
    fun serialize(values: List<DomainOption>): String {
        try {
            val jOptions = JSONArray()
            for (option in values) {
                val jOption = JSONObject()
                jOption.put(Attrs.TEXT, option.text)
                if (!TextUtils.isEmpty(option.code)) {
                    jOption.put(Attrs.CODE, option.code)
                }
                if (option.isOther) {
                    jOption.put(Attrs.IS_OTHER, true)
                }
                jOptions.put(jOption)
            }
            return jOptions.toString()
        } catch (e: JSONException) {
            Timber.e(e)
        }
        return ""
    }

    fun deserialize(data: String): List<DomainOption> {
        try {
            val options: MutableList<DomainOption> = ArrayList()
            val jOptions = JSONArray(data)
            for (i in 0 until jOptions.length()) {
                val jOption = jOptions.getJSONObject(i)
                val option = DomainOption(jOption.optString(Attrs.TEXT), jOption.optString(Attrs.CODE, ""), jOption.optBoolean(Attrs.IS_OTHER))
                options.add(option)
            }
            return options
        } catch (e: JSONException) {
            Timber.e(e)
        }

        // Default to old format
        val options: MutableList<DomainOption> = ArrayList()
        val tokens = data.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (token in tokens) {
            val o = DomainOption(code = token)
            options.add(o)
        }
        return options
    }

    fun getDatapointName(value: String): String {
        val builder = StringBuilder()
        var first = true
        for (o in deserialize(value)) {
            if (!first) {
                builder.append(" - ")
            }
            builder.append(o.text)
            first = false
        }
        return builder.toString()
    }

    internal interface Attrs {
        companion object {
            const val CODE = "code"
            const val TEXT = "text"
            const val IS_OTHER = "isOther"
        }
    }
}
