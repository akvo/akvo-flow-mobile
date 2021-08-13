/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.akvo.flow.domain.util.ImageSize;
import org.akvo.flow.utils.FileHelper;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;

import timber.log.Timber;

public class BitmapHelper {
    private static final int RESIZED_IMAGE_WIDTH = 320;
    private static final int RESIZED_IMAGE_HEIGHT = 240;
    private static final int QUALITY_FULL = 100;

    private final FileHelper fileHelper;

    @Inject
    public BitmapHelper(FileHelper fileHelper) {
        this.fileHelper = fileHelper;
    }

    boolean compressBitmap(@Nullable Bitmap bitmap, String filename) {
        if (bitmap == null) {
            return false;
        }
        OutputStream out = null;
        boolean saved = false;
        try {
            out = new BufferedOutputStream(new FileOutputStream(filename));
            saved = bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY_FULL, out);
        } catch (FileNotFoundException e) {
            Timber.e(e);
        } finally {
            fileHelper.close(out);
        }
        return saved;
    }

    Bitmap createResizedBitmap(@NonNull Bitmap bitmap) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()),
                new RectF(0, 0, RESIZED_IMAGE_WIDTH, RESIZED_IMAGE_HEIGHT),
                Matrix.ScaleToFit.CENTER);
        return Bitmap
                .createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    Bitmap getBitmap(int sizePreference, ParcelFileDescriptor parcelFileDescriptor) {
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        BitmapFactory.Options options = prepareBitmapOptions(fileDescriptor,
                sizePreference);
        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            fileHelper.close(parcelFileDescriptor);
        } else {
            try {
                parcelFileDescriptor.close();
            } catch (IOException e) {
                //ignore
            }
        }
        return bitmap;
    }

    @NonNull
    private BitmapFactory.Options prepareBitmapOptions(FileDescriptor origFilename,
            int sizePreference) {
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
}
