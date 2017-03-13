/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.database.migration;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Pair;

import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.database.Tables;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ResponseMigrationHelper {
    public ResponseMigrationHelper() {
    }

    public void migrateResponses(Map<Pair<String, String>, ContentValues> responseMigrationData,
            SQLiteDatabase db) {
        Set<Pair<String, String>> surveyInstanceIdQuestionIdSPair = responseMigrationData.keySet();
        for (Pair<String, String> key : surveyInstanceIdQuestionIdSPair) {
            db.update(Tables.RESPONSE, responseMigrationData.get(key),
                    ResponseColumns.SURVEY_INSTANCE_ID + " = ? AND " + ResponseColumns.QUESTION_ID,
                    new String[] { key.first, key.second });
        }
    }

    /**
     * Reads all the responses which have their iterations added as a pipe to questionId
     *
     * @param db
     * @return A map containing as key each entries surveyInstanceId and questionId and as value,
     * the ContentValues object with the correct questionId and iteration values
     */
    public Map<Pair<String, String>, ContentValues> obtainResponseMigrationData(
            SQLiteDatabase db) {
        Map<Pair<String, String>, ContentValues> insertionMap = new HashMap();
        Cursor cursor = db.query(Tables.RESPONSE,
                new String[] { ResponseColumns._ID, ResponseColumns.QUESTION_ID,
                        ResponseColumns.SURVEY_INSTANCE_ID
                }, null, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String questionIdContent = cursor
                            .getString(cursor.getColumnIndexOrThrow(ResponseColumns.QUESTION_ID));
                    String surveyInstanceId = cursor
                            .getString(cursor.getColumnIndexOrThrow(
                                    ResponseColumns.SURVEY_INSTANCE_ID));
                    if (questionIdContent != null && questionIdContent.contains("|") && !TextUtils
                            .isEmpty(surveyInstanceId)) {
                        String[] questionIdAndIteration = questionIdContent.split("\\|", -1);
                        if (questionIdAndIteration != null && questionIdAndIteration.length >= 2) {
                            ContentValues contentValues = new ContentValues(2);
                            contentValues
                                    .put(ResponseColumns.QUESTION_ID, questionIdAndIteration[0]);
                            contentValues.put(ResponseColumns.ITERATION, questionIdAndIteration[1]);
                            insertionMap.put(new Pair<String, String>(surveyInstanceId,
                                            questionIdContent),
                                    contentValues);
                        }
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return insertionMap;
    }
}