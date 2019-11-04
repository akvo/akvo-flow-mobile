/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util.files;

import android.content.Context;
import androidx.annotation.NonNull;

import java.io.File;

import javax.inject.Inject;

public class FormResourcesFileBrowser extends FileBrowser {

    // Form resources (i.e. cascading DB)
    private static final String DIR_RES = "res";

    private final FileBrowser fileBrowser;

    @Inject
    public FormResourcesFileBrowser(FileBrowser fileBrowser) {
        this.fileBrowser = fileBrowser;
    }

    @NonNull
    public File getExistingAppInternalFolder(Context context) {
        return fileBrowser.getExistingAppInternalFolder(context, DIR_RES);
    }

    @NonNull
    public File findFile(Context context, String fileName) {
        return fileBrowser.findFile(context, DIR_RES, fileName);
    }
}
