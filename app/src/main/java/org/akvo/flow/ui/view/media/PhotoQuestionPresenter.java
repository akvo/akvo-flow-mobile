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

package org.akvo.flow.ui.view.media;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.SaveResizedImage;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.response.value.Media;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.serialization.response.value.MediaValue;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.MediaFileHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

public class PhotoQuestionPresenter implements Presenter {

    private final UseCase saveResizedImage;
    private final MediaFileHelper mediaFileHelper;
    private IPhotoQuestionView view;

    @Inject
    public PhotoQuestionPresenter(@Named("saveResizedImage") UseCase saveResizedImage,
            MediaFileHelper mediaFileHelper) {
        this.saveResizedImage = saveResizedImage;
        this.mediaFileHelper = mediaFileHelper;
    }

    public void setView(IPhotoQuestionView view) {
        this.view = view;
    }

    @Override
    public void destroy() {
        saveResizedImage.dispose();
    }

    void onImageReady(@Nullable final String mediaFilePath) {
        if (!TextUtils.isEmpty(mediaFilePath)) {
            view.showLoading();
            final String resizedImageFilePath = mediaFileHelper.getImageFilePath();
            Map<String, Object> params = new HashMap<>(4);
            params.put(SaveResizedImage.ORIGINAL_FILE_NAME_PARAM, mediaFilePath);
            params.put(SaveResizedImage.RESIZED_FILE_NAME_PARAM, resizedImageFilePath);
            saveResizedImage.execute(new DefaultObserver<Boolean>() {
                @Override
                public void onNext(Boolean aBoolean) {
                    view.displayImage(resizedImageFilePath);
                }

                @Override
                public void onError(Throwable e) {
                    view.showErrorGettingMedia();
                }
            }, params);
        } else {
            view.showErrorGettingMedia();
        }
    }

    void onFilenameAvailable(String filename, boolean readOnly) {
        if (!TextUtils.isEmpty(filename)) {
            File file = new File(filename);
            if (!file.exists() && readOnly) {
                // Looks like the image is not present in the filesystem (i.e. remote URL)
                File localFile = mediaFileHelper.getMediaFile(file.getName());
                view.updateResponse(localFile.getAbsolutePath());
            }
            view.displayLocationInfo();
        }
    }
}
