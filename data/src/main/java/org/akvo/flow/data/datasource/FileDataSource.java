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
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.data.entity.MovedFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;

@Singleton
public class FileDataSource {

    private static final String DIR_DATA = "akvoflow/data/files";
    private static final String DIR_MEDIA = "akvoflow/data/media";

    private final Context context;
    private final FileHelper fileHelper;

    @Inject
    public FileDataSource(Context context, FileHelper fileHelper) {
        this.context = context;
        this.fileHelper = fileHelper;
    }

    public Observable<List<MovedFile>> moveZipFiles() {
        return moveFiles(DIR_DATA);
    }

    public Observable<List<MovedFile>> moveMediaFiles() {
        return moveFiles(DIR_MEDIA);
    }

    private Observable<List<MovedFile>> moveFiles(String folderName) {
        File publicFolder = getPublicFolder(folderName);
        List<MovedFile> movedFiles = new ArrayList<>();
        if (publicFolder.exists()) {
            File[] files = publicFolder.listFiles();
            movedFiles = copyFiles(files, folderName);
            if (files.length == movedFiles.size()) {
                //noinspection ResultOfMethodCallIgnored
                publicFolder.delete();
            }
        }
        return Observable.just(movedFiles);
    }

    private List<MovedFile> copyFiles(@Nullable File[] files, String folderName) {
        List<MovedFile> movedFiles = new ArrayList<>();
        if (files != null) {
            File folder = getPrivateFolder(folderName);
            for (File f : files) {
                String destinationPath = fileHelper.copyFile(f, folder);
                if (!TextUtils.isEmpty(destinationPath)) {
                    movedFiles.add(new MovedFile(f.getPath(), destinationPath));
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        }
        return movedFiles;
    }

    @NonNull
    private File getPublicFolder(String folderName) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                + folderName;
        return new File(path);
    }

    private File getPrivateFolder(String folderName) {
        File folder = new File(
                context.getFilesDir().getAbsolutePath() + File.separator + folderName);
        if (!folder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        }
        return folder;
    }

    public Observable<Boolean> copyPrivateData() {
        //TODO: error handling will be added in separate issue
        copyPrivateFileToPublic(DIR_DATA);
        copyPrivateFileToPublic(DIR_MEDIA);
        return Observable.just(true);
    }

    private void copyPrivateFileToPublic(String folderName) {
        File destinationDataFolder = getPublicFolder(folderName);
        if (!destinationDataFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            destinationDataFolder.mkdirs();
        }
        File dataFolder = getPrivateFolder(folderName);
        if (dataFolder.exists()) {
            File[] files = dataFolder.listFiles();
            if (files != null) {
                for (File f : files) {
                    fileHelper.copyFile(f, destinationDataFolder);
                }
            }
        }
    }
}
