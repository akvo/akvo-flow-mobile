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

package org.akvo.flow.data.repository;

import android.graphics.Bitmap;
import android.net.Uri;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.domain.entity.DomainImageMetadata;
import org.akvo.flow.domain.entity.InstanceIdUuid;
import org.akvo.flow.domain.repository.FileRepository;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
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
    public Observable<DomainImageMetadata> copyResizedImage(final Uri uri, final String resizedImagePath,
            final int imageSize, final boolean removeDuplicate) {
        return dataSourceFactory.getImageDataSource()
                .copyResizedImage(uri, resizedImagePath, imageSize, removeDuplicate);
    }

    @Override
    public Completable moveFiles() {
        return Completable.mergeArray(dataSourceFactory.getFileDataSource().moveZipFiles(),
                dataSourceFactory.getFileDataSource().moveMediaFiles());
    }

    @Override
    public Observable<Boolean> publishFiles(@NonNull List<String> fileNames) {
        return dataSourceFactory.getFileDataSource().publishFiles(fileNames);
    }

    @Override
    public Observable<Boolean> copyFile(String originFilePath, String destinationFilePath) {
        return dataSourceFactory.getFileDataSource().copyFile(originFilePath, destinationFilePath);
    }

    @Override
    public Observable<Boolean> unPublishData() {
        return dataSourceFactory.getFileDataSource().removePublishedFiles();
    }

    @Override
    public Observable<Boolean> clearResponseFiles() {
        return dataSourceFactory.getFileDataSource().deleteResponsesFiles();
    }

    @Override
    public Observable<Boolean> clearAllUserFiles() {
        return dataSourceFactory.getFileDataSource().deleteAllUserFiles();
    }

    @Override
    public Observable<Boolean> isExternalStorageFull() {
        return dataSourceFactory.getFileDataSource().getAvailableStorage()
                .map(new Function<Long, Boolean>() {
                    @Override
                    public Boolean apply(Long availableMb) {
                        return availableMb < 100;
                    }
                });
    }

    @Override
    public Maybe<File> getInstancesWithIncorrectZip(InstanceIdUuid instanceIdUuid) {
        return dataSourceFactory.getFileDataSource().getIncorrectZipFile(instanceIdUuid.getUuid());
    }

    @Override
    public Completable createDataZip(String zipFileName, String formInstanceData) {
        return dataSourceFactory.getFileDataSource()
                .writeDataToZipFile(zipFileName, formInstanceData);
    }

    @Override
    public Observable<String> copyVideo(final Uri uri, final boolean removeOriginal) {
        return dataSourceFactory.getVideoDataSource().copyVideo(uri, removeOriginal);
    }
}
