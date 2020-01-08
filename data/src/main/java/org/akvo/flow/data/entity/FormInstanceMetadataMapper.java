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
import androidx.core.util.Pair;

import org.akvo.flow.data.util.Constants;
import org.akvo.flow.domain.entity.FormInstanceMetadata;
import org.akvo.flow.domain.util.GsonMapper;

import java.util.Set;

import javax.inject.Inject;

public class FormInstanceMetadataMapper {

    private final FormInstanceMapper formInstanceMapper;
    private final GsonMapper mapper;

    @Inject
    public FormInstanceMetadataMapper(FormInstanceMapper formInstanceMapper,
            GsonMapper mapper) {
        this.formInstanceMapper = formInstanceMapper;
        this.mapper = mapper;
    }

    public FormInstanceMetadata transform(Cursor cursor, String deviceId) {
        Pair<FormInstance, Set<String>> processedInstance = formInstanceMapper
                .getFormInstanceWithMedia(deviceId, cursor);
        FormInstance formInstance = processedInstance.first;
        Set<String> imagePaths = processedInstance.second;
        String data = null;
        String uuid;
        String formId = null;
        String filename = null;
        if (formInstance != null) {
            data = mapper.write(formInstance, FormInstance.class);
            uuid = formInstance.getUUID();
            formId = formInstance.getFormId();
            filename = uuid + Constants.ARCHIVE_SUFFIX;
        }
        return new FormInstanceMetadata(filename, formId, data, imagePaths);
    }
}
