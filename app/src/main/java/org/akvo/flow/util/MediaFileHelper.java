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

import org.akvo.flow.R;

import java.io.File;

import timber.log.Timber;

public class MediaFileHelper {

    private static final String TEMP_PHOTO_NAME_PREFIX = "image";
    private static final String TEMP_VIDEO_NAME_PREFIX = "video";
    private static final String IMAGE_SUFFIX = ".jpg";
    private static final String VIDEO_SUFFIX = ".mp4";

    private final Context context;

    public MediaFileHelper(Context context) {
        this.context = context;
    }

    @NonNull
    public String getImageFilePath(int maxImgSize) {
        File tmp = getImageTmpFile();
        String tempAbsolutePath = tmp.getAbsolutePath();

        // Ensure no image is saved in the DCIM folder
        FileUtil.cleanDCIM(context, tempAbsolutePath);

        File imgFile = getNamedMediaFile(IMAGE_SUFFIX);
        String absolutePath = imgFile.getAbsolutePath();

        if (ImageUtil.resizeImage(tempAbsolutePath, absolutePath, maxImgSize)) {
            Timber.i("Image resized to: %s", getReadableImageSize(maxImgSize));
            if (!tmp.delete()) { // must check return value to know if it failed
                Timber.e("Media file delete failed");
            }
        } else if (!tmp.renameTo(imgFile)) {
            // must check  return  value to  know if it  failed!
            Timber.e("Media file rename failed");
        }
        return absolutePath;
    }

    @NonNull
    public String getVideoFilePath() {
        File tmp = getVideoTmpFile();
        String tempAbsolutePath = tmp.getAbsolutePath();

        // Ensure no image is saved in the DCIM folder
        FileUtil.cleanDCIM(context, tempAbsolutePath);

        File imgFile = getNamedMediaFile(VIDEO_SUFFIX);
        String absolutePath = imgFile.getAbsolutePath();

       if (!tmp.renameTo(imgFile)) {
            // must check  return  value to  know if it  failed!
            Timber.e("Media file rename failed");
        }
        return absolutePath;
    }

    @NonNull
    private File getNamedMediaFile(String fileSuffix) {
        String filename = PlatformUtil.uuid() + fileSuffix;
        return new File(FileUtil.getFilesDir(FileUtil.FileType.MEDIA), filename);
    }

    private String getReadableImageSize(int maxImgSize) {
        return context.getResources()
                .getStringArray(R.array.max_image_size_pref)[maxImgSize];
    }

    @NonNull
    public File getVideoTmpFile() {
        String filename = TEMP_VIDEO_NAME_PREFIX + VIDEO_SUFFIX;
        return getMediaFile(filename);
    }

    @NonNull
    public File getImageTmpFile() {
        String filename = TEMP_PHOTO_NAME_PREFIX + IMAGE_SUFFIX;
        return getMediaFile(filename);
    }

    @NonNull
    private File getMediaFile(String filename) {
        return new File(FileUtil.getFilesDir(FileUtil.FileType.TMP), filename);
    }
}