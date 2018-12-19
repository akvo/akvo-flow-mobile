/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class FilesResultMapper {

    @Inject
    public FilesResultMapper() {
    }

    @NonNull
    public FilteredFilesResult transform(@Nullable ApiFilesResult apiFilesResult) {
        if (apiFilesResult == null) {
            return new FilteredFilesResult(Collections.<String>emptyList(),
                    Collections.<String>emptyList());
        }
        List<String> missingFilesRaw = new ArrayList<>();
        if (apiFilesResult.getMissingFiles() != null ) {
            missingFilesRaw.addAll(apiFilesResult.getMissingFiles());
        }
        if (apiFilesResult.getMissingUnknown() != null) {
            missingFilesRaw.addAll(apiFilesResult.getMissingUnknown());
        }
        List<String> missingFilenames = new ArrayList<>();
        for (String f: missingFilesRaw) {
            String filename = getFilenameFromPath(f);
            if (!TextUtils.isEmpty(filename)) {
                 missingFilenames.add(filename);
            }
        }
        List<String> deletedForms = apiFilesResult.getDeletedForms();
        if (deletedForms == null) {
            deletedForms = new ArrayList<>();
        }
        return new FilteredFilesResult(missingFilenames, deletedForms);
    }

    @VisibleForTesting
    @Nullable
    String getFilenameFromPath(@Nullable String filePath) {
        String filename;
        if (!TextUtils.isEmpty(filePath) && filePath.contains(File.separator)
                && filePath.contains(".")) {
            filename = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
        } else {
            filename = filePath;
        }
        return filename;
    }
}
