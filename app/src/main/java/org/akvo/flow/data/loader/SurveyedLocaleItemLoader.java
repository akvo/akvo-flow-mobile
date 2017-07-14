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
package org.akvo.flow.data.loader;

import android.content.Context;
import android.database.Cursor;

import org.akvo.flow.data.loader.base.AsyncLoader;
import org.akvo.flow.data.loader.models.SurveyedLocaleMapper;
import org.akvo.flow.data.migration.FlowMigrationListener;
import org.akvo.flow.data.migration.languages.MigrationLanguageMapper;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyedLocale;

public class SurveyedLocaleItemLoader extends AsyncLoader<SurveyedLocale> {

    private final SurveyedLocaleMapper surveyedLocaleMapper;
    private final String datapointId;

    public SurveyedLocaleItemLoader(Context context, String datapointId) {
        super(context);
        this.surveyedLocaleMapper = new SurveyedLocaleMapper();
        this.datapointId = datapointId;
    }

    @Override
    public SurveyedLocale loadInBackground() {
        Context context = getContext();
        SurveyDbAdapter db = new SurveyDbAdapter(context, new FlowMigrationListener(
                new Prefs(context), new MigrationLanguageMapper(context)));
        db.open();
        SurveyedLocale datapoint = null;
        Cursor cursor = db.getSurveyedLocale(datapointId);
        db.close();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                datapoint = surveyedLocaleMapper.getSurveyedLocale(cursor);
            }
            cursor.close();
        }
        return datapoint;
    }
}
