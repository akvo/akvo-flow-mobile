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

import org.akvo.flow.data.datasource.files.FileDataSource;
import org.akvo.flow.data.datasource.files.ImageDataSource;
import org.akvo.flow.data.datasource.files.VideoDataSource;
import org.akvo.flow.data.datasource.preferences.SecureSharedPreferencesDataSource;
import org.akvo.flow.data.datasource.preferences.SharedPreferencesDataSource;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DataSourceFactory {

    private final SharedPreferencesDataSource sharedPreferencesDataSource;
    private final ImageDataSource imageDataSource;
    private final DatabaseDataSource dataBaseDataSource;
    private final FileDataSource fileDataSource;
    private final SecureSharedPreferencesDataSource secureSharedPreferencesDataSource;
    private final VideoDataSource videoDataSource;

    @Inject
    public DataSourceFactory(SharedPreferencesDataSource sharedPreferencesDataSource,
            ImageDataSource imageDataSource, DatabaseDataSource dataBaseDataSource,
            SecureSharedPreferencesDataSource secureSharedPreferencesDataSource,
            FileDataSource fileDataSource, VideoDataSource videoDataSource) {
        this.sharedPreferencesDataSource = sharedPreferencesDataSource;
        this.imageDataSource = imageDataSource;
        this.dataBaseDataSource = dataBaseDataSource;
        this.secureSharedPreferencesDataSource = secureSharedPreferencesDataSource;
        this.fileDataSource = fileDataSource;
        this.videoDataSource = videoDataSource;
    }

    public SharedPreferencesDataSource getSharedPreferencesDataSource() {
        return sharedPreferencesDataSource;
    }

    public SecureSharedPreferencesDataSource getSecureSharedPreferencesDataSource() {
        return secureSharedPreferencesDataSource;
    }

    public ImageDataSource getImageDataSource() {
        return imageDataSource;
    }

    public DatabaseDataSource getDataBaseDataSource() {
        return dataBaseDataSource;
    }

    public FileDataSource getFileDataSource() {
        return fileDataSource;
    }

    public VideoDataSource getVideoDataSource() {
        return videoDataSource;
    }
}
