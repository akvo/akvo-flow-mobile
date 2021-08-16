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
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.domain.util.VersionHelper;
import org.akvo.flow.util.FileUtil;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class ApkFileBrowser {

    private static final String DIR_APK = "apk"; // App upgrades

    private final FileBrowser fileBrowser;
    private final VersionHelper versionHelper;

    @Inject
    public ApkFileBrowser(FileBrowser fileBrowser,
            VersionHelper versionHelper) {
        this.fileBrowser = fileBrowser;
        this.versionHelper = versionHelper;
    }

    @Nullable
    public String getFileName(Context context, String version, String apkFileName) {
        File apkFolder = getApkRootFolder(context);
        if (apkFolder == null) {
            Timber.e(new Exception("App external storage unavailable"));
            return null;
        }
        if (!apkFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            apkFolder.mkdirs();
        }
        File directory = new File(apkFolder, version);
        if (!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdir();
        }
        File file = new File(directory, apkFileName);
        return file.getAbsolutePath();
    }

    private File getApkRootFolder(Context context) {
        return fileBrowser.getAppExternalFolder(context, DIR_APK);
    }

    @NonNull
    public List<File> findAllPossibleFolders(Context context) {
        return fileBrowser.findAllPossibleFolders(context, DIR_APK);
    }

    /**
     * Check out previously downloaded files. If the APK update is already downloaded,
     * and the MD5 checksum matches, the file is considered downloaded.
     *
     * @return filename of the already downloaded file, if exists. Null otherwise
     */
    public String verifyLatestApkFile(Context context, @Nullable String apkCheckSum) {
        if (apkCheckSum == null) {
            return null;
        }
        File file = getLatestApkFile(context);
        if (file != null) {
            if (apkCheckSum.equals(FileUtil.hexMd5(file))) {
                return file.getAbsolutePath();
            } else {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
        return null;
    }

    /**
     * Check for the latest downloaded version. If old versions are found, delete them.
     * The APK corresponding to the installed version will also be deleted, if found,
     * in order to perform a cleanup after an upgrade.
     * Apks are placed inside [external storage storage]/apk/[version]/
     *
     * @return the path and version of a newer APK, if found, null otherwise
     */
    @VisibleForTesting
    @Nullable
    File getLatestApkFile(Context context) {
        String latestKnowVersion = BuildConfig.VERSION_NAME;
        File apk = null;

        File[] versionsFolders = getApksFoldersList(context);
        if (versionsFolders != null) {
            for (File versionFolder : versionsFolders) {
                File[] apks = versionFolder.listFiles();
                String currentFolderVersionName = versionFolder.getName();
                if (!versionHelper.isNewerVersion(latestKnowVersion, currentFolderVersionName)) {
                    // Delete old versions
                    FileUtil.deleteFilesInDirectory(versionFolder, true);
                } else if (apks != null && apks.length > 0) {
                    latestKnowVersion = currentFolderVersionName;
                    apk = apks[0]; // There should only be 1
                }
            }
        }
        return apk;
    }

    @VisibleForTesting
    @Nullable
    File[] getApksFoldersList(Context context) {
        File apksFolder = getApkRootFolder(context);
        return apksFolder.exists() ? apksFolder.listFiles() : null;
    }
}
