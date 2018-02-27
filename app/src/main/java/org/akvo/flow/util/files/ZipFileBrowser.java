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

package org.akvo.flow.util.files;

import android.content.Context;
import android.support.annotation.NonNull;

import org.akvo.flow.util.ConstantUtil;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

public class ZipFileBrowser {

    private static final String DIR_DATA = "akvoflow/data/files";

    private final FileBrowser fileBrowser;
    private final Context context;

    @Inject
    public ZipFileBrowser(FileBrowser fileBrowser, Context context) {
        this.fileBrowser = fileBrowser;
        this.context = context;
    }

    public File getSurveyInstanceFile(String uuid) {
        return new File(fileBrowser.getExistingAppInternalFolder(context, DIR_DATA),
                uuid + ConstantUtil.ARCHIVE_SUFFIX);
    }

    @NonNull
    public List<File> findAllPossibleFolders() {
        return fileBrowser.findAllPossibleFolders(context, DIR_DATA);
    }
}
