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

package org.akvo.flow.data.loader.models;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.domain.SurveyedLocale;

public class SurveyedLocaleMapper {

    @NonNull
    public SurveyedLocale getSurveyedLocale(@NonNull Cursor cursor) {
        String id = cursor.getString(SurveyDbAdapter.RecordQuery.RECORD_ID);
        long surveyGroupId = cursor.getLong(SurveyDbAdapter.RecordQuery.SURVEY_GROUP_ID);
        long lastModified = cursor.getLong(SurveyDbAdapter.RecordQuery.LAST_MODIFIED);
        String name = cursor.getString(SurveyDbAdapter.RecordQuery.NAME);

        // Location. Check for null values first
        Double latitude = null;
        Double longitude = null;
        if (!cursor.isNull(SurveyDbAdapter.RecordQuery.LATITUDE) && !cursor.isNull(
                SurveyDbAdapter.RecordQuery.LONGITUDE)) {
            latitude = cursor.getDouble(SurveyDbAdapter.RecordQuery.LATITUDE);
            longitude = cursor.getDouble(SurveyDbAdapter.RecordQuery.LONGITUDE);
        }
        int columnIndex = cursor.getColumnIndex(SurveyInstanceColumns.STATUS);
        int status = 0;
        if (columnIndex != -1) {
            status = cursor.getInt(columnIndex);
        }
        return new SurveyedLocale(id, name, lastModified, surveyGroupId, latitude, longitude,
                status);
    }
}
