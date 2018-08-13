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

package org.akvo.flow.ui.view.media.video;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.akvo.flow.R;
import org.akvo.flow.async.MediaSyncTask;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.response.value.Media;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.serialization.response.value.MediaValue;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.MediaFileHelper;
import org.akvo.flow.util.image.GlideImageLoader;
import org.akvo.flow.util.image.ImageLoader;

import java.io.File;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Question type that supports taking a video recording
 *
 * @author Christopher Fagiani
 */
public class VideoQuestionView extends QuestionView
        implements MediaSyncTask.DownloadListener, IVideoQuestionView {

    @Inject
    SnackBarManager snackBarManager;

    @Inject
    Navigator navigator;

    @Inject
    MediaFileHelper mediaFileHelper;

    @Inject
    VideoQuestionPresenter presenter;

    @BindView(R.id.acquire_media_ll)
    View mediaLayout;

    @BindView(R.id.image)
    ImageView mImageView;

    @BindView(R.id.media_progress)
    ProgressBar mProgressBar;

    @BindView(R.id.media_download)
    View mDownloadBtn;

    private ImageLoader imageLoader;
    private String filePath;

    public VideoQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
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
    }

    private void initialiseInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    private void hideDownloadOptions() {
        mProgressBar.setVisibility(View.GONE);
        mDownloadBtn.setVisibility(View.GONE);
    }

    @OnClick(R.id.image)
    void onVideoViewClicked() {
        File file = rebuildFilePath();
        if (file != null && file.exists()) {
            navigator.navigateToVideoView(getContext(), file.getAbsolutePath());
        } else {
            showVideoLoadError();
        }
    }

    @OnClick(R.id.camera_btn)
    void onTakeVideoClicked() {
        notifyQuestionListeners(QuestionInteractionEvent.TAKE_VIDEO_EVENT);
    }

    @OnClick(R.id.gallery_btn)
    void onGetPictureClicked() {
        notifyQuestionListeners(QuestionInteractionEvent.GET_VIDEO_EVENT);
    }

    @OnClick(R.id.media_download)
    void onVideoDownloadClick() {
        mDownloadBtn.setVisibility(GONE);
        mProgressBar.setVisibility(VISIBLE);

        MediaSyncTask downloadTask = new MediaSyncTask(getContext(),
                new File(filePath), this);
        downloadTask.execute();
    }

    @Override
    public void onDestroy() {
        presenter.destroy();
    }

    /**
     * display the completion icon and install the response in the question
     * object
     */
    @Override
    public void questionComplete(Bundle mediaData) {
        Uri uri = mediaData != null ?
                (Uri) mediaData.getParcelable(ConstantUtil.VIDEO_FILE_KEY) :
                null;
        presenter.onVideoReady(uri);
    }

    @Override
    public void showErrorGettingMedia() {
        snackBarManager.displaySnackBar(this, R.string.error_getting_media, getContext());
    }

    @Override
    public void showLoading() {
        mDownloadBtn.setVisibility(GONE);
        mProgressBar.setVisibility(VISIBLE);
    }

    @Override
    public void hideLoading() {
        mProgressBar.setVisibility(GONE);
    }

    @Override
    public void displayThumbnail(String videoFilePath) {
        filePath = videoFilePath;
        captureResponse();
        displayThumbnail();
    }

    /**
     * restores the file path for the file and turns on the complete icon if the
     * file exists
     */
    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);

        if (resp == null || TextUtils.isEmpty(resp.getValue())) {
            return;
        }

        Media mMedia = MediaValue.deserialize(resp.getValue());
        filePath = mMedia == null ? null : mMedia.getFilename();

        displayThumbnail();
        if (TextUtils.isEmpty(filePath)) {
            return;
        }

        // We now check whether the file is found in the local filesystem, and update the path if it's not
        File file = new File(filePath);
        if (!file.exists() && isReadOnly()) {
            // Looks like the image is not present in the filesystem (i.e. remote URL)
            // Update response, matching the local path. Note: In the future, media responses should
            // not leak filesystem paths, for these are not guaranteed to be homogeneous in all devices.
            file = mediaFileHelper.getMediaFile(file.getName());
            filePath = file.getAbsolutePath();
            captureResponse();
        }
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        filePath = null;
        mImageView.setImageDrawable(null);
        hideDownloadOptions();
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        QuestionResponse response = null;
        if (!TextUtils.isEmpty(filePath)) {
            Question question = getQuestion();
            Media media = new Media();
            media.setFilename(filePath);
            String value = MediaValue.serialize(media);
            response = new QuestionResponse.QuestionResponseBuilder()
                    .setValue(value)
                    .setType(ConstantUtil.VIDEO_RESPONSE_TYPE)
                    .setQuestionId(question.getQuestionId())
                    .setIteration(question.getIteration())
                    .setFilename(filePath)
                    .createQuestionResponse();
        }
        setResponse(response);
    }

    private void displayThumbnail() {
        hideDownloadOptions();
        File file = rebuildFilePath();
        if (file != null && file.exists()) {
            imageLoader.loadFromFile(file, mImageView);
        } else {
            showTemporaryMedia();
        }
    }

    private void showTemporaryMedia() {
        mImageView.setImageResource(R.drawable.blurry_image);
        mDownloadBtn.setVisibility(VISIBLE);
    }

    @Override
    public void onResourceDownload(boolean done) {
        if (!done) {
            showVideoLoadError();
        }
        displayThumbnail();
    }

    private void showVideoLoadError() {
        snackBarManager.displaySnackBar(this, R.string.error_video_preview, getContext());
    }

    /**
     * File paths cannot be trusted so we need to get the name of the file and rebuild the path.
     * All media files should be located in the same folder
     * @return File with the correct file path on the device
     */
    @Nullable
    private File rebuildFilePath() {
        return presenter.getExistingVideoFilePath(filePath);
    }
}
