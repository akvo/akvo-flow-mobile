/*
 * Copyright (C) 2014-2020 Stichting Akvo (Akvo Foundation)
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

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.response.value.Location;
import org.akvo.flow.domain.response.value.Media;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.serialization.response.value.MediaValue;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.uicomponents.SnackBarManager;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ImageUtil;
import org.akvo.flow.util.StoragePermissionsHelper;
import org.akvo.flow.util.image.GlideImageLoader;
import org.akvo.flow.util.image.ImageLoader;
import org.akvo.flow.utils.entity.Question;

import java.io.File;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Question type that supports taking a picture with the
 * device's on-board camera.
 *
 */
public class PhotoQuestionView extends QuestionView implements IPhotoQuestionView {

    @Inject
    SnackBarManager snackBarManager;

    @Inject
    Navigator navigator;

    @Inject
    PhotoQuestionPresenter presenter;

    @Inject
    StoragePermissionsHelper storagePermissionsHelper;

    @BindView(R.id.acquire_media_ll)
    View mediaLayout;

    @BindView(R.id.image)
    ImageView mImageView;

    @BindView(R.id.media_progress)
    ProgressBar mProgressBar;

    @BindView(R.id.media_download)
    View mDownloadBtn;

    @BindView(R.id.location_info)
    TextView mLocationInfo;

    private Media mMedia;
    private ImageLoader imageLoader;

