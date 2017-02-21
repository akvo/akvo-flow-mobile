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

package org.akvo.flow.data.datasource;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.squareup.sqlbrite.BriteDatabase;

import org.akvo.flow.database.Constants;
import org.akvo.flow.database.britedb.BriteDbAdapter;

import javax.inject.Inject;

import rx.Observable;

public class DatabaseDataSource {

    private final BriteDbAdapter briteDbAdapter;

    @Inject
    public DatabaseDataSource(BriteDatabase db) {
        this.briteDbAdapter = new BriteDbAdapter(db);
    }

    public Observable<Cursor> getDataPoints(@NonNull Long surveyGroupId, @Nullable Double latitude,
            @Nullable Double longitude, @Nullable Integer orderBy) {
        if (orderBy == null) {
            orderBy = Constants.ORDER_BY_NONE;
        }
        switch (orderBy) {
            case Constants.ORDER_BY_DISTANCE:
            case Constants.ORDER_BY_DATE:
            case Constants.ORDER_BY_STATUS:
            case Constants.ORDER_BY_NAME:
                return briteDbAdapter
                        .getFilteredSurveyedLocales(surveyGroupId, latitude, longitude, orderBy);
            default:
                return briteDbAdapter.getSurveyedLocales(surveyGroupId);
        }
    }
}
