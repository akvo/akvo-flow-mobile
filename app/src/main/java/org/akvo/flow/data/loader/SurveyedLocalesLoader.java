/*
 *  Copyright (C) 2013-2017 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.akvo.flow.data.loader;

import android.content.Context;
import android.database.Cursor;

import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.data.loader.base.AsyncLoader;
import org.akvo.flow.data.loader.models.SurveyedLocaleMapper;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;

public class SurveyedLocalesLoader extends AsyncLoader<List<SurveyedLocale>> {

    public static final int INVALID_COORDINATE = -1;

    private final long mSurveyGroupId;
    private final double mLatitude;
    private final double mLongitude;
    private final int mOrderBy;
    private final SurveyedLocaleMapper surveyedLocaleMapper;

    public SurveyedLocalesLoader(Context context, long surveyGroupId, double latitude,
            double longitude, int orderBy) {
        super(context);
        this.mSurveyGroupId = surveyGroupId;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mOrderBy = orderBy;
        this.surveyedLocaleMapper = new SurveyedLocaleMapper();
    }

    public SurveyedLocalesLoader(Context context, long surveyGroupId, int orderBy) {
        super(context);
        this.mSurveyGroupId = surveyGroupId;
        this.mLatitude = INVALID_COORDINATE;
        this.mLongitude = INVALID_COORDINATE;
        this.mOrderBy = orderBy;
        this.surveyedLocaleMapper = new SurveyedLocaleMapper();
    }

    @Override
    public List<SurveyedLocale> loadInBackground() {
        List<SurveyedLocale> mItems = new ArrayList<>();
        SurveyDbAdapter database = new SurveyDbAdapter(getContext());
        database.open();
        Cursor cursor = null;

        switch (mOrderBy) {
            case ConstantUtil.ORDER_BY_DISTANCE:
            case ConstantUtil.ORDER_BY_DATE:
            case ConstantUtil.ORDER_BY_STATUS:
            case ConstantUtil.ORDER_BY_NAME:
                // TODO: Compute filter here in the Loader, instead of the DB
                cursor = database
                        .getFilteredSurveyedLocales(mSurveyGroupId, mLatitude, mLongitude,
                                mOrderBy);
                break;
            case ConstantUtil.ORDER_BY_NONE:
                cursor = database.getSurveyedLocales(mSurveyGroupId);
                break;
            default:
                break;
        }
        database.close();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    SurveyedLocale item = surveyedLocaleMapper.getSurveyedLocale(cursor);
                    mItems.add(item);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return mItems;
    }
}
