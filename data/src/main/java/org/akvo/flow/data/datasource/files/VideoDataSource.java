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

package org.akvo.flow.data.datasource.files;

import android.net.Uri;

import org.akvo.flow.data.util.FlowFileBrowser;
import org.akvo.flow.utils.FileHelper;

import java.io.File;
import java.io.InputStream;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class VideoDataSource {

    private final FileHelper fileHelper;
    private final FlowFileBrowser flowFileBrowser;
    private final MediaResolverHelper mediaResolverHelper;

    @Inject
    public VideoDataSource(FileHelper fileHelper, FlowFileBrowser flowFileBrowser,
            MediaResolverHelper mediaResolverHelper) {
        this.fileHelper = fileHelper;
        this.flowFileBrowser = flowFileBrowser;
        this.mediaResolverHelper = mediaResolverHelper;
    }

    public Observable<String> copyVideo(final Uri uri, final boolean removeOriginal) {
        return copyVideo(uri)
                .flatMap(new Function<String, Observable<String>>() {
                    @Override
                    public Observable<String> apply(String videoPath) {
                        if (removeOriginal) {
                            mediaResolverHelper.deleteMedia(uri);
                        }
                        return Observable.just(videoPath);
                    }
                });
    }

    private Observable<String> copyVideo(final Uri uri) {
        InputStream inputStream = mediaResolverHelper.getInputStreamFromUri(uri);
        final String copiedVideoPath = flowFileBrowser.getVideoFilePath();
        return copyVideo(copiedVideoPath, inputStream)
                .map(new Function<Boolean, String>() {
                    @Override
                    public String apply(Boolean ignored) {
                        return copiedVideoPath;
                    }
                });
    }

    private Observable<Boolean> copyVideo(String destinationFilePath, InputStream inputStream) {
        File destinationFile = new File(destinationFilePath);
        String copiedFilePath = fileHelper.saveStreamToFile(inputStream, destinationFile);
        if (copiedFilePath == null) {
            return Observable.error(new Exception("Error copying video file"));
        }
        return Observable.just(true);
    }
}
