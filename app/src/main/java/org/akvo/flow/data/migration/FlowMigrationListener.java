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

package org.akvo.flow.data.migration;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.akvo.flow.data.migration.languages.LanguagesExtractor;
import org.akvo.flow.data.migration.languages.MigrationLanguageMapper;
import org.akvo.flow.data.migration.languages.SurveyLanguageMigratingDbDataSource;
import org.akvo.flow.data.migration.preferences.InsertablePreferences;
import org.akvo.flow.data.migration.preferences.MigratablePreferences;
import org.akvo.flow.data.migration.preferences.PreferenceExtractor;
import org.akvo.flow.data.migration.preferences.PreferenceMapper;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.Constants;
import org.akvo.flow.database.MigrationListener;

import java.util.Set;

public class FlowMigrationListener implements MigrationListener {

    private final Prefs prefs;
    private MigrationLanguageMapper languagesMapper;

    public FlowMigrationListener(Prefs prefs, MigrationLanguageMapper migrationLanguageMapper) {
        this.prefs = prefs;
        this.languagesMapper = migrationLanguageMapper;
    }

    @Override
    public void migrateLanguages(SQLiteDatabase db) {
        long selectedSurveyId = prefs
                .getLong(Prefs.KEY_SURVEY_GROUP_ID, Constants.SURVEY_GROUP_ID_NONE);
        if (selectedSurveyId != Constants.SURVEY_GROUP_ID_NONE) {
            String dataBaseLanguages = new LanguagesExtractor().retrieveLanguages(db);
            if (!TextUtils.isEmpty(dataBaseLanguages)) {

                Set<String> insertableLanguages = languagesMapper
                        .transform(dataBaseLanguages);
                new SurveyLanguageMigratingDbDataSource()
                        .insertLanguagePreferences(db, selectedSurveyId, insertableLanguages);
            }
        }
    }

    @Override
    public void migratePreferences(SQLiteDatabase db) {
        PreferenceMapper mapper = new PreferenceMapper();
        PreferenceExtractor preferenceExtractor = new PreferenceExtractor();
        MigratablePreferences migratablePreferences = preferenceExtractor
                .retrievePreferences(db);
        InsertablePreferences insertablePreferences = mapper.transform(migratablePreferences);
        prefs.insertUserPreferences(insertablePreferences);
    }
}