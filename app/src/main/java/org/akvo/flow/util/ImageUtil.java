/*
 *  Copyright (C) 2012-2016,2018-2019 Stichting Akvo (Akvo Foundation)
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
import androidx.exifinterface.media.ExifInterface;
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
}
