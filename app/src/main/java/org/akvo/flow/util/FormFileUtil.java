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

public class FormFileUtil extends InternalFileUtil{

    private static final String DIR_FORMS = "forms";

    @Inject
    public FormFileUtil() {
    }

    @Override
    @NonNull
    protected String getInternalFolderPath(Context context) {
        return FileUtil.getInternalFolderPath(context, DIR_FORMS);
    }

    @Override
    @Nullable
    protected File getAppExternalFormsDir(Context context) {
        String path = FileUtil.getAppExternalFolderPath(context, DIR_FORMS);
        File folder = null;
        if (path != null) {
            folder = new File(path);
        }
        return folder;
    }

    @Override
    @NonNull
    protected String getPublicFolderPath() {
        return FileUtil.getPublicFolderPath(DIR_FORMS);
    }
}
