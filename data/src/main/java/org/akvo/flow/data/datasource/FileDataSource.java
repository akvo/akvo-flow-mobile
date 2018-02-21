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

    public Observable<Boolean> deleteZipFiles() {
        File file = fileHelper.getPublicFolder(DIR_DATA);
        if (file.exists()) {
            File[] files = file.listFiles();
            deleteFiles(files);
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        return Observable.just(true);
    }

    public Observable<Boolean> moveMediaFiles() {
        File file = fileHelper.getPublicFolder(DIR_MEDIA);
        if (file.exists()) {
            File[] files = file.listFiles();
            copyFiles(files);
            deleteFiles(files);
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        return Observable.just(true);
    }

    private void deleteFiles(@Nullable File[] files) {
        if (files != null) {
            for (File f : files) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }


    private void copyFiles(@Nullable File[] files) {
        if (files != null) {
            File folder = getPrivateMediaFolder();
            for (File f : files) {
                fileHelper.copyFile(f, folder);
            }
        }
    }

    private File getPrivateMediaFolder() {
        File folder = new File(
                context.getFilesDir().getAbsolutePath() + File.separator + DIR_MEDIA);
        if (!folder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        }
        return folder;
    }

}
