/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.entity;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;

import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.domain.entity.Response;

import javax.inject.Inject;

public class ResponseMapper {

    @Inject
    public ResponseMapper() {
    }

    @NonNull
    Response extractResponse(Cursor data, String value) {
        String type = getAnswerType(data);
        Pair<String, Integer> mappedIdIteration = mapIdIteration(data);
        return new Response(mappedIdIteration.first, type, value, mappedIdIteration.second);
    }

    private String getAnswerType(Cursor data) {
        int answerTypeColumn = data.getColumnIndexOrThrow(ResponseColumns.TYPE);
        return data.getString(answerTypeColumn);
    }

    @VisibleForTesting
    Pair<String, Integer> mapIdIteration(Cursor data) {
        int questionIdColumn = data.getColumnIndexOrThrow(ResponseColumns.QUESTION_ID);
        int iterationColumn = data.getColumnIndexOrThrow(ResponseColumns.ITERATION);
        String rawQuestionId = data.getString(questionIdColumn);
        int iteration = data.getInt(iterationColumn);
        String[] tokens = rawQuestionId.split("\\|", -1);
        if (tokens.length == 2) {
            // This is a compound ID from a repeatable question
            rawQuestionId = tokens[0];
            iteration = Integer.parseInt(tokens[1]);
        }
        iteration = Math.max(iteration, 0);
        return new Pair<>(rawQuestionId, iteration);
    }
}