    public PhotoQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.old_media_question_view);
        initialiseInjector();
        ButterKnife.bind(this);

        presenter.setView(this);

        imageLoader = new GlideImageLoader(getContext());
        if (isReadOnly()) {
            mediaLayout.setVisibility(GONE);
        }

        mMedia = null;
    }

    private void initialiseInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    @OnClick(R.id.image)
    void onImageViewClicked() {
        File file = rebuildFilePath();
        if (file != null && file.exists()) {
            AppCompatActivity activity = (AppCompatActivity) getContext();
            navigator.navigateToLargeImage(activity, file.getAbsolutePath());
        } else {
            showImageError();
        }
    }

    @OnClick(R.id.camera_btn)
    void onTakePictureClicked() {
        if (storagePermissionsHelper.isStorageAllowed()) {
            notifyQuestionListeners(QuestionInteractionEvent.TAKE_PHOTO_EVENT);
        } else {
            requestStoragePermissions();
        }
    }

    private void requestStoragePermissions() {
        final FormActivity activity = (FormActivity) getContext();
        activity.requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                ConstantUtil.STORAGE_PERMISSION_CODE, getQuestion().getId());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == ConstantUtil.STORAGE_PERMISSION_CODE) {
            if (storagePermissionsHelper.storagePermissionsGranted(permissions, grantResults)) {
                notifyQuestionListeners(QuestionInteractionEvent.TAKE_PHOTO_EVENT);
            } else {
                storagePermissionNotGranted();
            }
        }
    }

    private void storagePermissionNotGranted() {
        final View.OnClickListener retryListener = v -> {
            if (storagePermissionsHelper.userPressedDoNotShowAgain((FormActivity) getContext())) {
                navigator.navigateToAppSystemSettings(getContext());
            } else {
                requestStoragePermissions();
            }
        };
        snackBarManager
                .displaySnackBarWithAction(this,
                        R.string.storage_permission_missing,
                        R.string.action_retry, retryListener, getContext());
    }

    @OnClick(R.id.gallery_btn)
    void onGetPictureClicked() {
        notifyQuestionListeners(QuestionInteractionEvent.GET_PHOTO_EVENT);
    }

    @OnClick(R.id.media_download)
    void onImageDownloadClick() {
        presenter.downloadMedia(mMedia.getFilename());
    }

    @Override
    public void showLoading() {
        mDownloadBtn.setVisibility(GONE);
        mProgressBar.setVisibility(VISIBLE);
    }

    /**
     * display the completion icon and install the response in the question
     * object
     */
    @Override
    public void onQuestionResultReceived(Bundle mediaData) {
        Uri imagePath =
                mediaData != null ?
                        (Uri) mediaData.getParcelable(ConstantUtil.IMAGE_FILE_KEY) : null;
        boolean deleteOriginal =
                mediaData != null && mediaData
                        .getBoolean(ConstantUtil.PARAM_REMOVE_ORIGINAL, false);
        presenter.onImageReady(imagePath, deleteOriginal);
    }

    @Override
    public void displayImage(Media media) {
        mMedia = media;

        captureResponse();
        displayThumbnail();

        displayLocationInfo();
    }

    @Override
    public void hideLoading() {
        mProgressBar.setVisibility(GONE);
    }

    @Override
    public void showErrorGettingMedia() {
        snackBarManager.displaySnackBar(this, R.string.error_getting_media, getContext());
    }

    @Override
    public void updateResponse(String localFilePath) {
        mMedia.setFilename(localFilePath);
        captureResponse();
    }

    /**
     * restores the file path for the file and turns on the complete icon if the
     * file exists
     */
    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        String value = resp == null ? null : resp.getValue();
        if (!TextUtils.isEmpty(value)) {
            mMedia = MediaValue.deserialize(value);
            displayThumbnail();
            presenter.onFilenameAvailable(mMedia.getFilename(), isReadOnly());
        }
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mMedia = null;
        mImageView.setImageDrawable(null);
        hideDownloadViews();
        mLocationInfo.setVisibility(GONE);
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        QuestionResponse response = null;
        if (mMedia != null && !TextUtils.isEmpty(mMedia.getFilename())) {
            Question question = getQuestion();
            String value = MediaValue.serialize(mMedia, true);
            response = new QuestionResponse.QuestionResponseBuilder()
                    .setValue(value)
                    .setType(ConstantUtil.IMAGE_RESPONSE_TYPE)
                    .setQuestionId(question.getId())
                    .setIteration(question.getIteration())
                    .setFilename(mMedia.getFilename())
                    .createQuestionResponse();
        }
        setResponse(response);
    }

    @Override
    public void onDestroy() {
        presenter.destroy();
    }

    @Override
    public boolean isValid() {
        if (getQuestion().isMandatory()) {
            File file = rebuildFilePath();
            if (file == null|| !file.exists()) {
                return false;
            }
        }
        return super.isValid();
    }

    @Override
    public void displayThumbnail() {
        hideDownloadViews();

        File file = rebuildFilePath();
        if (file != null && file.exists()) {
            imageLoader.loadFromFile(file, mImageView);
        } else {
            showImageCanBeDownloaded();
        }
    }

    private void showImageCanBeDownloaded() {
        mImageView.setImageResource(R.drawable.blurry_image);
        mDownloadBtn.setVisibility(VISIBLE);
    }

    private void hideDownloadViews() {
        mProgressBar.setVisibility(View.GONE);
        mDownloadBtn.setVisibility(View.GONE);
    }

    @Override
    public void displayLocationInfo() {
        Location mediaLocation = mMedia.getLocation();
        if (mediaLocation != null) {
            displayLocation(mediaLocation.getLatitude(), mediaLocation.getLongitude());
        } else {
            File file = rebuildFilePath();
            if (file != null && file.exists()) {
                double[] location = ImageUtil.getLocation(file.getAbsolutePath());
                if (location != null) {
                    displayLocation(location[0], location[1]);
                } else {
                    mLocationInfo.setVisibility(VISIBLE);
                    mLocationInfo.setText(R.string.image_location_unknown);
                }
            } else {
                mLocationInfo.setVisibility(GONE);
            }
        }
    }

    @Override
    public void showImageError() {
        snackBarManager.displaySnackBar(this, R.string.error_img_preview, getContext());
    }

    private void displayLocation(double latitude, double longitude) {
        String locationText = getContext()
                .getString(R.string.image_location_coordinates, latitude + "", longitude + "");
        mLocationInfo.setText(locationText);
        mLocationInfo.setVisibility(VISIBLE);
    }

    /**
     * File paths cannot be trusted so we need to get the name of the file and rebuild the path.
     * All media files should be located in the same folder
     *
     * @return File with the correct file path on the device
     */
    @Nullable
    private File rebuildFilePath() {
        String filePath = mMedia != null ? mMedia.getFilename() : null;
        return presenter.getExistingImageFilePath(filePath);
    }
}
