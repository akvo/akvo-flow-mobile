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

package org.akvo.flow.data.datasource;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.text.TextUtils;

import org.akvo.flow.data.util.FileHelper;
import org.akvo.flow.data.util.ImageSize;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import timber.log.Timber;

@Singleton
public class ImageDataSource {

    private static final int RESIZED_IMAGE_WIDTH = 320;
    private static final int RESIZED_IMAGE_HEIGHT = 240;
    private static final int QUALITY_FULL = 100;

    private final FileHelper fileHelper;
    private final Context context;

    @Inject
    public ImageDataSource(FileHelper fileHelper, Context context) {
        this.fileHelper = fileHelper;
        this.context = context;
    }

    public Observable<Boolean> saveImages(Bitmap bitmap, String originalFilePath,
            String resizedFilePath) {
        return Observable.zip(saveImage(bitmap, originalFilePath),
                saveResizedImage(bitmap, resizedFilePath),
                new BiFunction<Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean apply(Boolean savedImage, Boolean savedResizedImage) {
                        return savedImage && savedResizedImage;
                    }
                });
    }

    public Observable<Boolean> saveResizedImage(final Uri originalImagePath,
            final String resizedImagePath, int imageSize, final InputStream inputStream) {
        return resizeImage(originalImagePath, resizedImagePath, imageSize)
                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(Boolean result) {
                        return updateExifOrientationData(inputStream, resizedImagePath);
                    }
                });
    }

    private Observable<Boolean> saveImage(@Nullable Bitmap bitmap, String filename) {
        if (bitmap == null) {
            return Observable.just(false);
        }
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(filename));
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY_FULL, out)) {
                return Observable.just(true);
            }
        } catch (FileNotFoundException e) {
            Timber.e(e);
        } finally {
            fileHelper.close(out);
        }
        return Observable.error(new Exception("Error saving bitmap"));
    }

    private Observable<Boolean> saveResizedImage(Bitmap bitmap, String absolutePath) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()),
                new RectF(0, 0, RESIZED_IMAGE_WIDTH, RESIZED_IMAGE_HEIGHT),
                Matrix.ScaleToFit.CENTER);
        Bitmap resizedBitmap = Bitmap
                .createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
        return saveImage(resizedBitmap, absolutePath);
    }

    private Observable<Boolean> resizeImage(Uri uri, String outFilename, int sizePreference) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    context.getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                BitmapFactory.Options options = prepareBitmapOptions(fileDescriptor, sizePreference);
                Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                parcelFileDescriptor.close();
                return saveImage(bitmap, outFilename);
            }
        } catch (IOException e) {
            Timber.e(e);
        }
        return Observable.error(new Exception("Error getting bitmap from uri: " + uri));
    }

    @NonNull
    private BitmapFactory.Options prepareBitmapOptions(FileDescriptor origFilename, int sizePreference) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(origFilename, null, options);
        Timber.d("Orig Image size: %d x %d", options.outWidth, options.outHeight);

        ImageSize imageSize = getTargetImageSize(sizePreference, options);

        options.inSampleSize = calculateInSampleSize(options, imageSize);

        Timber.d("Will sample size by: %s", options.inSampleSize);
        options.inJustDecodeBounds = false;
        return options;
    }

    @NonNull
    private ImageSize getTargetImageSize(int size, BitmapFactory.Options options) {
        ImageSize imageSize;
        switch (size) {
            case ImageSize.IMAGE_SIZE_1280_960:
                imageSize = new ImageSize(1280, 980);
                break;
            case ImageSize.IMAGE_SIZE_640_480:
                imageSize = new ImageSize(640, 480);
                break;
            case ImageSize.IMAGE_SIZE_320_240:
            default:
                imageSize = new ImageSize(320, 240);
                break;
        }

        if (options.outHeight > options.outWidth) {
            imageSize = imageSize.swapWidthHeightForPortrait();
        }
        return imageSize;
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize.
     *
     * @param options   An options object with out* params already populated
     * @param imageSize The requested width and height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    private int calculateInSampleSize(BitmapFactory.Options options, ImageSize imageSize) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        int requestedWidth = imageSize.getWidth();
        int requestedHeight = imageSize.getHeight();

        if (height > requestedHeight || width > requestedWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) requestedHeight);
            final int widthRatio = Math.round((float) width / (float) requestedWidth);

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
            final float totalReqPixelsCap = requestedWidth * requestedHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    private Observable<Boolean> updateExifOrientationData(InputStream originalImageInputStream,
            String resizedImagePath) {
        try {

            final String orientation1 = getExifOrientationTag(originalImageInputStream);
            final String orientation2 = getExifOrientationTag(resizedImagePath);

            if (!TextUtils.isEmpty(orientation1) && !orientation1.equals(orientation2)) {
                Timber.d("Exif orientation in resized image will be updated");
                updateExifOrientation(orientation1, resizedImagePath);

            }
            originalImageInputStream.close();
        } catch (IOException e) {
            Timber.e(e);
        }
        return Observable.just(true);
    }

    private void updateExifOrientation(String orientation, String filename) throws IOException {
        ExifInterface exif = new ExifInterface(filename);
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation);
        exif.saveAttributes();
    }

    private String getExifOrientationTag(InputStream inputStream) throws IOException {
        ExifInterface exif = new ExifInterface(inputStream);
        return exif.getAttribute(ExifInterface.TAG_ORIENTATION);
    }

    private String getExifOrientationTag(String filename) throws IOException {
        ExifInterface exif = new ExifInterface(filename);
        return exif.getAttribute(ExifInterface.TAG_ORIENTATION);
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
    private boolean compareImages(String image1, String image2) {
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
                equals = compareFilesChecksum(image1, image2);
            }
        } catch (IOException e) {
            Timber.e(e);
        }

        return equals;
    }

    /**
     * Compare two files to determine if their content is the same. To state that
     * the two of them are the same, the MD5 checksum will be compared. Note
     * that if any of the files does not exist, or if its checksum cannot be
     * computed, false will be returned.
     *
     * @param path1 Absolute path to the first file
     * @param path2 Absolute path to the second file
     * @return true if their MD5 checksum is the same, false otherwise.
     */
    private boolean compareFilesChecksum(String path1, String path2) {
        final byte[] checksum1 = fileHelper.getMD5Checksum(new File(path1));
        final byte[] checksum2 = fileHelper.getMD5Checksum(new File(path2));

        return Arrays.equals(checksum1, checksum2);
    }
}
