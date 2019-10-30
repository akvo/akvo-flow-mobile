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

package org.akvo.flow.data.datasource.files;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;

import org.akvo.flow.data.entity.images.DataImageLocation;
import org.akvo.flow.data.util.FileHelper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

public class MediaResolverHelper {

    private final Context context;
    private final ExifHelper exifHelper;
    private final FileHelper fileHelper;

    @Inject
    public MediaResolverHelper(Context context, ExifHelper exifHelper, FileHelper fileHelper) {
        this.context = context;
        this.exifHelper = exifHelper;
        this.fileHelper = fileHelper;
    }

    @Nullable
    public InputStream getInputStreamFromUri(@NonNull Uri uri) {
        try {
            return context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            Timber.e(e, "Error getting inputStream for: " + uri.toString());
            return null;
        }
    }

    @Nullable
    public ParcelFileDescriptor openFileDescriptor(@NonNull Uri uri) {
        try {
            return context.getContentResolver().openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            Timber.e(e);
            return null;
        }
    }

    public boolean removeDuplicateImage(@NonNull Uri uri) {
        String imagePath = getLastImageTakenPath();
        if (!TextUtils.isEmpty(imagePath)) {
            removeDuplicatedExtraFile(uri, imagePath);
        }
        return deleteMedia(uri);
    }

    DataImageLocation updateExifData(@NonNull Uri uri, @NonNull String resizedImagePath) {
        final InputStream inputStream = getInputStreamFromUri(uri);
        final DataImageLocation location = exifHelper.updateExifData(inputStream, resizedImagePath);
        fileHelper.close(inputStream);
        return location;
    }

    void removeDuplicatedExtraFile(@NonNull Uri uri, @NonNull String imagePath) {
        InputStream inputStream = null;
        FileInputStream fileInputStream = null;
        try {
            inputStream = getInputStreamFromUri(uri);
            fileInputStream = new FileInputStream(imagePath);
            if (exifHelper.areDatesEqual(inputStream, fileInputStream)) {
                deleteImageByPath(imagePath);
            }
        } catch (FileNotFoundException e) {
            Timber.e(e);
        } finally {
            fileHelper.close(fileInputStream);
            fileHelper.close(inputStream);
        }
    }

    boolean deleteMedia(@NonNull final Uri uri) {
        return context.getContentResolver().delete(uri, null, null) > 0;
    }

    private String getLastImageTakenPath() {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] {
                        MediaStore.Images.ImageColumns.DATA,
                        MediaStore.Images.ImageColumns.DATE_TAKEN
                },
                null,
                null,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
        );
        String lastImagePath = "";
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                lastImagePath = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.ImageColumns.DATA));
            }
            cursor.close();
        }
        return lastImagePath;
    }

    private void deleteImageByPath(String path) {
        context.getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Images.ImageColumns.DATA + "=?", new String[] { path });
    }
}
