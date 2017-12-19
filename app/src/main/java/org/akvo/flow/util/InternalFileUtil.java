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
import java.util.ArrayList;
import java.util.List;

public abstract class InternalFileUtil {

    @NonNull
    @SuppressWarnings({ "unchecked", "ResultOfMethodCallIgnored" })
    public File getExistingAppInternalFolder(Context context) {
        File folder = getAppInternalFolder(context);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    @NonNull
    public File findFile(Context context, String fileName) {
        File file = getAppInternalFileIfExists(context, fileName);
        if (file == null) {
            file = getAppExternalFileIfExists(context, fileName);
        }
        if (file == null) {
            file = new File(getExistingPublicFolder(), fileName);
        }
        return file;
    }

    @NonNull
    public List<File> findAllPossibleFolders(Context context) {
        List<File> folders = new ArrayList<>(3);
        File folder = getAppInternalFolder(context);
        if (folder.exists()) {
            folders.add(folder);
        }
        File folder2 = getAppExternalFolder(context);
        if (folder2 != null && folder2.exists()) {
            folders.add(folder2);
        }
        File folder3 = getPublicFolder();
        if (folder3.exists()) {
            folders.add(folder3);
        }
        return folders;
    }

    @NonNull
    protected abstract String getAppInternalFolderPath(Context context);

    @Nullable
    protected abstract String getAppExternalFolderPath(Context context);

    @NonNull
    protected abstract String getPublicFolderPath();

    @Nullable
    private File getAppExternalFolder(Context context) {
        String path = getAppExternalFolderPath(context);
        File folder = null;
        if (path != null) {
            folder = new File(path);
        }
        return folder;
    }

    @Nullable
    private File getAppInternalFileIfExists(Context context, String fileName) {
        File folder = getAppInternalFolder(context);
        if (folder.exists()) {
            File file = new File(folder, fileName);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    @NonNull
    private File getAppInternalFolder(Context context) {
        String path = getAppInternalFolderPath(context);
        return new File(path);
    }

    @Nullable
    private File getAppExternalFileIfExists(Context context, String fileName) {
        File folder = getAppExternalFolder(context);
        if (folder != null && folder.exists()) {
            File file = new File(folder, fileName);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    @NonNull
    private File getPublicFolder() {
        return new File(getPublicFolderPath());
    }

    @NonNull
    @SuppressWarnings({ "unchecked", "ResultOfMethodCallIgnored" })
    private File getExistingPublicFolder() {
        File folder = getPublicFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }
}
