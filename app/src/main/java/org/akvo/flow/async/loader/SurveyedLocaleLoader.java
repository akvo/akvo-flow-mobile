/*
 *  Copyright (C) 2013-2015 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.async.loader;

import android.content.Context;
import android.database.Cursor;

import org.akvo.flow.async.loader.base.DataLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.util.ConstantUtil;

public class SurveyedLocaleLoader extends DataLoader<Cursor> {
    private String mAppId;
    private long mSurveyGroupId;
    private double mLatitude;
    private double mLongitude;

    private int mOrderBy;

    public SurveyedLocaleLoader(Context context, SurveyDbAdapter db, String appId,
            long surveyGroupId, double latitude, double longitude, int orderBy) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
        mAppId = appId;
        mLatitude = latitude;
        mLongitude = longitude;
        mOrderBy = orderBy;
    }
    
    public SurveyedLocaleLoader(Context context, SurveyDbAdapter db, String appId,
            long surveyGroupId, int orderBy) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
        mOrderBy = orderBy;
        mAppId = appId;
    }

    @Override
    protected Cursor loadData(SurveyDbAdapter database) {
        switch (mOrderBy) {
            case ConstantUtil.ORDER_BY_DISTANCE:
            case ConstantUtil.ORDER_BY_DATE:
            case ConstantUtil.ORDER_BY_STATUS:
            case ConstantUtil.ORDER_BY_NAME:
                // TODO: Compute filter here in the Loader, instead of the DB
                return database.getFilteredSurveyedLocales(mSurveyGroupId, mAppId, mLatitude, mLongitude, mOrderBy);
            case ConstantUtil.ORDER_BY_NONE:
                return database.getSurveyedLocales(mSurveyGroupId, mAppId);
            default:
                return null;
        }
    }

}
