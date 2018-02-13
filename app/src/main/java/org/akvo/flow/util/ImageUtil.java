/*
 *  Copyright (C) 2012-2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.akvo.flow.util;

import android.graphics.Bitmap;
import android.support.media.ExifInterface;
import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import timber.log.Timber;

public class ImageUtil {

    public static String encodeBase64(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] image = stream.toByteArray();
        return Base64.encodeToString(image, Base64.DEFAULT);
    }

    public static double[] getLocation(String image) {
        try {
            ExifInterface exif = new ExifInterface(image);
            return exif.getLatLong();
        } catch (IOException e) {
            Timber.e(e);
        }
        return null;
    }

    public static void setLocation(String image, double latitude, double longitude) {
        try {
            ExifInterface exif = new ExifInterface(image);

            String latDMS = convertDMS(latitude);
            String lonDMS = convertDMS(longitude);
            String latRef = latitude >= 0d ? "N" : "S";
            String lonRef = longitude >= 0d ? "E" : "W";

            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latDMS);
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latRef);
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lonDMS);
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lonRef);
            exif.saveAttributes();
        } catch (IOException e) {
            Timber.e(e.getMessage());
        }

    }

    private static String convertDMS(double coordinate) {
        if (coordinate < -180.0 || coordinate > 180.0 || Double.isNaN(coordinate)) {
            throw new IllegalArgumentException("coordinate=" + coordinate);
        }

        // Fixed denominator for seconds
        final int secondsDenom = 1000;

        StringBuilder sb = new StringBuilder();

        coordinate = Math.abs(coordinate);

        int degrees = (int) Math.floor(coordinate);
        sb.append(degrees);
        sb.append("/1,");
        coordinate -= degrees;
        coordinate *= 60.0;
        int minutes = (int) Math.floor(coordinate);
        sb.append(minutes);
        sb.append("/1,");
        coordinate -= minutes;
        coordinate *= 60.0;
        coordinate *= secondsDenom;
        int secondsNum = (int) Math.floor(coordinate);
        sb.append(secondsNum);
        sb.append("/").append(secondsDenom);
        return sb.toString();
    }
}
