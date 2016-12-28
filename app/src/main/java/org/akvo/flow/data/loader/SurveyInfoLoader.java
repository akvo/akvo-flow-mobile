/*
 *  Copyright (C) 2013-2016 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.loader;

import android.content.Context;
import android.database.Cursor;

import org.akvo.flow.data.database.SurveyColumns;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.data.database.SurveyInstanceColumns;
import org.akvo.flow.data.database.Tables;
import org.akvo.flow.data.loader.base.AsyncLoader;

/**
 * Loader to query the database and return the list of surveys. This Loader will
 * include the latest submission date for each survey, if exists.
 */
public class SurveyInfoLoader extends AsyncLoader<Cursor> {

    private final long mSurveyGroupId;
    private final String mRecordId;

    public SurveyInfoLoader(Context context, long surveyGroupId,
            String recordId) {
        super(context);
        mSurveyGroupId = surveyGroupId;
        mRecordId = recordId;
    }

    @Override
    public Cursor loadInBackground() {
        String table = SurveyDbAdapter.SURVEY_JOIN_SURVEY_INSTANCE;
        if (mRecordId != null) {
            // Add record id to the join condition. If put in the where, the left join won't work
            table +=  " AND " + Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.RECORD_ID
                    + "='" + mRecordId + "'";
        }
        SurveyDbAdapter database = new SurveyDbAdapter(getContext());
        database.open();
        Cursor query = database.query(table,
                SurveyQuery.PROJECTION,
                SurveyColumns.SURVEY_GROUP_ID + " = ?",
                new String[] { String.valueOf(mSurveyGroupId) },
                Tables.SURVEY + "." + SurveyColumns.SURVEY_ID,
                null,
                SurveyColumns.NAME);
        database.close();
        return query;
    }

    public interface SurveyQuery {
        String[] PROJECTION = {
                Tables.SURVEY + "." + SurveyColumns.SURVEY_ID,
                Tables.SURVEY + "." + SurveyColumns.NAME,
                Tables.SURVEY + "." + SurveyColumns.VERSION,
                Tables.SURVEY + "." + SurveyColumns.DELETED,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SUBMITTED_DATE
        };

        int SURVEY_ID = 0;
        int NAME      = 1;
        int VERSION   = 2;
        int DELETED   = 3;
        int SUBMITTED = 4;
    }
}
