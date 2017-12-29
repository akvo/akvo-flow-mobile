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
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import org.akvo.flow.R;
import org.akvo.flow.util.files.TempFileBrowser;

import java.io.File;

import javax.inject.Inject;

import timber.log.Timber;

public class MediaFileHelper {

    public static final String RESIZED_SUFFIX = "resized";
    public static final String ORIGINAL_SUFFIX = "original";

    private static final String TEMP_PHOTO_NAME_PREFIX = "image";
    private static final String TEMP_VIDEO_NAME_PREFIX = "video";
    private static final String IMAGE_SUFFIX = ".jpg";
    private static final String VIDEO_SUFFIX = ".mp4";

    private final Context context;
    private final TempFileBrowser tempFileBrowser;

    @Inject
    public MediaFileHelper(Context context, TempFileBrowser tempFileBrowser) {
        this.context = context;
        this.tempFileBrowser = tempFileBrowser;
    }

    @NonNull
    public String getImageFilePath(int maxImgSize) {
        File tmp = getImageTmpFile();
        String tempAbsolutePath = tmp.getAbsolutePath();

        // Ensure no image is saved in the DCIM folder
        cleanDCIM(context, tempAbsolutePath);

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
        cleanDCIM(context, tempAbsolutePath);

        File imgFile = getNamedMediaFile(VIDEO_SUFFIX);
        String absolutePath = imgFile.getAbsolutePath();

        if (!tmp.renameTo(imgFile)) {
            // must check  return  value to  know if it  failed!
            Timber.e("Media file rename failed");
        }
        return absolutePath;
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
    public File getImageFile(String sizeSuffix, String questionId, String datapointId) {
        String fileName =
                "signature_" + questionId + "_" + datapointId + "_" + sizeSuffix + IMAGE_SUFFIX;
        return new File(tempFileBrowser.getExistingAppInternalFolder(context), fileName);
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
    private File getMediaFile(String filename) {
        return new File(tempFileBrowser.getExistingAppInternalFolder(context), filename);
    }

    /**
     * Some manufacturers will duplicate the image saving a copy in the DCIM
     * folder. This method will try to spot those situations and remove the
     * duplicated image.
     *
     * @param context  Context
     * @param filepath The absolute path to the original image
     */
    private void cleanDCIM(Context context, String filepath) {
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

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                final String lastImagePath = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.ImageColumns.DATA));

                if ((!filepath.equals(lastImagePath))
                        && (ImageUtil.compareImages(filepath, lastImagePath))) {
                    final int result = context.getContentResolver().delete(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            MediaStore.Images.ImageColumns.DATA + " = ?",
                            new String[] {
                                    lastImagePath
                            });

                    if (result == 1) {
                        Timber.i("Duplicated file successfully removed: %s", lastImagePath);
                    } else {
                        Timber.e("Error removing duplicated image: %s", lastImagePath);
                    }
                }
            }

            cursor.close();
        }
    }
}