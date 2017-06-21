/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.signature;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.akvo.flow.domain.interactor.DefaultSubscriber;
import org.akvo.flow.domain.interactor.SaveImage;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.util.MediaFileHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

import static org.akvo.flow.util.MediaFileHelper.ORIGINAL_SUFFIX;
import static org.akvo.flow.util.MediaFileHelper.RESIZED_SUFFIX;

public class SignaturePresenter implements Presenter {

    private final UseCase saveImage;
    private final MediaFileHelper mediaFileHelper;

    private String questionId;
    private String datapointId;
    private SignatureView view;

    @Inject
    public SignaturePresenter(@Named("saveImage") UseCase saveImage,
            MediaFileHelper mediaFileHelper) {
        this.saveImage = saveImage;
        this.mediaFileHelper = mediaFileHelper;
    }

    @Override
    public void destroy() {
        saveImage.unSubscribe();
    }

    @NonNull
    File getOriginalSignatureFile() {
        return mediaFileHelper.getImageFile(ORIGINAL_SUFFIX, questionId, datapointId);
    }

    @NonNull private File getResizedSignatureFile() {
        return mediaFileHelper.getImageFile(RESIZED_SUFFIX, questionId, datapointId);
    }

    public void setExtras(String questionId, String datapointId, String name) {
        this.questionId = questionId;
        this.datapointId = datapointId;
        if (!TextUtils.isEmpty(name)) {
            view.setNameText(name);
        }
    }

    public void setView(SignatureView view) {
        this.view = view;
    }

    public void onViewContentChanged(String name, boolean emptySignature) {
        boolean isNameEmpty = TextUtils.isEmpty(name);
        boolean enableSaveButton = !isNameEmpty && !emptySignature;
        if (enableSaveButton) {
            view.enableSaveButton();
        } else {
            view.disableSaveButton();
        }
    }

    public void onSaveButtonTap(final String name, boolean emptySignature, Bitmap bitmap) {
        if (!emptySignature && !TextUtils.isEmpty(name)) {
            view.showSaving();

            Map<String, Object> params = new HashMap<>(4);
            params.put(SaveImage.RESIZED_FILE_NAME_PARAM,
                    getResizedSignatureFile().getAbsolutePath());
            params.put(SaveImage.ORIGINAL_FILE_NAME_PARAM,
                    getOriginalSignatureFile().getAbsolutePath());
            params.put(SaveImage.IMAGE_BITMAP_PARAM, bitmap);
            saveImage.execute(new DefaultSubscriber() {
                @Override
                public void onError(Throwable e) {
                    Timber.e(e, "Error saving image");
                    //TODO: display error???
                }

                @Override
                public void onNext(Object o) {
                    view.finishWithResultOK(name);
                }
            }, params);
        }
    }
}
