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

package org.akvo.flow.data.datasource;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import org.akvo.flow.data.util.ExternalStorageHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class FolderBrowser {

    static final String CADDISFLY_OLD_FOLDER = "/result-images";
    static final String DIR_DATA = "akvoflow/data/files";
    static final String DIR_MEDIA = "akvoflow/data/media";
    static final String DIR_PUBLISHED = "published";
    static final String DIR_PUBLISHED_DATA = DIR_PUBLISHED + "/files";
    static final String DIR_PUBLISHED_MEDIA = DIR_PUBLISHED + "/media";
    static final String DIR_TMP = "tmp";
    static final String DIR_RES = "res";
    static final String DIR_FORMS = "forms";
    static final String DIR_INBOX = "akvoflow/inbox";

    private final Context context;
    private final ExternalStorageHelper externalStorageHelper;

    @Inject
    public FolderBrowser(Context context,
            ExternalStorageHelper externalStorageHelper) {
        this.context = context;
        this.externalStorageHelper = externalStorageHelper;
    }

    @NonNull
    public List<File> findAllPossibleFolders(String folderName) {
        List<File> folders = new ArrayList<>(3);
        File folder = getInternalFolder(folderName);
        if (folder.exists()) {
            folders.add(folder);
        }
        File folder2 = getAppExternalFolder(folderName);
        if (folder2 != null && folder2.exists()) {
            folders.add(folder2);
        }
        File folder3 = getPublicFolder(folderName);
        if (folder3 != null && folder3.exists()) {
            folders.add(folder3);
        }
        return folders;
    }

    @Nullable
    File getPublicFolder(String folderName) {
        String externalStoragePath = externalStorageHelper.getExternalStoragePath();
        if (externalStoragePath == null) {
            return null;
        }
        return new File(externalStoragePath + File.separator + folderName);
    }

    @NonNull
    File getInternalFolder(String folder) {
        String path = context.getFilesDir().getAbsolutePath() + File.separator + folder;
        return new File(path);
    }

    @Nullable
    File getAppExternalFolder(String folderName) {
        String path = getAppExternalFolderPath(folderName);
        File folder = null;
        if (path != null) {
            folder = new File(path);
        }
        return folder;
    }

    @Nullable
    private String getAppExternalFolderPath(String folder) {
        String appExternalStoragePath = getAppExternalStoragePath();
        return appExternalStoragePath == null ?
                null :
                appExternalStoragePath + File.separator + folder;
    }

    /**
     * Returns app specific folder on the external storage
     * External Storage may not be available
     */
    @Nullable
    private String getAppExternalStoragePath() {
        File externalFilesDir = ContextCompat.getExternalFilesDirs(context, null)[0];
        if (externalFilesDir != null) {
            return externalFilesDir.getAbsolutePath();
        }
        return null;
    }
}
