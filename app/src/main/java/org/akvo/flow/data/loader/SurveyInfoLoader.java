/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.loader;

import android.content.Context;
import android.database.Cursor;

import org.akvo.flow.data.database.SurveyColumns;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.data.database.SurveyInstanceColumns;
import org.akvo.flow.data.database.SurveyInstanceStatus;
import org.akvo.flow.data.database.Tables;
import org.akvo.flow.data.loader.base.AsyncLoader;
import org.akvo.flow.data.loader.models.SurveyInfo;
import org.akvo.flow.domain.SurveyGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader to query the database and return the list of surveys. This Loader will
 * include the latest submission date for each survey, if exists.
 */
public class SurveyInfoLoader extends AsyncLoader<List<SurveyInfo>> {

    private final long surveyGroupId;
    private final String recordId;
    private final SurveyGroup surveyGroup;

    public SurveyInfoLoader(Context context, String recordId, SurveyGroup surveyGroup) {
        super(context);
        this.surveyGroup = surveyGroup;
        this.surveyGroupId = surveyGroup.getId();
        this.recordId = recordId;
    }

    @Override
    public List<SurveyInfo> loadInBackground() {

        SurveyDbAdapter database = new SurveyDbAdapter(getContext());
        database.open();

        Cursor cursor = getDataPointForms(database, surveyGroupId, recordId);

        boolean submittedDataPoint = isDataPointSubmitted(database);
        database.close();
        List<SurveyInfo> surveys = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {

                String id = cursor.getString(SurveyQuery.SURVEY_ID);
                String name = cursor.getString(SurveyQuery.NAME);
                String version = String.valueOf(cursor.getFloat(SurveyQuery.VERSION));
                Long lastSubmission = null;
                boolean registrationSurvey = isRegistrationForm(id);

                if (!cursor.isNull(SurveyQuery.SUBMITTED)) {
                    lastSubmission = cursor.getLong(SurveyQuery.SUBMITTED);
                }
                boolean deleted = cursor.getInt(SurveyQuery.DELETED) == 1;
                SurveyInfo s = new SurveyInfo(id, name, version, lastSubmission, deleted,
                        registrationSurvey, submittedDataPoint);
                if (surveyGroup.isMonitored() && registrationSurvey) {
                    surveys.add(0, s);// Make sure registration survey is at the top
                } else {
                    surveys.add(s);
                }
            } while (cursor.moveToNext());
        }
        return surveys;
    }

        //TODO: move this code to survyedb adapter
    private Cursor getDataPointForms(SurveyDbAdapter database, long surveyGroupId, String recordId) {
        String table = SurveyDbAdapter.SURVEY_JOIN_SURVEY_INSTANCE +"";
        if (recordId != null) {
            // Add record id to the join condition. If put in the where, the left join won't work
            table +=  " AND " + Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.RECORD_ID
                    + "='" + recordId + "'";
        }
        return database.query(table,
                SurveyQuery.PROJECTION,
                SurveyColumns.SURVEY_GROUP_ID + " = ?",
                new String[] { String.valueOf(surveyGroupId) },
                Tables.SURVEY + "." + SurveyColumns.SURVEY_ID,
                null,
                SurveyColumns.NAME);
    }

    private boolean isDataPointSubmitted(SurveyDbAdapter database) {
        boolean submittedDataPoint = false;
        Cursor c = database.getDatapointStatus(recordId);
        if (c.moveToFirst()) {
            int status = c.getInt(0); //TODO fix
            submittedDataPoint = status == SurveyInstanceStatus.SUBMITTED
                    || status == SurveyInstanceStatus.EXPORTED
                    || status == SurveyInstanceStatus.SYNCED
                    || status == SurveyInstanceStatus.DOWNLOADED;
        }
        c.close();
        return submittedDataPoint;
    }

    private boolean isRegistrationForm(String surveyId) {
        return surveyId.equals(surveyGroup.getRegisterSurveyId());
    }

    interface SurveyQuery {
        String[] PROJECTION = {
                Tables.SURVEY + "." + SurveyColumns.SURVEY_ID,
                Tables.SURVEY + "." + SurveyColumns.NAME,
                Tables.SURVEY + "." + SurveyColumns.VERSION,
                Tables.SURVEY + "." + SurveyColumns.DELETED,
                Tables.SURVEY_INSTANCE + "." + SurveyInstanceColumns.SUBMITTED_DATE,
        };

        int SURVEY_ID = 0;
        int NAME = 1;
        int VERSION = 2;
        int DELETED = 3;
        int SUBMITTED = 4;
    }
}
