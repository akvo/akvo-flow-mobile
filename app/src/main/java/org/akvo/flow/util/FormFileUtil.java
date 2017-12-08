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

package org.akvo.flow.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

import javax.inject.Inject;

public class FormFileUtil {

    private static final String DIR_FORMS = "forms";

    @Inject
    public FormFileUtil() {
    }

    public File getFormStoragePath(Context context) {
        String path = FileUtil.getInternalFolderPath(context, DIR_FORMS);
        return FileUtil.createDir(path);
    }

    public File findFormFile(Context context, String formFileName) {
        File file = new File(getFormStoragePath(context), formFileName);
        if (file.exists()) {
            return file;
        }
        File folder = getFormAppExternalStoragePath(context);
        if (folder != null && folder.exists()) {
            file = new File(folder, formFileName);
            if (file.exists()) {
                return file;
            }
        }

        return new File(getPublicFormFolderPath(), formFileName);
    }

    public File[] findAllPossibleSurveyFolders(Context context) {
        File[] folders = new File[3];
        folders[0] = getFormStoragePath(context);
        folders[1] = getFormAppExternalStoragePath(context);
        folders[2] = new File(FileUtil.getPublicFolderPath(DIR_FORMS));
        return folders;
    }

    @NonNull
    public File getPublicFormFolderPath() {
        String publicFolderPath = FileUtil.getPublicFolderPath(DIR_FORMS);
        return FileUtil.createDir(publicFolderPath);
    }

    @Nullable
    private File getFormAppExternalStoragePath(Context context) {
        String path = FileUtil.getAppExternalFolderPath(context, DIR_FORMS);
        if (path != null) {
            return FileUtil.createDir(path);
        }
        return null;
    }
}
