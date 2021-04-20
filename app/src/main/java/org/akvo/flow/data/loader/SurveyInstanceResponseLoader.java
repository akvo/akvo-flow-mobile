/*
 * Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
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
import android.database.sqlite.SQLiteOpenHelper;

import org.akvo.flow.data.loader.base.AsyncLoader;
import org.akvo.flow.database.SurveyDbAdapter;

public class SurveyInstanceResponseLoader extends AsyncLoader<Cursor> {

    private final String surveyedLocaleId;
    private final SQLiteOpenHelper databaseHelper;

    public SurveyInstanceResponseLoader(Context context, String surveyedLocaleId, SQLiteOpenHelper databaseHelper) {
        super(context);
        this.surveyedLocaleId = surveyedLocaleId;
        this.databaseHelper = databaseHelper;
    }

    @Override
    public Cursor loadInBackground() {
        SurveyDbAdapter database = new SurveyDbAdapter(databaseHelper);
        database.open();
        Cursor formInstances = database.getFormInstancesWithResponses(surveyedLocaleId);
        database.close();
        return formInstances;
    }
}
