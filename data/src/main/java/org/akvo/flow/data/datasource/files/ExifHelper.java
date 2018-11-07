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

package org.akvo.flow.data.datasource.files;

import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import timber.log.Timber;

public class ExifHelper {

    @Inject
    public ExifHelper() {
    }

    boolean updateExifData(InputStream originalImageInputStream, String resizedImagePath) {
        try {
            ExifInterface originalImageExif = new ExifInterface(originalImageInputStream);
            ExifInterface newImageExif = new ExifInterface(resizedImagePath);
            final String originalImageOrientation = originalImageExif
                    .getAttribute(ExifInterface.TAG_ORIENTATION);
            final String newImageOrientation = newImageExif
                    .getAttribute(ExifInterface.TAG_ORIENTATION);

            if (!TextUtils.isEmpty(originalImageOrientation) && !originalImageOrientation
                    .equals(newImageOrientation)) {
                Timber.d("Exif orientation in resized image will be updated");
                newImageExif.setAttribute(ExifInterface.TAG_ORIENTATION, originalImageOrientation);
            }

            copyAttribute(originalImageExif, newImageExif, ExifInterface.TAG_GPS_LATITUDE);
            copyAttribute(originalImageExif, newImageExif, ExifInterface.TAG_GPS_LATITUDE_REF);
            copyAttribute(originalImageExif, newImageExif, ExifInterface.TAG_GPS_LONGITUDE);
            copyAttribute(originalImageExif, newImageExif, ExifInterface.TAG_GPS_LONGITUDE_REF);
            newImageExif.saveAttributes();
            originalImageInputStream.close();
        } catch (IOException e) {
            Timber.e(e);
        }
        return true;
    }

    boolean areDatesEqual(InputStream inputStream, @Nullable String imagePath) {
        boolean equals = false;
        if (!TextUtils.isEmpty(imagePath)) {
            try {
                ExifInterface originalImageExif = new ExifInterface(inputStream);
                ExifInterface newImageExif = new ExifInterface(imagePath);
                String originalFileDate = originalImageExif
                        .getAttribute(ExifInterface.TAG_DATETIME);
                String duplicatedFileDate = newImageExif.getAttribute(ExifInterface.TAG_DATETIME);
                Timber.d("original date: " + originalFileDate);
                Timber.d("duplicated date: " + duplicatedFileDate);
                equals = originalFileDate != null && originalFileDate.equals(duplicatedFileDate);
            } catch (IOException e) {
                Timber.e(e);
            }
        }

        return equals;
    }

    private void copyAttribute(ExifInterface originalImageExif, ExifInterface newImageExif,
            String attribute) {
        newImageExif.setAttribute(attribute, originalImageExif.getAttribute(attribute));
    }
}
