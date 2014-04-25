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

package org.akvo.flow.async.loader;

import android.content.Context;
import android.database.Cursor;

import org.akvo.flow.async.loader.base.DataLoader;
import org.akvo.flow.dao.SurveyDbAdapter;

public class SurveyInstanceLoader extends DataLoader<Cursor> {
    private long mSurveyGroupId;
    private String mSurveyedLocaleId;
    private String mSurveyId;
    private int mStatus;

    public SurveyInstanceLoader(Context context, SurveyDbAdapter db, long surveyGroupId, 
            String surveyedLocaleId, String surveyId, int status) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
        mSurveyedLocaleId = surveyedLocaleId;
        mSurveyId = surveyId;
        mStatus = status;
    }

    @Override
    public Cursor loadData(SurveyDbAdapter database) {
        if (mSurveyId != null) {
            // Specific Survey with a particular Status
            return database.getSurveyInstances(mSurveyedLocaleId, mSurveyId, mStatus);
        } else if (mSurveyedLocaleId != null) {
            // Monitored
            return database.getSurveyInstances(mSurveyedLocaleId);
        }
        // Non monitored
        return database.getSurveyInstances(mSurveyGroupId);
    }

}
