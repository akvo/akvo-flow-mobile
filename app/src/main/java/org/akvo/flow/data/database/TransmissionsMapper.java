/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.akvo.flow.database.TransmissionColumns;
import org.akvo.flow.domain.FileTransmission;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TransmissionsMapper {

    @NonNull
    List<FileTransmission> getFileTransmissions(Cursor cursor) {
        List<FileTransmission> transmissions = new ArrayList<>();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                final int startCol = cursor.getColumnIndexOrThrow(TransmissionColumns.START_DATE);
                final int endCol = cursor.getColumnIndexOrThrow(TransmissionColumns.END_DATE);
                final int idCol = cursor.getColumnIndexOrThrow(TransmissionColumns._ID);
                final int formIdCol = cursor.getColumnIndexOrThrow(TransmissionColumns.SURVEY_ID);
                final int surveyInstanceCol = cursor
                        .getColumnIndexOrThrow(TransmissionColumns.SURVEY_INSTANCE_ID);
                final int fileCol = cursor.getColumnIndexOrThrow(TransmissionColumns.FILENAME);
                final int statusCol = cursor.getColumnIndexOrThrow(TransmissionColumns.STATUS);

                transmissions = new ArrayList<>();
                do {
                    FileTransmission trans = new FileTransmission();
                    trans.setId(cursor.getLong(idCol));
                    trans.setFormId(cursor.getString(formIdCol));
                    trans.setRespondentId(cursor.getLong(surveyInstanceCol));
                    trans.setFileName(cursor.getString(fileCol));
                    trans.setStatus(cursor.getInt(statusCol));

                    // Start and End date. Handle null cases
                    trans.setStartDate(getDate(cursor, startCol));
                    trans.setEndDate(getDate(cursor, endCol));

                    transmissions.add(trans);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return transmissions;
    }

    @Nullable
    private Date getDate(Cursor cursor, int column) {
        if (!cursor.isNull(column)) {
            return new Date(cursor.getLong(column));
        }
        return null;
    }
}
