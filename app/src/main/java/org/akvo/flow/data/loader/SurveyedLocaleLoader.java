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

import org.akvo.flow.data.loader.base.DataLoader;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.util.ConstantUtil;

public class SurveyedLocaleLoader extends DataLoader<Cursor> {
    private long mSurveyGroupId;
    private double mLatitude;
    private double mLongitude;

    private int mOrderBy;

    public SurveyedLocaleLoader(Context context, SurveyDbAdapter db, long surveyGroupId,
            double latitude, double longitude, int orderBy) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
        mLatitude = latitude;
        mLongitude = longitude;
        mOrderBy = orderBy;
    }
    
    public SurveyedLocaleLoader(Context context, SurveyDbAdapter db, long surveyGroupId, int orderBy) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
        mOrderBy = orderBy;
    }

    @Override
    protected Cursor loadData(SurveyDbAdapter database) {
        switch (mOrderBy) {
            case ConstantUtil.ORDER_BY_DISTANCE:
            case ConstantUtil.ORDER_BY_DATE:
            case ConstantUtil.ORDER_BY_STATUS:
            case ConstantUtil.ORDER_BY_NAME:
                // TODO: Compute filter here in the Loader, instead of the DB
                return database.getFilteredSurveyedLocales(mSurveyGroupId, mLatitude, mLongitude, mOrderBy);
            case ConstantUtil.ORDER_BY_NONE:
                return database.getSurveyedLocales(mSurveyGroupId);
            default:
                return null;
        }
    }

}
