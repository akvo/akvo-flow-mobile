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

package org.akvo.flow.data.entity;

import android.database.Cursor;
import androidx.annotation.Nullable;

import org.akvo.flow.database.TransmissionColumns;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class TransmissionMapper {

    private final S3FileMapper fileMapper;

    @Inject
    public TransmissionMapper(S3FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    public List<Transmission> transform(@Nullable Cursor cursor) {
        int size = cursor == null ? 0 : cursor.getCount();
        List<Transmission> transmissions = new ArrayList<>(size);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                final int idCol = cursor.getColumnIndexOrThrow(TransmissionColumns._ID);
                final int formIdCol = cursor.getColumnIndexOrThrow(TransmissionColumns.SURVEY_ID);
                final int surveyInstanceCol = cursor
                        .getColumnIndexOrThrow(TransmissionColumns.SURVEY_INSTANCE_ID);
                final int fileCol = cursor.getColumnIndexOrThrow(TransmissionColumns.FILENAME);
                do {
                    String filename = cursor.getString(fileCol);
                    S3File s3File = fileMapper.transform(filename);
                    if (s3File != null) {
                        Transmission trans = new Transmission(cursor.getLong(idCol),
                                cursor.getLong(surveyInstanceCol), cursor.getString(formIdCol),
                                s3File);
                        transmissions.add(trans);
                    } else {
                        Timber.e("Transmission error: file " + filename + " could not be processed");
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return transmissions;
    }
}
