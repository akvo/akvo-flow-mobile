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

package org.akvo.flow.domain.repository;

import android.graphics.Bitmap;
import android.net.Uri;

import org.akvo.flow.domain.entity.DomainImageMetadata;
import org.akvo.flow.domain.entity.InstanceIdUuid;

import java.io.File;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;

public interface FileRepository {

    Observable<Boolean> saveImage(@NonNull Bitmap bitmap, String fileName, String resizedFilePath);

    Observable<DomainImageMetadata> copyResizedImage(Uri fileName, String resizedFilePath, int imageSize,
            boolean removeDuplicate);

    Completable moveFiles();

    Observable<Boolean> publishFiles(@NonNull List<String> fileNames);

    Observable<Boolean> copyFile(String originFilePath, String destinationFilePath);

    Observable<Boolean> saveByteArrayToFile(byte[] imageByteArray, String destinationFilePath);

    Observable<Boolean> unPublishData();

    Observable<Boolean> clearResponseFiles();

    Observable<Boolean> clearAllUserFiles();

    Observable<Boolean> isExternalStorageFull();

    Observable<String> copyVideo(Uri uri, boolean removeOriginal);

    Completable createDataZip(String zipFileName, String formInstanceData);

    Maybe<File> getInstancesWithIncorrectZip(InstanceIdUuid instanceIdUuid);
}
