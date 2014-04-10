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
import org.akvo.flow.dao.SurveyDbAdapter.Tables;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyColumns;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceColumns;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader to query the database and return the list of surveys. This Loader will
 * include the latest submission date for each survey, if exists.
 */
public class SurveyInfoLoader extends DataLoader<Cursor> {
    private long mSurveyGroupId;
    private String mRecordId;

    public SurveyInfoLoader(Context context, SurveyDbAdapter db, long surveyGroupId,
                            String recordId) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
        mRecordId = recordId;
    }

    @Override
    protected Cursor loadData(SurveyDbAdapter database) {
        String where = SurveyColumns.DELETED + " <> 1 AND " + SurveyColumns.SURVEY_GROUP_ID + " = ?";
        List<String> argList =  new ArrayList<String>();
        argList.add(String.valueOf(mSurveyGroupId));
        if (mRecordId != null) {
            where += " OR " + SurveyInstanceColumns.RECORD_ID + " = ?";
            argList.add(mRecordId);
        }

        return database.query(Tables.SURVEY_JOIN_SURVEY_INSTANCE,
                SurveyQuery.PROJECTION,
                where, argList.toArray(new String[argList.size()]),
                Tables.SURVEY + "." + SurveyColumns.SURVEY_ID,
                null,
                SurveyInstanceColumns.SUBMITTED_DATE + " DESC");
    }

    public interface SurveyQuery {
        String[] PROJECTION = {
                Tables.SURVEY + "." + SurveyColumns.SURVEY_ID,
                Tables.SURVEY + "." + SurveyColumns.NAME,
                Tables.SURVEY + "." + SurveyColumns.VERSION,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SUBMITTED_DATE
        };

        int SURVEY_ID = 0;
        int NAME      = 1;
        int VERSION   = 2;
        int SUBMITTED = 3;
    }
}
