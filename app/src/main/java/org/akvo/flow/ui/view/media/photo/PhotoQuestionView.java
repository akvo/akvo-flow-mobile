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

package org.akvo.flow.ui.view.media.photo;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.async.MediaSyncTask;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.response.value.Location;
import org.akvo.flow.domain.response.value.Media;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.event.TimedLocationListener;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.serialization.response.value.MediaValue;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ImageUtil;
import org.akvo.flow.util.image.GlideImageLoader;
import org.akvo.flow.util.image.ImageLoader;

import java.io.File;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Question type that supports taking a picture with the
 * device's on-board camera.
 *
 * @author Christopher Fagiani
 */
public class PhotoQuestionView extends QuestionView implements
        TimedLocationListener.Listener, MediaSyncTask.DownloadListener, IPhotoQuestionView {

    @Inject
    SnackBarManager snackBarManager;

    @Inject
    Navigator navigator;

    @Inject
    PhotoQuestionPresenter presenter;

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

    private final TimedLocationListener mLocationListener;
    private Media mMedia;
    private ImageLoader imageLoader;

    public PhotoQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        mLocationListener = new TimedLocationListener(context, this, !q.isLocked());
        init();
    }

    private void init() {
        setQuestionView(R.layout.media_question_view);
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
        notifyQuestionListeners(QuestionInteractionEvent.TAKE_PHOTO_EVENT);
    }

    @OnClick(R.id.gallery_btn)
    void onGetPictureClicked() {
        notifyQuestionListeners(QuestionInteractionEvent.GET_PHOTO_EVENT);
    }

    @OnClick(R.id.media_download)
    void onImageDownloadClick() {
        showLoading();

        MediaSyncTask downloadTask = new MediaSyncTask(new File(mMedia.getFilename()), this);
        downloadTask.execute();
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
    public void questionComplete(Bundle mediaData) {
        Uri imagePath =
                mediaData != null ?
                        (Uri) mediaData.getParcelable(ConstantUtil.IMAGE_FILE_KEY) : null;
        boolean deleteOriginal =
                mediaData != null && mediaData
                        .getBoolean(ConstantUtil.PARAM_REMOVE_ORIGINAL, false);
        presenter.onImageReady(imagePath, deleteOriginal);
    }

    @Override
    public void displayImage(String mediaFilePath) {
        mMedia = new Media();
        mMedia.setFilename(mediaFilePath);

        captureResponse();
        displayThumbnail();

        updateImageLocation(mediaFilePath);
    }

    private void updateImageLocation(String mediaFilePath) {
        if (ImageUtil.getLocation(mediaFilePath) == null) {
            mLocationListener.start();
        }
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
        mLocationListener.stop();
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        QuestionResponse response = null;
        if (mMedia != null && !TextUtils.isEmpty(mMedia.getFilename())) {
            Question question = getQuestion();
            String value = MediaValue.serialize(mMedia);
            response = new QuestionResponse.QuestionResponseBuilder()
                    .setValue(value)
                    .setType(ConstantUtil.IMAGE_RESPONSE_TYPE)
                    .setQuestionId(question.getQuestionId())
                    .setIteration(question.getIteration())
                    .setFilename(mMedia.getFilename())
                    .createQuestionResponse();
        }
        setResponse(response);
    }

    @Override
    public void onDestroy() {
        if (mLocationListener.isListening()) {
            mLocationListener.stop();
        }
        presenter.destroy();
    }

    private void displayThumbnail() {
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
    public void onResourceDownload(boolean done) {
        if (!done) {
            showImageError();
        }
        displayThumbnail();
        displayLocationInfo();
    }

    @Override
    public void onLocationReady(double latitude, double longitude, double altitude,
            float accuracy) {
        if (accuracy > TimedLocationListener.ACCURACY_DEFAULT) {
            // This location is not accurate enough. Keep listening for updates
            return;
        }
        mLocationListener.stop();
        if (mMedia != null) {
            Location location = new Location();
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setAltitude(altitude);
            location.setAccuracy(accuracy);

            mMedia.setLocation(location);
            File file = rebuildFilePath();
            if (file != null && file.exists()) {
                ImageUtil.setLocation(file.getAbsolutePath(), latitude, longitude);
            }

            captureResponse();
            displayLocationInfo();
        }
    }

    @Override
    public void onTimeout() {
        displayLocationInfo();
    }

    @Override
    public void onGPSDisabled() {
        displayLocationInfo();
    }

    @Override
    public void displayLocationInfo() {
        File file = rebuildFilePath();
        if (file != null && file.exists()) {
            mLocationInfo.setVisibility(VISIBLE);
            double[] location = ImageUtil.getLocation(file.getAbsolutePath());
            if (location != null) {
                mLocationInfo.setText(R.string.image_location_saved);
            } else if (mLocationListener.isListening()) {
                mLocationInfo.setText(R.string.image_location_reading);
            } else {
                mLocationInfo.setText(R.string.image_location_unknown);
            }
        } else {
            mLocationInfo.setVisibility(GONE);
        }
    }

    private void showImageError() {
        snackBarManager.displaySnackBar(this, R.string.error_img_preview, getContext());
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
