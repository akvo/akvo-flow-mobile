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

import org.akvo.flow.data.util.Constants;

import java.io.File;

public class S3File {

    public static final String S3_DATA_DIR = "devicezip";
    public static final String S3_IMAGE_DIR = "images";
    public static final String ACTION_SUBMIT = "submit";
    public static final String ACTION_IMAGE = "image";

    private final File file;
    private final boolean isPublic;
    private final String dir;
    private final String action;
    private final String md5Base64;
    private final String md5Hex;
    private final String filename;

    public S3File(File file, boolean isPublic, String dir, String action, String md5Base64,
            String md5Hex) {
        this.file = file;
        this.isPublic = isPublic;
        this.dir = dir;
        this.action = action;
        this.md5Base64 = md5Base64;
        this.filename = file.getName();
        this.md5Hex = md5Hex;
    }

    public File getFile() {
        return file;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getDir() {
        return dir;
    }

    public String getAction() {
        return action;
    }

    public String getMd5Base64() {
        return md5Base64;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        String ext = filename.substring(filename.lastIndexOf("."));
        switch (ext) {
            case Constants.PNG_SUFFIX:
                return Constants.PNG_CONTENT_TYPE;
            case Constants.JPG_SUFFIX:
                return Constants.JPEG_CONTENT_TYPE;
            case Constants.VIDEO_SUFFIX:
                return Constants.VIDEO_CONTENT_TYPE;
            case Constants.ARCHIVE_SUFFIX:
                return Constants.DATA_CONTENT_TYPE;
            default:
                return null;
        }
    }

    @NonNull
    public String getObjectKey() {
        return dir + "/" + filename;
    }

    public String getMd5Hex() {
        return md5Hex;
    }
}
