/*
 * Copyright (C) 2018-2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.view.media.video;

import android.net.Uri;
import android.text.TextUtils;

import org.akvo.flow.domain.interactor.CopyVideo;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.interactor.datapoints.DownloadMedia;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.util.MediaFileHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.Nullable;
import io.reactivex.observers.DisposableCompletableObserver;
import timber.log.Timber;

public class VideoQuestionPresenter implements Presenter {

    private final UseCase copyVideo;
    private final DownloadMedia downloadMediaUseCase;
    private final MediaFileHelper mediaFileHelper;

    private IVideoQuestionView view;

    @Inject
    public VideoQuestionPresenter(@Named("copyVideo") UseCase copyVideo,
            DownloadMedia downloadMediaUseCase,
            MediaFileHelper mediaFileHelper) {
        this.copyVideo = copyVideo;
        this.downloadMediaUseCase = downloadMediaUseCase;
        this.mediaFileHelper = mediaFileHelper;
    }

    public void setView(IVideoQuestionView view) {
        this.view = view;
    }

    @Override
    public void destroy() {
        copyVideo.dispose();
        downloadMediaUseCase.dispose();
    }

    public void downloadMedia(String filename) {
        view.showLoading();
        Map<String, Object> params = new HashMap<>(2);
        params.put(DownloadMedia.PARAM_FILE_PATH, filename);
        downloadMediaUseCase.execute(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                view.hideLoading();
                view.displayThumbnail();
            }

            @Override
            public void onError(Throwable e) {
                view.hideLoading();
                view.displayThumbnail();
                view.showVideoLoadError();
            }
        }, params);
    }


    public void onVideoReady(@Nullable Uri uri, boolean removeOriginal) {
            view.showLoading();
            Map<String, Object> params = new HashMap<>(4);
            params.put(CopyVideo.URI_ORIGINAL_FILE, uri);
            params.put(CopyVideo.REMOVE_ORIGINAL_IMAGE_PARAM, removeOriginal);
            copyVideo.execute(new DefaultObserver<String>() {
                @Override
                public void onNext(String targetVideoFilePath) {
                    view.displayThumbnail(targetVideoFilePath);
                }

                @Override
                public void onError(Throwable e) {
                    Timber.e(e);
                    view.hideLoading();
                    view.showErrorGettingMedia();
                }
            }, params);
    }

    @Nullable
    public File getExistingVideoFilePath(@Nullable String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        String filename = filePath;
        if (filePath.contains(File.separator)) {
            filename = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
        }
        if (TextUtils.isEmpty(filename)) {
            return null;
        }
        return mediaFileHelper.getMediaFile(filename);
    }
}
