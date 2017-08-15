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

package org.akvo.flow.data.datasource;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Flowable;
import rx.functions.Func2;
import timber.log.Timber;

@Singleton
public class ImageDataSource {

    private static final int RESIZED_IMAGE_WIDTH = 320;
    private static final int RESIZED_IMAGE_HEIGHT = 240;
    private static final int QUALITY_FULL = 100;

    @Inject
    public ImageDataSource() {
    }

    private Observable<Boolean> saveImage(Bitmap bitmap, String filename) {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(filename));
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY_FULL, out)) {
                return Observable.just(true);
            }
        } catch (FileNotFoundException e) {
            Timber.e(e.getMessage());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {
                    //Ignored
                }
            }
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

    public Observable<Boolean> saveImages(Bitmap bitmap, String originalFilePath,
            String resizedFilePath) {
        return Observable.zip(saveImage(bitmap, originalFilePath),
                saveResizedImage(bitmap, resizedFilePath), new Func2<Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean savedImage, Boolean savedResizedImage) {
                        return savedImage && savedResizedImage;
                    }
                });
    }
}
