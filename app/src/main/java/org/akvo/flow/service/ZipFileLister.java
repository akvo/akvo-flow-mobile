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

package org.akvo.flow.service;

import android.os.Environment;

import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class ZipFileLister {

    @Inject
    public ZipFileLister() {
    }

    /**
     * returns an ordered list of zip files that exist in the device's bootstrap
     * directory
     */
    List<File> getSortedZipFiles() {
        List<File> zipFiles = getZipFiles();
        if (!zipFiles.isEmpty()) {
            Collections.sort(zipFiles);
        }
        return zipFiles;
    }

    List<File> getZipFiles() {
        List<File> zipFiles = new ArrayList<File>();
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File dir = FileUtil.getFilesDir(FileUtil.FileType.INBOX);
            File[] fileList = dir.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    if (isZipFile(file)) {
                        zipFiles.add(file);
                    }
                }
            }

        }
        return zipFiles;
    }

    private boolean isZipFile(File file) {
        return file.isFile() && file.getName().toLowerCase()
                .endsWith(ConstantUtil.ARCHIVE_SUFFIX);
    }
}