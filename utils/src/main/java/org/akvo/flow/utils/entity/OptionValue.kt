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
package org.akvo.flow.utils.entity

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.ArrayList

object OptionValue {

    @JvmStatic
    fun serialize(values: List<Option>): String {
        try {
            val jOptions = JSONArray()
            for (option in values) {
                val jOption = JSONObject()
                jOption.put(Attrs.TEXT, option.text)
                if (!option.code.isNullOrEmpty()) {
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

    @JvmStatic
    fun deserialize(data: String): List<Option> {
        try {
            val options: MutableList<Option> = ArrayList()
            val jOptions = JSONArray(data)
            for (i in 0 until jOptions.length()) {
                val jOption = jOptions.getJSONObject(i)
                var code: String? = jOption.optString(Attrs.CODE)
                if (code.isNullOrEmpty()) {
                    code = null
                }
                val option = Option(jOption.optString(Attrs.TEXT), code, jOption.optBoolean(Attrs.IS_OTHER))
                options.add(option)
            }
            return options
        } catch (e: JSONException) {
            Timber.e(e)
        }

        // Default to old format
        val options: MutableList<Option> = ArrayList()
        val tokens = data.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (token in tokens) {
            val o = Option(code = token)
            options.add(o)
        }
        return options
    }

    @JvmStatic
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