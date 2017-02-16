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
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;

import timber.log.Timber;

public class ImageUtil {

    public static String encodeBase64(Bitmap bitmap, int reqWidth, int reqHeight) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()),
                new RectF(0, 0, reqWidth, reqHeight), Matrix.ScaleToFit.CENTER);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] image = stream.toByteArray();
        return Base64.encodeToString(image, Base64.DEFAULT);
    }

    public static Bitmap decodeBase64(String data) {
        byte[] image = Base64.decode(data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(image, 0, image.length);
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

        Timber.d("Orig Image size: " + options.outWidth + "x" + options.outHeight);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(origFilename, options);

        if (bitmap != null && saveImage(bitmap, outFilename)) {
            checkOrientation(origFilename, outFilename);// Ensure the EXIF data is not lost
            Timber.d("Resized Image size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
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

    public static float[] getLocation(String image) {
        try {
            ExifInterface exif = new ExifInterface(image);
            float[] output = new float[2];
            if (exif.getLatLong(output)) {
                return output;
            }
        } catch (IOException e) {
            Timber.e(e.getMessage());
        }

        return null;
    }

    public static boolean setLocation(String image, double latitude, double longitude) {
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
            return true;
        } catch (IOException e) {
            Timber.e(e.getMessage());
        }

        return false;
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

    public static void displayImage(ImageView imageView, String filename) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // Calculate inSampleSize
        final int[] size = getImageSize(imageView);// [width, height]
        options.inSampleSize = calculateInSampleSize(options, size[0], size[1]);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(filename, options);

        if (bitmap != null) {
            Timber.d("Displaying image with inSampleSize: " + options.inSampleSize);
            imageView.setImageBitmap(bitmap);
        }
    }

    /**
     * Size computing algorithm:
     * 1) Get layout_width and layout_height. If both of them haven't exact value then go to step #2.
     * 2) Get maxWidth and maxHeight. If both of them are not set then go to step #3.
     * 3) Get device screen dimensions.
     */
    public static int[] getImageSize(ImageView imageView) {
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();

        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        int width = params.width; // Get layout width parameter
        if (width <= 0) width = getFieldValue(imageView, "mMaxWidth"); // Check maxWidth parameter
        if (width <= 0) width = displayMetrics.widthPixels;

        int height = params.height; // Get layout height parameter
        if (height <= 0) height = getFieldValue(imageView, "mMaxHeight"); // Check maxHeight parameter
        if (height <= 0) height = displayMetrics.heightPixels;

        return new int[]{width, height};
    }

    /**
     * Access the properties by Reflection.
     *
     * @param object
     * @param fieldName
     * @return
     */
    private static int getFieldValue(Object object, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = (Integer) field.get(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }
        return value;
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
