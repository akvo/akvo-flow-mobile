/*
 * Copyright (C) 2010-2019 Stichting Akvo (Akvo Foundation)
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
import androidx.annotation.NonNull;

import org.akvo.flow.data.loader.base.AsyncLoader;
import org.akvo.flow.data.loader.models.FormInfo;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.domain.SurveyGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader to query the database and return the list of surveys. This Loader will
 * include the latest submission date for each survey, if exists.
 */
public class FormInfoLoader extends AsyncLoader<List<FormInfo>> {

    private final long surveyGroupId;
    private final String recordId;
    private final SurveyGroup surveyGroup;

    public FormInfoLoader(Context context, String recordId, SurveyGroup surveyGroup) {
        super(context);
        this.surveyGroup = surveyGroup;
        this.surveyGroupId = surveyGroup.getId();
        this.recordId = recordId;
    }

    @Override
    public List<FormInfo> loadInBackground() {
        Context context = getContext();
        SurveyDbAdapter database = new SurveyDbAdapter(context);
        database.open();

        boolean submittedDataPoint = isDataPointSubmitted(database);
        List<FormInfo> forms = retrieveForms(database, submittedDataPoint);

        database.close();
        return forms;
    }

    @NonNull
    private List<FormInfo> retrieveForms(SurveyDbAdapter database, boolean submittedDataPoint) {
        List<FormInfo> surveys = new ArrayList<>();
        Cursor cursor = database.getDataPointForms(surveyGroupId, recordId);
        if (cursor.moveToFirst()) {
            do {

                String id = cursor.getString(SurveyDbAdapter.SurveyQuery.SURVEY_ID);
                String name = cursor.getString(SurveyDbAdapter.SurveyQuery.NAME);
                String version = String
                        .valueOf(cursor.getFloat(SurveyDbAdapter.SurveyQuery.VERSION));
                boolean registrationSurvey = isRegistrationForm(id);

                Long lastSubmission = null;
                if (!cursor.isNull(SurveyDbAdapter.SurveyQuery.SUBMITTED)) {
                    lastSubmission = cursor.getLong(SurveyDbAdapter.SurveyQuery.SUBMITTED);
                }
                boolean deleted = cursor.getInt(SurveyDbAdapter.SurveyQuery.DELETED) == 1;

                FormInfo s = new FormInfo(id, name, version, lastSubmission, deleted,
                        registrationSurvey, submittedDataPoint);
                if (surveyGroup.isMonitored() && registrationSurvey) {
                    surveys.add(0, s);// Make sure registration survey is at the top
                } else {
                    surveys.add(s);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return surveys;
    }

    private boolean isDataPointSubmitted(SurveyDbAdapter database) {
        boolean submittedDataPoint = false;
        Cursor c = database.getDatapointStatus(recordId);
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(SurveyInstanceColumns.STATUS));
            submittedDataPoint = status == SurveyInstanceStatus.SUBMIT_REQUESTED
                    || status == SurveyInstanceStatus.SUBMITTED
                    || status == SurveyInstanceStatus.UPLOADED
                    || status == SurveyInstanceStatus.DOWNLOADED;
        }
        c.close();
        return submittedDataPoint;
    }

    private boolean isRegistrationForm(String surveyId) {
        return surveyId.equals(surveyGroup.getRegisterSurveyId());
    }
}
