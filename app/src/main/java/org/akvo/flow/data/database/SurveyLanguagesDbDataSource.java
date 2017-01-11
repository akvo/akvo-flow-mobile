/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.akvo.flow.data.SurveyLanguagesDataSource;
import org.akvo.flow.util.ConstantUtil;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Retrieves / Saves user language preferences for each survey
 */
public class SurveyLanguagesDbDataSource implements SurveyLanguagesDataSource {

    private final DatabaseHelper databaseHelper;

    public SurveyLanguagesDbDataSource(Context context) {
        this.databaseHelper = new DatabaseHelper(context, new LanguageTable());
    }

    @Override
    public void saveLanguagePreferences(String surveyId, @NonNull Set<String> languageCodes) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues(2);
        database.delete(LanguageTable.TABLE_NAME, LanguageTable.COLUMN_SURVEY_ID + " = ?",
                new String[] { surveyId });
        for (String languageCode : languageCodes) {
            contentValues.put(LanguageTable.COLUMN_SURVEY_ID, surveyId);
            contentValues.put(LanguageTable.COLUMN_LANGUAGE_CODE, languageCode);
            database.insert(LanguageTable.TABLE_NAME, null, contentValues);
        }
        databaseHelper.close();
    }

    @NonNull
    @Override
    public Set<String> getLanguagePreferences(String surveyId) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Set<String> languages = new LinkedHashSet<>();
        //english is added by default at the beginning, should it?
        languages.add(ConstantUtil.ENGLISH_CODE);
        Cursor cursor = database.query(LanguageTable.TABLE_NAME,
                new String[] { LanguageTable.COLUMN_LANGUAGE_CODE },
                LanguageTable.COLUMN_SURVEY_ID + " = ?",
                new String[] { surveyId },
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
                int languageCodeColumnIndex = cursor
                        .getColumnIndexOrThrow(LanguageTable.COLUMN_LANGUAGE_CODE);
            if (languageCodeColumnIndex >= 0) {
                do {
                    languages.add(cursor.getString(languageCodeColumnIndex));
                } while (cursor.moveToNext());
            }
        }
        cursor.close();
        databaseHelper.close();
        return languages;
    }
}
