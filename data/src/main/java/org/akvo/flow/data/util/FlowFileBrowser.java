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

package org.akvo.flow.data.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class FlowFileBrowser {

    public static final String DIR_MEDIA = "akvoflow/data/media";
    public static final String DIR_DATA = "akvoflow/data/files";
    public static final String CADDISFLY_OLD_FOLDER = "/result-images";
    public static final String DIR_PUBLISHED = "published";
    public static final String DIR_PUBLISHED_DATA = DIR_PUBLISHED + "/files";
    public static final String DIR_PUBLISHED_MEDIA = DIR_PUBLISHED + "/media";
    public static final String DIR_TMP = "tmp";
    public static final String DIR_RES = "res";
    public static final String DIR_FORMS = "forms";
    public static final String DIR_INBOX = "akvoflow/inbox";
    public static final String XML_SUFFIX = ".xml";
    public static final String ZIP_SUFFIX = ".zip";
    private static final String VIDEO_SUFFIX = ".mp4";

    private final Context context;
    private final ExternalStorageHelper externalStorageHelper;

    @Inject
    public FlowFileBrowser(Context context,
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

    @NonNull
    public File getInternalFolder(String folder) {
        String path = context.getFilesDir().getAbsolutePath() + File.separator + folder;
        return new File(path);
    }

    public File getExistingInternalFolder(String folderName) {
        File folder = getInternalFolder(folderName);
        if (!folder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        }
        return folder;
    }

    public File getInternalFile(@NonNull String filename, @Nullable String folderName) {
        File folder = getInternalFolder(folderName);
        return new File(folder, filename);
    }

    @Nullable
    public File getPublicFolder(String folderName) {
        String externalStoragePath = externalStorageHelper.getExternalStoragePath();
        if (externalStoragePath == null) {
            return null;
        }
        return new File(externalStoragePath + File.separator + folderName);
    }

    @Nullable
    public File getAppExternalFolder(String folderName) {
        String path = getAppExternalFolderPath(folderName);
        File folder = null;
        if (path != null) {
            folder = new File(path);
        }
        return folder;
    }

    @Nullable
    public File getExistingAppExternalFolder(String publicFolderName) {
        File destinationDataFolder = getAppExternalFolder(publicFolderName);
        if (destinationDataFolder != null && !destinationDataFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            destinationDataFolder.mkdirs();
        }
        return destinationDataFolder;
    }

    public String getVideoFilePath() {
        String filename = UUID.randomUUID().toString() + VIDEO_SUFFIX;
        File mediaFolder = getInternalFolder(DIR_MEDIA);
        if (!mediaFolder.exists()) {
            mediaFolder.mkdirs();
        }
        return new File(mediaFolder, filename).getAbsolutePath();
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
