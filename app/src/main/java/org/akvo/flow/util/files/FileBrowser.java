/*
 * Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.util.files;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.akvo.flow.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class FileBrowser {

    @Inject
    public FileBrowser() {
    }

    @NonNull
    @SuppressWarnings({ "unchecked", "ResultOfMethodCallIgnored" })
    public File getExistingAppInternalFolder(Context context, String folderName) {
        File folder = getAppInternalFolder(context, folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    @NonNull
    File findFile(Context context, String folderName, String fileName) {
        File file = getAppInternalFileIfExists(context, folderName, fileName);
        if (file == null) {
            file = getAppExternalFileIfExists(context, folderName, fileName);
        }
        if (file == null) {
            file = new File(getExistingPublicFolder(folderName), fileName);
        }
        return file;
    }

    @NonNull
    public List<File> findAllPossibleFolders(Context context, String folderName) {
        List<File> folders = new ArrayList<>(3);
        File folder = getAppInternalFolder(context, folderName);
        if (folder.exists()) {
            folders.add(folder);
        }
        File folder2 = getAppExternalFolder(context, folderName);
        if (folder2 != null && folder2.exists()) {
            folders.add(folder2);
        }
        File folder3 = getPublicFolder(folderName);
        if (folder3.exists()) {
            folders.add(folder3);
        }
        return folders;
    }

    @NonNull
    private File getAppInternalFolder(Context context, String folder) {
        String path = getAppInternalFolderPath(context, folder);
        return new File(path);
    }

    @NonNull
    private String getAppInternalFolderPath(Context context, String folder) {
        return context.getFilesDir().getAbsolutePath() + File.separator + folder;
    }

    @Nullable
    private String getAppExternalFolderPath(Context context, String folder) {
        String appExternalStoragePath = FileUtil.getAppExternalStoragePath(context);
        return appExternalStoragePath == null ?
                null :
                appExternalStoragePath + File.separator + folder;
    }

    @NonNull
    private String getPublicFolderPath(String folder) {
        return FileUtil.getExternalStoragePath() + File.separator + folder;
    }

    @Nullable
    File getAppExternalFolder(Context context, String folderName) {
        String path = getAppExternalFolderPath(context, folderName);
        File folder = null;
        if (path != null) {
            folder = new File(path);
        }
        return folder;
    }

    @Nullable
    private File getAppInternalFileIfExists(Context context, String folderName, String fileName) {
        File folder = getAppInternalFolder(context, folderName);
        if (folder.exists()) {
            File file = new File(folder, fileName);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    @Nullable
    private File getAppExternalFileIfExists(Context context, String folderName, String fileName) {
        File folder = getAppExternalFolder(context, folderName);
        if (folder != null && folder.exists()) {
            File file = new File(folder, fileName);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    @NonNull
    private File getPublicFolder(String folderName) {
        return new File(getPublicFolderPath(folderName));
    }

    @NonNull
    @SuppressWarnings({ "unchecked", "ResultOfMethodCallIgnored" })
    private File getExistingPublicFolder(String folderName) {
        File folder = getPublicFolder(folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }
}
