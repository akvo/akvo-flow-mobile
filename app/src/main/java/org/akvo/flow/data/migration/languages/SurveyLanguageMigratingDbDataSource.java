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

package org.akvo.flow.data.migration.languages;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.akvo.flow.data.database.LanguageTable;

import java.util.Set;

/**
 * Migrating languages from table PREFERENCE to {@link LanguageTable.TABLE_NAME}
 */
public class SurveyLanguageMigratingDbDataSource {

    public void insertLanguagePreferences(SQLiteDatabase database, long surveyGroupId,
            @NonNull Set<String> languageCodes) {
        ContentValues contentValues = new ContentValues(2);
        database.delete(LanguageTable.TABLE_NAME, LanguageTable.COLUMN_SURVEY_ID + " = ?",
                new String[] { surveyGroupId + "" });
        for (String languageCode : languageCodes) {
            contentValues.put(LanguageTable.COLUMN_SURVEY_ID, surveyGroupId);
            contentValues.put(LanguageTable.COLUMN_LANGUAGE_CODE, languageCode);
            database.insert(LanguageTable.TABLE_NAME, null, contentValues);
        }
    }
}
