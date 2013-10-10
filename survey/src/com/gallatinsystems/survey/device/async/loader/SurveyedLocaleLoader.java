/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
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

package com.gallatinsystems.survey.device.async.loader;

import android.content.Context;
import android.database.Cursor;

import com.gallatinsystems.survey.device.async.loader.base.DataLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;

public class SurveyedLocaleLoader extends DataLoader<Cursor> {
    private int mSurveyGroupId;
    private double mLatitude;
    private double mLongitude;
    private double mRadius;

    public SurveyedLocaleLoader(Context context, SurveyDbAdapter db, int surveyGroupId,
            double latitude, double longitude, double radius) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
        mLatitude = latitude;
        mLongitude = longitude;
        mRadius = radius;
    }

    @Override
    protected Cursor loadData(SurveyDbAdapter database) {
        //return database.getSurveyedLocales(mSurveyGroupId);
        return database.getFilteredSurveyedLocales(mSurveyGroupId, mLatitude, mLongitude, mRadius);
    }

}
