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

package org.akvo.flow.presentation.form.caddisfly;

import androidx.annotation.NonNull;

import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.SaveByteArrayToFile;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.util.MediaFileHelper;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

public class CaddisflyPresenter implements Presenter {

    private final UseCase saveByteArrayToFile;
    private final MediaFileHelper mediaFileHelper;

    private CaddisflyView view;

    @Inject
    public CaddisflyPresenter(@Named("saveByteArrayToFile") UseCase saveByteArrayToFile, MediaFileHelper mediaFileHelper) {
        this.saveByteArrayToFile = saveByteArrayToFile;
        this.mediaFileHelper = mediaFileHelper;
    }

    public void setView(CaddisflyView view) {
        this.view = view;
    }

    public void onImageReady(@NonNull final String imageFileName, byte[] imageByteArray) {
        final String destinationFilePath = mediaFileHelper.getMediaFile(imageFileName)
                .getAbsolutePath();
        Map<String, Object> params = new HashMap<>(2);
        params.put(SaveByteArrayToFile.FILE_BYTE_ARRAY_PARAM, imageByteArray);
        params.put(SaveByteArrayToFile.DESTINATION_FILE_PATH_PARAM, destinationFilePath);
        saveByteArrayToFile.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                view.showErrorGettingMedia();
                view.updateResponse();
            }

            @Override
            public void onNext(@NonNull Boolean ignored) {
                view.updateResponse(destinationFilePath);
            }
        }, params);
    }

    @Override
    public void destroy() {
        saveByteArrayToFile.dispose();
    }
}
