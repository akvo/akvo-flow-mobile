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
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;

@Singleton
public class MediaDataSource {

    private final Context context;

    @Inject
    public MediaDataSource(Context context) {
        this.context = context;
    }

    public Observable<Boolean> deleteMedia(final Uri uri) {
        context.getContentResolver().delete(uri, null, null);
        return Observable.just(true);
    }

    public Observable<InputStream> getInputStreamFromUri(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return Observable.error(new Exception("null inputStream for: " + uri.toString()));
            }
            return Observable.just(inputStream);
        } catch (FileNotFoundException e) {
            return Observable.error(e);
        }
    }
}
