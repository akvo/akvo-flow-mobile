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
import android.net.Uri;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.datasource.MediaDataSource;
import org.akvo.flow.domain.repository.FileRepository;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import timber.log.Timber;

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
    public Observable<Boolean> copyResizedImage(final Uri uri, final String resizedImagePath,
            final int imageSize, final boolean removeDuplicate) {
        return dataSourceFactory.getMediaDataSource().getInputStreamFromUri(uri)
                .concatMap(new Function<InputStream, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(final InputStream inputStream) {
                        return saveResizedImage(uri, resizedImagePath, imageSize)
                                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                                    @Override
                                    public Observable<Boolean> apply(Boolean result) {
                                        if (removeDuplicate) {
                                            return removeDuplicateImage(uri);
                                        } else {
                                            return Observable.just(result);
                                        }
                                    }
                                });
                    }
                });
    }

    private Observable<Boolean> removeDuplicateImage(final Uri uri) {
        try {
            new File(uri.getPath()).delete();
        } catch (Exception e) {
            Timber.e(e);
        }
        return Observable.just(true);
    }

    private Observable<Boolean> saveResizedImage(final Uri uri, final String resizedImagePath,
            final int imageSize) {
        return dataSourceFactory.getMediaDataSource().getInputStreamFromUri(uri)
                .concatMap(new Function<InputStream, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(InputStream inputStream) {
                        return dataSourceFactory.getImageDataSource()
                                .saveResizedImage(uri, resizedImagePath, imageSize, inputStream);
                    }
                });
    }

    @Override
    public Observable<Boolean> moveFiles() {
        return Observable.merge(dataSourceFactory.getFileDataSource().moveZipFiles(),
                dataSourceFactory.getFileDataSource().moveMediaFiles())
                .concatMap(new Function<List<String>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(List<String> movedFiles) {
                        return Observable.just(true);
                    }
                });
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
    public Observable<File> getZipFile(String uuid) {
        return dataSourceFactory.getFileDataSource().getZipFile(uuid);
    }

    @Override
    public Observable<Boolean> createDataZip(String zipFileName,
            String formInstanceData) {
        return dataSourceFactory.getFileDataSource().writeDataToZipFile(zipFileName, formInstanceData);
    }

    @Override
    public Observable<String> copyVideo(final Uri uri, final boolean removeOriginal) {
        final MediaDataSource mediaDataSource = dataSourceFactory.getMediaDataSource();
        return mediaDataSource.getInputStreamFromUri(uri)
                .concatMap(new Function<InputStream, Observable<String>>() {
                    @Override
                    public Observable<String> apply(InputStream inputStream) {
                        return dataSourceFactory.getFileDataSource().copyVideo(inputStream)
                                .flatMap(new Function<String, Observable<String>>() {
                                    @Override
                                    public Observable<String> apply(String videoPath) {
                                        if (removeOriginal) {
                                            mediaDataSource.deleteMedia(uri);
                                        }
                                        return Observable.just(videoPath);
                                    }
                                });
                    }
                });
    }
}
