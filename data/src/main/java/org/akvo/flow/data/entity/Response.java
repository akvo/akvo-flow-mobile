/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

public class Response {
    private final String questionId;
    private final String answerType;
    private final String value;

    // Added for repeatable question groups
    private final Integer iteration;

    public Response(String questionId, String answerType, String value, Integer iteration) {
        this.questionId = questionId;
        this.answerType = answerType;
        this.value = value;
        this.iteration = iteration;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getValue() {
        return value;
    }

    public String getAnswerType() {
        return answerType;
    }

    public Integer getIteration() {
        return iteration;
    }
}
