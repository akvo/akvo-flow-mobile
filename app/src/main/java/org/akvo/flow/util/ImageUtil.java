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
import android.graphics.BitmapFactory;
import android.support.media.ExifInterface;
import android.text.TextUtils;
import android.util.Base64;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import timber.log.Timber;

public class ImageUtil {

    public static String encodeBase64(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] image = stream.toByteArray();
        return Base64.encodeToString(image, Base64.DEFAULT);
    }

    /**
     * resizeImage handles resizing a too-large image file from the camera,
     * @return true if the image was successfully resized to the new file, false otherwise
     */
    public static boolean resizeImage(String origFilename, String outFilename, int size) {
        int reqWidth, reqHeight;
        switch (size) {
            case ConstantUtil.IMAGE_SIZE_1280_960:
                reqWidth = 1280;
                reqHeight = 960;
                break;
            case ConstantUtil.IMAGE_SIZE_640_480:
                reqWidth = 640;
                reqHeight = 480;
                break;
            case ConstantUtil.IMAGE_SIZE_320_240:
            default:
                reqWidth = 320;
                reqHeight = 240;
                break;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(origFilename, options);

        // If image is in portrait mode, we swap the maximum width and height
        if (options.outHeight > options.outWidth) {
            int tmp = reqHeight;
            reqHeight = reqWidth;
            reqWidth = tmp;
        }

        Timber.d("Orig Image size: %d x %d", options.outWidth, options.outHeight);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(origFilename, options);

        if (bitmap != null && saveImage(bitmap, outFilename)) {
            checkOrientation(origFilename, outFilename);// Ensure the EXIF data is not lost
            Timber.d("Resized Image size: %d x %d", bitmap.getWidth(), bitmap.getHeight());
            return true;
        }
        return false;
    }

    private static void checkOrientation(String originalImage, String resizedImage) {
        try {
            ExifInterface exif1 = new ExifInterface(originalImage);
            ExifInterface exif2 = new ExifInterface(resizedImage);

            final String orientation1 = exif1.getAttribute(ExifInterface.TAG_ORIENTATION);
            final String orientation2 = exif2.getAttribute(ExifInterface.TAG_ORIENTATION);

            if (!TextUtils.isEmpty(orientation1) && !orientation1.equals(orientation2)) {
                Timber.d("Orientation property in EXIF does not match. Overriding it with original value...");
                exif2.setAttribute(ExifInterface.TAG_ORIENTATION, orientation1);
                exif2.saveAttributes();
            }
        } catch (IOException e) {
            Timber.e(e.getMessage());
        }

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

    /**
     * Compare to images to determine if their content is the same. To state
     * that the two of them are the same, the datetime contained in their exif
     * metadata will be compared. If the exif does not contain a datetime, the
     * MD5 checksum of the images will be compared.
     *
     * @param image1 Absolute path to the first image
     * @param image2 Absolute path to the second image
     * @return true if their datetime is the same, false otherwise
     */
    static boolean compareImages(String image1, String image2) {
        boolean equals = false;
        try {
            ExifInterface exif1 = new ExifInterface(image1);
            ExifInterface exif2 = new ExifInterface(image2);

            final String datetime1 = exif1.getAttribute(ExifInterface.TAG_DATETIME);
            final String datetime2 = exif2.getAttribute(ExifInterface.TAG_DATETIME);

            if (!TextUtils.isEmpty(datetime1) && !TextUtils.isEmpty(datetime2)) {
                equals = datetime1.equals(datetime2);
            } else {
                Timber.d("Datetime is null or empty. The MD5 checksum will be compared");
                equals = FileUtil.compareFilesChecksum(image1, image2);
            }
        } catch (IOException e) {
            Timber.e(e);
        }

        return equals;
    }

    private static boolean saveImage(Bitmap bitmap, String filename) {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(filename));
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)) {
                return true;
            }
        } catch (FileNotFoundException e) {
            Timber.e(e.getMessage());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {}
            }
        }

        return false;
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
     * results in a larger bitmap which isn't as useful for caching purposes.
     *
     * @param options An options object with out* params already populated (run through a decode*
     * method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    private static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
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
