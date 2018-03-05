/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import timber.log.Timber;

public class MediaFileHelper {

    private static final String TEMP_PHOTO_NAME_PREFIX = "image";
    private static final String TEMP_VIDEO_NAME_PREFIX = "video";
    private static final String IMAGE_SUFFIX = ".jpg";
    private static final String VIDEO_SUFFIX = ".mp4";

    private final Context context;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);

    @Inject
    public MediaFileHelper(Context context) {
        this.context = context;
    }

    @NonNull
    public String getImageFilePath() {
        return getNamedMediaFile(IMAGE_SUFFIX).getAbsolutePath();
    }

    @Nullable
    public String getVideoFilePath(Intent intent) {
        File tmp = getVideoTmpFile();
        if (!tmp.exists()) {
            tmp = new File(getVideoPathFromIntent(intent));
        }
        return renameFile(tmp);
    }

    /**
     * On some devices the uri we pass for taking videos is ignored and in this case we need to get
     * the actual uri returned by the intent
     */
    private String getVideoPathFromIntent(Intent intent) {
        String videoAbsolutePath = null;
        Uri videoUri = intent.getData();
        if (videoUri != null) {
            String[] filePathColumns = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = context.getContentResolver()
                    .query(videoUri, filePathColumns, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumns[0]);
                videoAbsolutePath = cursor.getString(columnIndex);
                cursor.close();
            }
        }
        return videoAbsolutePath;
    }

    private String renameFile(File temporaryVideoFile) {
        File videoFile = getNamedMediaFile(VIDEO_SUFFIX);

        if (!temporaryVideoFile.renameTo(videoFile)) {
            Timber.e("Media file rename failed");
            return temporaryVideoFile.getAbsolutePath();
        }
        return videoFile.getAbsolutePath();
    }

    @NonNull
    public File getVideoTmpFile() {
        String filename = TEMP_VIDEO_NAME_PREFIX + VIDEO_SUFFIX;
        return getMediaFile(filename);
    }

    @Nullable
    public File getImageTmpFile() {
        String timeStamp = dateFormat.format(new Date());
        String imageFileName = TEMP_PHOTO_NAME_PREFIX + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile(imageFileName, IMAGE_SUFFIX, storageDir);
        } catch (IOException e) {
            Timber.e(e, "Unable to create image file");
        }
        return null;
    }

    @NonNull
    private File getNamedMediaFile(String fileSuffix) {
        String filename = PlatformUtil.uuid() + fileSuffix;
        return new File(FileUtil.getFilesDir(FileUtil.FileType.MEDIA), filename);
    }

    @NonNull
    private File getMediaFile(String filename) {
        return new File(FileUtil.getFilesDir(FileUtil.FileType.TMP), filename);
    }
}