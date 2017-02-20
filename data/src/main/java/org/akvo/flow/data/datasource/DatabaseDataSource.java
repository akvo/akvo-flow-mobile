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

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.akvo.flow.database.Constants;
import org.akvo.flow.database.SurveyDbAdapter;

import javax.inject.Inject;

import rx.Observable;

public class DatabaseDataSource {

    private final Context context;

    @Inject
    public DatabaseDataSource(Context context) {
        this.context = context;
    }

    public Observable<Cursor> getDataPoints(@NonNull Long surveyGroupId, @Nullable Double latitude,
            @Nullable Double longitude, @Nullable Integer orderBy) {
        Cursor cursor = null;
        SurveyDbAdapter database = new SurveyDbAdapter(context);
        database.open();
        if (orderBy == null) {
            orderBy = Constants.ORDER_BY_NONE;
        }
        switch (orderBy) {
            case Constants.ORDER_BY_DISTANCE:
            case Constants.ORDER_BY_DATE:
            case Constants.ORDER_BY_STATUS:
            case Constants.ORDER_BY_NAME:
                cursor = database
                        .getFilteredSurveyedLocales(surveyGroupId, latitude, longitude,
                                orderBy);
                break;
            case Constants.ORDER_BY_NONE:
                cursor = database.getSurveyedLocales(surveyGroupId);
                break;
            default:
                break;
        }
        database.close();
        return Observable.just(cursor);
    }
}
