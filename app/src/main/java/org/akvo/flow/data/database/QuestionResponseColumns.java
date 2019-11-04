/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.database;

import android.database.Cursor;
import androidx.annotation.NonNull;

import org.akvo.flow.database.ResponseColumns;

public class QuestionResponseColumns {

    private final int idColumn;
    private final int answerColumn;
    private final int typeColumn;
    private final int questionIdColumn;
    private final int includeColumn;
    private final int filenameColumn;
    private final int iterationColumn;

    public QuestionResponseColumns(@NonNull Cursor cursor) {
        this.idColumn = cursor.getColumnIndexOrThrow(ResponseColumns._ID);
        this.answerColumn = cursor.getColumnIndexOrThrow(ResponseColumns.ANSWER);
        this.typeColumn = cursor.getColumnIndexOrThrow(ResponseColumns.TYPE);
        this.questionIdColumn = cursor.getColumnIndexOrThrow(ResponseColumns.QUESTION_ID);
        this.includeColumn = cursor.getColumnIndexOrThrow(ResponseColumns.INCLUDE);
        this.filenameColumn = cursor.getColumnIndexOrThrow(ResponseColumns.FILENAME);
        this.iterationColumn = cursor.getColumnIndexOrThrow(ResponseColumns.ITERATION);
    }

    public int getIdColumn() {
        return idColumn;
    }

    public int getAnswerColumn() {
        return answerColumn;
    }

    public int getTypeColumn() {
        return typeColumn;
    }

    public int getQuestionIdColumn() {
        return questionIdColumn;
    }

    public int getIncludeColumn() {
        return includeColumn;
    }

    public int getFilenameColumn() {
        return filenameColumn;
    }

    public int getIterationColumn() {
        return iterationColumn;
    }
}
