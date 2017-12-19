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

public class FormResourcesFileUtil extends InternalFileUtil {

    // Form resources (i.e. cascading DB)
    private static final String DIR_RES = "res";

    @Inject
    public FormResourcesFileUtil() {
    }

    @Override
    @NonNull
    protected String getInternalFolderPath(Context context) {
        return FileUtil.getInternalFolderPath(context, DIR_RES);
    }

    @Override
    @Nullable
    protected String getAppExternalFolderPath(Context context) {
        return FileUtil.getAppExternalFolderPath(context, DIR_RES);
    }

    @Override
    @NonNull
    protected String getPublicFolderPath() {
        return FileUtil.getPublicFolderPath(DIR_RES);
    }
}
