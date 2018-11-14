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
            final String originalImageOrientation = getOrientation(originalImageExif);
            final String newImageOrientation = getOrientation(newImageExif);

            boolean orientationNeedsUpdate =
                    !TextUtils.isEmpty(originalImageOrientation) && !originalImageOrientation
                            .equals(newImageOrientation);
            if (orientationNeedsUpdate) {
                newImageExif.setAttribute(ExifInterface.TAG_ORIENTATION, originalImageOrientation);
            }

            copyAttribute(originalImageExif, newImageExif, ExifInterface.TAG_GPS_LATITUDE);
            copyAttribute(originalImageExif, newImageExif, ExifInterface.TAG_GPS_LATITUDE_REF);
            copyAttribute(originalImageExif, newImageExif, ExifInterface.TAG_GPS_LONGITUDE);
            copyAttribute(originalImageExif, newImageExif, ExifInterface.TAG_GPS_LONGITUDE_REF);
            newImageExif.saveAttributes();
        } catch (IOException e) {
            Timber.e(e);
        }
        return true;
    }

    private String getOrientation(ExifInterface exifInterface) {
        return exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
    }

    boolean areDatesEqual(InputStream inputStream, InputStream duplicateInputStream) {
        boolean equals = false;
        try {
            String originalFileDate = getExifDate(inputStream);
            String duplicatedFileDate = getExifDate(duplicateInputStream);
            equals = originalFileDate != null && originalFileDate.equals(duplicatedFileDate);
        } catch (IOException e) {
            Timber.e(e);
        }
        return equals;
    }

    private String getExifDate(InputStream inputStream) throws IOException {
        ExifInterface exifInterface = new ExifInterface(inputStream);
        return exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
    }

    private void copyAttribute(ExifInterface originalImageExif, ExifInterface newImageExif,
            String attribute) {
        newImageExif.setAttribute(attribute, originalImageExif.getAttribute(attribute));
    }
}
