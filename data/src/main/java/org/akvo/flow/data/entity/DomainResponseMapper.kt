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

package org.akvo.flow.data.entity

import android.database.Cursor
import org.akvo.flow.database.ResponseColumns
import org.akvo.flow.domain.entity.DomainResponse
import org.akvo.flow.domain.util.TextValueCleaner
import java.util.ArrayList
import javax.inject.Inject
import kotlin.math.max

class DomainResponseMapper @Inject constructor(val textValueCleaner: TextValueCleaner) {

    fun extractResponses(data: Cursor?): List<DomainResponse> {
        val responses: MutableList<DomainResponse> = ArrayList()
        if (data != null && data.moveToFirst()) {
            do {
                responses.add(extractResponse(data))
            } while (data.moveToNext())
        }
        data?.close()
        return responses
    }

    private fun extractResponse(data: Cursor): DomainResponse {
        val answerValueColumn = data.getColumnIndexOrThrow(ResponseColumns.ANSWER)
        val answerTypeColumn = data.getColumnIndexOrThrow(ResponseColumns.TYPE)
        val questionIdColumn = data.getColumnIndexOrThrow(ResponseColumns.QUESTION_ID)
        val iterationColumn = data.getColumnIndexOrThrow(ResponseColumns.ITERATION)
        val includeColumn = data.getColumnIndexOrThrow(ResponseColumns.INCLUDE)
        val idColumn = data.getColumnIndex(ResponseColumns._ID)
        var rawQuestionId = data.getString(questionIdColumn)
        var iteration = data.getInt(iterationColumn)
        val tokens = rawQuestionId.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (tokens.size == 2) {
            // This is a compound ID from a repeatable question
            rawQuestionId = tokens[0]
            iteration = tokens[1].toInt()
        }
        iteration = max(iteration, 0)
        val type: String = data.getString(answerTypeColumn)
        val value: String = textValueCleaner.sanitizeValue(data.getString(answerValueColumn))
        val id = data.getLong(idColumn)
        val include = data.getInt(includeColumn) == 1
        return DomainResponse(id, rawQuestionId, type, value, iteration, include)
    }

}