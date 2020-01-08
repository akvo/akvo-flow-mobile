/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.entity;

import android.database.Cursor;

import org.akvo.flow.database.SurveyGroupColumns;
import org.akvo.flow.domain.entity.Survey;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class SurveyMapper {

    @Inject
    public SurveyMapper() {
    }

    private Survey getSurvey(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyGroupColumns.SURVEY_GROUP_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(SurveyGroupColumns.NAME));
        String registerSurveyId = cursor
                .getString(cursor.getColumnIndexOrThrow(SurveyGroupColumns.REGISTER_SURVEY_ID));
        boolean monitored =
                cursor.getInt(cursor.getColumnIndexOrThrow(SurveyGroupColumns.MONITORED)) > 0;
        return new Survey(id, name, monitored, registerSurveyId);
    }

    public List<Survey> getSurveys(Cursor cursor) {
        List<Survey> surveys = new ArrayList<>();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Survey survey = getSurvey(cursor);
                    surveys.add(survey);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return surveys;
    }
}
