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

package org.akvo.flow.data.repository;

import android.graphics.Bitmap;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.entity.MovedFile;
import org.akvo.flow.domain.repository.FileRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class FileDataRepository implements FileRepository {

    private final DataSourceFactory dataSourceFactory;

    @Inject
    public FileDataRepository(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public Observable<Boolean> saveImage(Bitmap bitmap, String originalFilePath,
            String resizedFilePath) {
        return dataSourceFactory.getImageDataSource()
                .saveImages(bitmap, originalFilePath, resizedFilePath);
    }

    @Override
    public Observable<Boolean> saveResizedImage(String originalImagePath, String resizedImagePath,
            int imageSize) {
        return dataSourceFactory.getImageDataSource()
                .saveResizedImage(originalImagePath, resizedImagePath, imageSize);
    }

    @Override
    public Observable<Boolean> moveFiles() {
        return Observable.merge(dataSourceFactory.getFileDataSource().moveZipFiles(),
                dataSourceFactory.getFileDataSource().moveMediaFiles())
                .concatMap(new Function<List<MovedFile>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(List<MovedFile> movedFiles) throws Exception {
                        dataSourceFactory.getDataBaseDataSource().updateTransmissions(movedFiles);
                        return Observable.just(true);
                    }
                });
    }

    @Override
    public Observable<Boolean> copyPrivateData() {
        return dataSourceFactory.getFileDataSource().copyPrivateData();
    }
}
