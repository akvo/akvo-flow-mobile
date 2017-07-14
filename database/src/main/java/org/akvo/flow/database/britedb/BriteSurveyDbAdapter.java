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

package org.akvo.flow.database.britedb;

import android.database.Cursor;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import org.akvo.flow.database.SurveyColumns;
import org.akvo.flow.database.SurveyGroupColumns;
import org.akvo.flow.database.Tables;

import rx.Observable;
import rx.functions.Func1;

public class BriteSurveyDbAdapter {

    private static final int DOES_NOT_EXIST = -1;

    private final BriteDatabase briteDatabase;

    public BriteSurveyDbAdapter(BriteDatabase briteDatabase) {
        this.briteDatabase = briteDatabase;
    }

    public Observable<Boolean> deleteSurvey(long surveyGroupId) {
        // First the group
        briteDatabase.delete(Tables.SURVEY_GROUP, SurveyGroupColumns.SURVEY_GROUP_ID + " = ? ",
                new String[] { String.valueOf(surveyGroupId) });
        // Now the surveys
        briteDatabase.delete(Tables.SURVEY, SurveyColumns.SURVEY_GROUP_ID + " = ? ",
                new String[] { String.valueOf(surveyGroupId) });
        return Observable.just(true);
    }

    public Observable<Cursor> getSurveys() {
        String sqlQuery = "SELECT * FROM " + Tables.SURVEY_GROUP;
        return briteDatabase.createQuery(Tables.SURVEY_GROUP, sqlQuery, null).concatMap(
                new Func1<SqlBrite.Query, Observable<? extends Cursor>>() {
                    @Override
                    public Observable<? extends Cursor> call(SqlBrite.Query query) {
                        return Observable.just(query.run());
                    }
                });
    }
}
