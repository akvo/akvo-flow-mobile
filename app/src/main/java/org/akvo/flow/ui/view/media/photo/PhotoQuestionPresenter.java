/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.view.media.photo;

import android.net.Uri;
import android.text.TextUtils;

import org.akvo.flow.domain.entity.DomainImageMetadata;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.SaveResizedImage;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.util.MediaFileHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.Nullable;
import timber.log.Timber;

public class PhotoQuestionPresenter implements Presenter {

    private final UseCase saveResizedImage;
    private final MediaFileHelper mediaFileHelper;
    private IPhotoQuestionView view;
    private final MediaMapper mediaMapper;

    @Inject
    public PhotoQuestionPresenter(@Named("copyResizedImage") UseCase saveResizedImage,
            MediaFileHelper mediaFileHelper, MediaMapper mediaMapper) {
        this.saveResizedImage = saveResizedImage;
        this.mediaFileHelper = mediaFileHelper;
        this.mediaMapper = mediaMapper;
    }

    public void setView(IPhotoQuestionView view) {
        this.view = view;
    }

    @Override
    public void destroy() {
        saveResizedImage.dispose();
    }

    @Nullable
    public File getExistingImageFilePath(@Nullable String filePath) {
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

    void onImageReady(@Nullable final Uri originalFilePath, boolean deleteOriginal) {
        if (originalFilePath != null) {
            view.showLoading();
            final String resizedImageFilePath = mediaFileHelper.getImageFilePath();
            Map<String, Object> params = new HashMap<>(6);
            params.put(SaveResizedImage.ORIGINAL_FILE_NAME_PARAM, originalFilePath);
            params.put(SaveResizedImage.RESIZED_FILE_NAME_PARAM, resizedImageFilePath);
            params.put(SaveResizedImage.REMOVE_ORIGINAL_IMAGE_PARAM, deleteOriginal);
            saveResizedImage.execute(new DefaultObserver<DomainImageMetadata>() {
                @Override
                public void onNext(DomainImageMetadata imageMetadata) {
                    view.displayImage(mediaMapper.transform(imageMetadata));
                }

                @Override
                public void onError(Throwable e) {
                    Timber.e(e);
                    view.hideLoading();
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
