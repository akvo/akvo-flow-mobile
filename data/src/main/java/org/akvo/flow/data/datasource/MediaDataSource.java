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

package org.akvo.flow.data.datasource;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import timber.log.Timber;

@Singleton
public class MediaDataSource {

    private final Context context;

    @Inject
    public MediaDataSource(Context context) {
        this.context = context;
    }

    public Observable<Boolean> notifyMediaDelete(final Uri uri) {
        context.getContentResolver().delete(uri, null, null);
        return Observable.just(true);
    }

    public Observable<String> getLastImageTaken() {
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
        String lastImagePath = "";
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                lastImagePath = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.ImageColumns.DATA));
            }
            cursor.close();
        }
        return Observable.just(lastImagePath);
    }

    public Observable<Boolean> deleteImage(String lastImagePath) {
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
        return Observable.just(true);
    }

    public Observable<String> getVideoFilePath(@Nullable Uri videoUri) {
        String videoAbsolutePath = null;
        if (videoUri != null) {
            String[] filePathColumns = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = context.getContentResolver()
                    .query(videoUri, filePathColumns, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    videoAbsolutePath = cursor.getString(columnIndex);
                }
                cursor.close();
            }
        }
        if (videoAbsolutePath == null) {
            return Observable.error(new Exception("Video path not found"));
        }
        return Observable.just(videoAbsolutePath);
    }

    public Observable<InputStream> getVideoInputStream(Uri uri) {
        try {
            return Observable.just(context.getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            return Observable.error(e);
        }
    }
}
