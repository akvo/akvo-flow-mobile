/*
 * Copyright (C) 2018-2020 Stichting Akvo (Akvo Foundation)
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
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import org.akvo.flow.data.util.FileHelper;
import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.UserColumns;
import org.akvo.flow.domain.entity.InstanceIdUuid;
import org.akvo.flow.domain.entity.Response;
import org.akvo.flow.domain.util.TextValueCleaner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class FormInstanceMapper {

    private final TextValueCleaner textValueCleaner;
    private final ResponseMapper responseMapper;
    private final FileHelper fileHelper;

    @Inject
    public FormInstanceMapper(TextValueCleaner textValueCleaner,
            ResponseMapper responseMapper, FileHelper fileHelper) {
        this.textValueCleaner = textValueCleaner;
        this.responseMapper = responseMapper;
        this.fileHelper = fileHelper;
    }

    @NonNull
    public List<InstanceIdUuid> getInstanceIdUuids(Cursor cursor) {
        int size = cursor == null ? 0 : cursor.getCount();
        List<InstanceIdUuid> instanceIdUuids = new ArrayList<>(size);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                instanceIdUuids.add(getInstanceIdUuid(cursor));
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
        }
        return instanceIdUuids;
    }

    @NonNull
    public List<Long> getInstanceIds(Cursor cursor) {
        int count = cursor == null? 0: cursor.getCount();
        List<Long> surveyInstanceIds = new ArrayList<>(count);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    surveyInstanceIds.add(getInstanceId(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return surveyInstanceIds;
    }

    /**
     * Iterate over the survey data returned from the database and populate the
     * ZipFileData information, setting the UUID, Survey ID, image paths, and String data.
     */
    @NonNull
    public Pair<FormInstance, Set<String>> getFormInstanceWithMedia(@NonNull String deviceId,
            Cursor data) {
        FormInstance formInstance = null;
        List<Response> responses = new ArrayList<>();
        Set<String> imagePaths = new HashSet<>();

        if (data != null && data.moveToFirst()) {
            int answer_col = data.getColumnIndexOrThrow(ResponseColumns.ANSWER);
            formInstance = getFormInstance(deviceId, data);

            do {
                String value = textValueCleaner.sanitizeValue(data.getString(answer_col));

                // empty answers will be ignored
                if (!TextUtils.isEmpty(value)) {
                    // If the response has any file attached, enqueue it to the image list
                    String filename = getFilename(data);

                    if (!TextUtils.isEmpty(filename)) {
                        imagePaths.add(filename);
                    }

                    Response response = responseMapper.extractResponse(data, value);
                    responses.add(response);
                }
            } while (data.moveToNext());

            formInstance.getResponses().addAll(responses);
        }

        if (data != null) {
            data.close();
        }

        return new Pair<>(formInstance, imagePaths);
    }


    private InstanceIdUuid getInstanceIdUuid(Cursor cursor) {
        return new InstanceIdUuid(getInstanceId(cursor), getInstanceUuid(cursor));
    }

    private String getInstanceUuid(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow(SurveyInstanceColumns.UUID));
    }

    private long getInstanceId(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(SurveyInstanceColumns._ID));
    }

    private String getFilename(Cursor data) {
        int filenameCol = data.getColumnIndexOrThrow(ResponseColumns.FILENAME);
        String filePath = data.getString(filenameCol);
        return fileHelper.getFilenameFromPath(filePath);
    }

    @NonNull
    public FormInstance getFormInstance(@NonNull String deviceId, Cursor data) {
        int surveyIdColumn = data.getColumnIndexOrThrow(SurveyInstanceColumns.SURVEY_ID);
        int emailColumn = data.getColumnIndexOrThrow(UserColumns.EMAIL);
        int submittedDateColumn = data.getColumnIndexOrThrow(SurveyInstanceColumns.SUBMITTED_DATE);
        int durationColumn = data.getColumnIndexOrThrow(SurveyInstanceColumns.DURATION);
        int localeIdColumn = data.getColumnIndexOrThrow(SurveyInstanceColumns.RECORD_ID);
        int displayNameColumn = data.getColumnIndexOrThrow(UserColumns.NAME);
        int versionColumn = data.getColumnIndexOrThrow(SurveyInstanceColumns.VERSION);

        String uuid = getInstanceUuid(data);
        String formId = data.getString(surveyIdColumn);
        String dataPointId = data.getString(localeIdColumn);
        String username = textValueCleaner.cleanVal(data.getString(displayNameColumn));
        String email = textValueCleaner.cleanVal(data.getString(emailColumn));
        final long submittedDate = data.getLong(submittedDateColumn);
        final long duration = (data.getLong(durationColumn)) / 1000;

        double formVersion = data.getDouble(versionColumn);
        return new FormInstance(uuid, dataPointId, deviceId, username, email,
                formId, submittedDate, duration, formVersion);
    }

    public Long getFormInstanceId(Cursor cursor) {
        long formInstanceId = -1L;
        if (cursor!= null && cursor.moveToFirst()) {
            formInstanceId = getInstanceId(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
        return formInstanceId;
    }
}
