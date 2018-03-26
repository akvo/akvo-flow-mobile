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

import org.akvo.flow.util.files.FileBrowser;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import timber.log.Timber;

public class MediaFileHelper {

    private static final String TEMP_PHOTO_NAME_PREFIX = "image";
    private static final String TEMP_VIDEO_NAME_PREFIX = "video";
    private static final String IMAGE_SUFFIX = ".jpg";
    private static final String VIDEO_SUFFIX = ".mp4";
    private static final String DIR_MEDIA = "akvoflow/data/media";

    private final Context context;
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
    private final FileBrowser fileBrowser;

    @Inject
    public MediaFileHelper(Context context, FileBrowser fileBrowser) {
        this.context = context;
        this.fileBrowser = fileBrowser;
    }

    @NonNull
    public String getImageFilePath() {
        return getNamedMediaFile(IMAGE_SUFFIX).getAbsolutePath();
    }

    @NonNull
    public String getVideoFilePath() {
        return getNamedMediaFile(VIDEO_SUFFIX).getAbsolutePath();
    }

    @Nullable
    public String getAcquiredVideoFilePath(Intent intent, String mediaPath) {
        File tmp = new File(mediaPath);
        if (!tmp.exists()) {
            return getVideoPathFromIntent(intent);
        }
        return tmp.getAbsolutePath();
    }

    @Nullable
    public File getVideoTmpFile() {
        return getTempMediaFile(TEMP_VIDEO_NAME_PREFIX, VIDEO_SUFFIX);
    }

    @Nullable
    public File getImageTmpFile() {
        return getTempMediaFile(TEMP_PHOTO_NAME_PREFIX, IMAGE_SUFFIX);
    }

    @Nullable
    private File getTempMediaFile(String prefix, String suffix) {
        String timeStamp = dateFormat.format(new Date());
        String imageFileName = prefix + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile(imageFileName, suffix, storageDir);
        } catch (IOException e) {
            Timber.e(e, "Unable to create image file");
        }
        return null;
    }

    @NonNull
    public File getMediaFile(String filename) {
        File mediaFolder = fileBrowser.getExistingAppInternalFolder(context, DIR_MEDIA);
        return new File(mediaFolder, filename);
    }

    @NonNull
    public List<File> findAllPossibleFolders() {
        return fileBrowser.findAllPossibleFolders(context, DIR_MEDIA);
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

    @NonNull
    private File getNamedMediaFile(String fileSuffix) {
        String filename = PlatformUtil.uuid() + fileSuffix;
        File mediaFolder = fileBrowser.getExistingAppInternalFolder(context, DIR_MEDIA);
        return new File(mediaFolder, filename);
    }
}