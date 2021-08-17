/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.database;

import android.database.Cursor;

import org.akvo.flow.database.SurveyColumns;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.utils.entity.Form;

import javax.inject.Inject;

public class SurveyMapper {

    @Inject
    public SurveyMapper() {
    }

    public Survey getSurvey(Cursor cursor) {
        Survey survey = new Survey();
        survey.setId(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.SURVEY_ID)));
        survey.setName(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.NAME)));
        survey.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LOCATION)));
        survey.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.FILENAME)));
        survey.setType(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.TYPE)));
        survey.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LANGUAGE)));
        survey.setVersion(cursor.getDouble(cursor.getColumnIndexOrThrow(SurveyColumns.VERSION)));

        int helpDownloaded = cursor
                .getInt(cursor.getColumnIndexOrThrow(SurveyColumns.HELP_DOWNLOADED));
        survey.setHelpDownloaded(helpDownloaded == 1);
        return survey;
    }

    public Form getForm(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(SurveyColumns._ID));
        String formId = cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.SURVEY_ID));
        long surveyId = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyColumns.SURVEY_GROUP_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.NAME));
        String location = cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LOCATION));
        String filename = cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.FILENAME));
        String type = cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.TYPE));
        String language = cursor.getString(cursor.getColumnIndexOrThrow(SurveyColumns.LANGUAGE));
        double version = cursor.getDouble(cursor.getColumnIndexOrThrow(SurveyColumns.VERSION));

        int helpDownloaded = cursor
                .getInt(cursor.getColumnIndexOrThrow(SurveyColumns.HELP_DOWNLOADED));

        return new Form(id, formId, surveyId, name, version, type, location, filename, language,
                helpDownloaded == 1, false);
    }
}
