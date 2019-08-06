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

package org.akvo.flow.domain.entity;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

public class FormInstanceMetadata {

    private final String zipFileName;
    private final String formId;
    private final Set<String> mediaFileNames = new HashSet<>();
    private final String formInstanceData;

    public FormInstanceMetadata(String zipFileName, String formId, String formInstanceData,
            @NonNull Set<String> mediaFileNames) {
        this.zipFileName = zipFileName;
        this.formId = formId;
        this.formInstanceData = formInstanceData;
        this.mediaFileNames.addAll(mediaFileNames);
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public String getFormId() {
        return formId;
    }

    public Set<String> getMediaFileNames() {
        return mediaFileNames;
    }

    public String getFormInstanceData() {
        return formInstanceData;
    }

    public boolean isValid() {
        return !TextUtils.isEmpty(zipFileName) && !TextUtils.isEmpty(formInstanceData) && !TextUtils
                .isEmpty(formId);
    }
}
