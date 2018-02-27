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

import java.io.File;

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

    public Observable<Boolean> moveZipFiles() {
        return moveFiles(DIR_DATA);
    }

    public Observable<Boolean> moveMediaFiles() {
        return moveFiles(DIR_MEDIA);
    }

    private Observable<Boolean> moveFiles(String folderName) {
        File file = getPublicFolder(folderName);
        if (file.exists()) {
            File[] files = file.listFiles();
            final boolean success = copyFiles(files, folderName);
            if (success) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
        return Observable.just(true);
    }

    private boolean copyFiles(@Nullable File[] files, String folderName) {
        boolean copySuccess = true;
        if (files != null) {
            for (File f : files) {
                File folder = getPrivateDestinationFolder(folderName);
                boolean success = fileHelper.copyFile(f, folder);
                if (success) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                } else {
                    copySuccess = false;
                }
            }
        }
        return copySuccess;
    }

    @NonNull
    private File getPublicFolder(String folderName) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                + folderName;
        return new File(path);
    }

    private File getPrivateDestinationFolder(String folderName) {
        File folder = new File(
                context.getFilesDir().getAbsolutePath() + File.separator + folderName);
        if (!folder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        }
        return folder;
    }

}
