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

package org.akvo.flow.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.data.util.FlowFileBrowser;
import org.akvo.flow.data.util.Constants;
import org.akvo.flow.data.util.FileHelper;

import java.io.File;

import javax.inject.Inject;

public class S3FileMapper {

    private final FlowFileBrowser fileBrowser;
    private final FileHelper fileHelper;

    @Inject
    public S3FileMapper(FlowFileBrowser fileBrowser, FileHelper fileHelper) {
        this.fileBrowser = fileBrowser;
        this.fileHelper = fileHelper;
    }

    @Nullable
    public S3File transform(String filename) {
        S3File trans = null;
        File transmissionFile = getFile(filename);
        if (transmissionFile != null && transmissionFile.exists()) {
            String md5Checksum = fileHelper.getMd5Base64(transmissionFile);
            String md5Hex = fileHelper.hexMd5(transmissionFile);
            trans = new S3File(transmissionFile, isFilePublic(filename), getDir(filename),
                    getAction(filename), md5Checksum, md5Hex);
        }
        return trans;
    }

    private String getAction(String filename) {
        String ext = getFileExtension(filename);
        if (isMedia(ext)) {
            return S3File.ACTION_IMAGE;
        } else if (isArchive(ext)) {
            return S3File.ACTION_SUBMIT;
        }
        return null;
    }

    private String getDir(String filename) {
        String ext = getFileExtension(filename);
        if (isMedia(ext)) {
            return S3File.S3_IMAGE_DIR;
        } else if (isArchive(ext)) {
            return S3File.S3_DATA_DIR;
        }
        //unsupported file format found
        return null;
    }

    private boolean isFilePublic(String filename) {
        String ext = getFileExtension(filename);
        return isMedia(ext);
    }

    @Nullable
    private File getFile(@Nullable String filename) {
        if (TextUtils.isEmpty(filename)) {
            return null;
        }
        String ext = getFileExtension(filename);
        String folderName;
        if (isMedia(ext)) {
            folderName = FlowFileBrowser.DIR_MEDIA;
        } else if (isArchive(ext)) {
            folderName = FlowFileBrowser.DIR_DATA;
        } else {
            //unsupported file format found
            folderName = null;
        }
        return fileBrowser.getInternalFile(filename, folderName);
    }

    private boolean isMedia(@Nullable String ext) {
        return Constants.JPG_SUFFIX.equals(ext) || Constants.PNG_SUFFIX.equals(ext)
                || Constants.VIDEO_SUFFIX.equals(ext);
    }

    private boolean isArchive(@Nullable String ext) {
        return Constants.ARCHIVE_SUFFIX.equals(ext);
    }

    @NonNull
    private String getFileExtension(@Nullable String filename) {
        if (TextUtils.isEmpty(filename)) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
