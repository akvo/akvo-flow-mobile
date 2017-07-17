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

package org.akvo.flow.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.akvo.flow.database.Constants.ENGLISH_CODE;

/**
 * Retrieves / Saves user language preferences for each survey
 */
public class SurveyLanguagesDbDataSource implements SurveyLanguagesDataSource {

    private final DatabaseHelper databaseHelper;

    public SurveyLanguagesDbDataSource(Context context, MigrationListener migrationListener) {
        this.databaseHelper = new DatabaseHelper(context, new LanguageTable(), migrationListener);
    }

    @Override
    public void saveLanguagePreferences(long surveyGroupId, @NonNull Set<String> languageCodes) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues(2);
        database.delete(LanguageTable.TABLE_NAME, LanguageTable.COLUMN_SURVEY_ID + " = ?",
                new String[] { surveyGroupId + ""});
        for (String languageCode : languageCodes) {
            contentValues.put(LanguageTable.COLUMN_SURVEY_ID, surveyGroupId);
            contentValues.put(LanguageTable.COLUMN_LANGUAGE_CODE, languageCode);
            database.insert(LanguageTable.TABLE_NAME, null, contentValues);
        }
        databaseHelper.close();
    }

    @NonNull
    @Override
    public Set<String> getLanguagePreferences(long surveyGroupId) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Set<String> languages = new LinkedHashSet<>();
        Cursor cursor = database.query(LanguageTable.TABLE_NAME,
                new String[] { LanguageTable.COLUMN_LANGUAGE_CODE },
                LanguageTable.COLUMN_SURVEY_ID + " = ?",
                new String[] { surveyGroupId + "" },
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int languageCodeColumnIndex = cursor
                    .getColumnIndexOrThrow(LanguageTable.COLUMN_LANGUAGE_CODE);
            do {
                languages.add(cursor.getString(languageCodeColumnIndex));
            } while (cursor.moveToNext());
        } else {
            //if nothing there, we add english
            languages.add(ENGLISH_CODE);
        }
        if (cursor != null) {
            cursor.close();
        }
        databaseHelper.close();
        return languages;
    }
}
