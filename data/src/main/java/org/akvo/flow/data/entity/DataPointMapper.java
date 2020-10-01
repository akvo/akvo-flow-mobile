/*
 * Copyright (C) 2017,2019-2020 Stichting Akvo (Akvo Foundation)
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.entity.SurveyInstanceStatus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class DataPointMapper {

    @Inject
    public DataPointMapper() {
    }

    @Nullable
    public DataPoint mapOneDataPoint(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            return getDataPoint(cursor);
        }
        return null;
    }

    private DataPoint getDataPoint(Cursor cursor) {
        String id = cursor.getString(SurveyDbAdapter.RecordQuery.RECORD_ID);
        long surveyGroupId = cursor.getLong(SurveyDbAdapter.RecordQuery.SURVEY_GROUP_ID);
        long lastModified = cursor.getLong(SurveyDbAdapter.RecordQuery.LAST_MODIFIED);
        String name = cursor.getString(SurveyDbAdapter.RecordQuery.NAME);
        boolean viewed = cursor.getInt(SurveyDbAdapter.RecordQuery.VIEWED) == 1;

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
        return new DataPoint(id, name, lastModified, surveyGroupId, latitude, longitude,
                mapStatus(status), viewed);
    }

    private SurveyInstanceStatus mapStatus(int status) {
        switch (status) {
            case org.akvo.flow.database.SurveyInstanceStatus.SAVED:
                return SurveyInstanceStatus.SAVED;
            case org.akvo.flow.database.SurveyInstanceStatus.SUBMIT_REQUESTED:
                return SurveyInstanceStatus.SUBMIT_REQUESTED;
            case org.akvo.flow.database.SurveyInstanceStatus.SUBMITTED:
                return SurveyInstanceStatus.SUBMITTED;
            case org.akvo.flow.database.SurveyInstanceStatus.UPLOADED:
                return SurveyInstanceStatus.UPLOADED;
            case org.akvo.flow.database.SurveyInstanceStatus.DOWNLOADED:
                return SurveyInstanceStatus.DOWNLOADED;
            default:
                return SurveyInstanceStatus.MISSING;
        }
    }

    @NonNull
    public List<DataPoint> getDataPoints(Cursor cursor) {
        List<DataPoint> items = new ArrayList<>();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    DataPoint item = getDataPoint(cursor);
                    items.add(item);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return items;
    }
}
