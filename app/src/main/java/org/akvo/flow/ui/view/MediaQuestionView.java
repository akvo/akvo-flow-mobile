/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.ui.view;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.async.MediaSyncTask;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.response.value.Location;
import org.akvo.flow.domain.response.value.Media;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.event.TimedLocationListener;
import org.akvo.flow.serialization.response.value.MediaValue;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.ImageUtil;

import java.io.File;

/**
 * Question type that supports taking a picture/video/audio recording with the
 * device's on-board camera.
 * 
 * @author Christopher Fagiani
 */
public class MediaQuestionView extends QuestionView implements OnClickListener,
        TimedLocationListener.Listener, MediaSyncTask.DownloadListener {
    private Button mMediaButton;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private View mDownloadBtn;
    private TextView mLocationInfo;
    private String mMediaType;
    private TimedLocationListener mLocationListener;
    private Media mMedia;

    public MediaQuestionView(Context context, Question q, SurveyListener surveyListener,
            String type) {
        super(context, q, surveyListener);
        mMediaType = type;
        mLocationListener = new TimedLocationListener(context, this, !q.isLocked());
        init();
    }

    private void init() {
        setQuestionView(R.layout.media_question_view);

        mMediaButton = (Button)findViewById(R.id.media_btn);
        mImageView = (ImageView)findViewById(R.id.image);
        mProgressBar = (ProgressBar)findViewById(R.id.progress);
        mDownloadBtn = findViewById(R.id.download);
        mLocationInfo = (TextView)findViewById(R.id.location_info);

        if (isImage()) {
            mMediaButton.setText(R.string.takephoto);
        } else {
            mMediaButton.setText(R.string.takevideo);
        }
        mMediaButton.setOnClickListener(this);
        if (isReadOnly()) {
            mMediaButton.setEnabled(false);
        }

        mImageView.setOnClickListener(this);
        mDownloadBtn.setOnClickListener(this);

        mMedia = null;

        hideDownloadOptions();
    }

    private void hideDownloadOptions() {
        mProgressBar.setVisibility(View.GONE);
        mDownloadBtn.setVisibility(View.GONE);
    }

    /**
     * handle the action button click
     */
    public void onClick(View v) {
        // TODO: Use switch instead of if-else
        if (v == mImageView) {
            String filename = mMedia != null ? mMedia.getFilename() : null;
            if (TextUtils.isEmpty(filename) || !(new File(filename).exists())) {
                Toast.makeText(getContext(), R.string.error_img_preview, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isImage()) {
                // Images are embedded in the app itself, whereas video are delegated through an Intent
                Dialog dia = new Dialog(new ContextThemeWrapper(getContext(), R.style.Flow_Dialog));
                dia.requestWindowFeature(Window.FEATURE_NO_TITLE);
                ImageView imageView = new ImageView(getContext());
                imageView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT));
                ImageUtil.displayImage(imageView, filename);
                dia.setContentView(imageView);
                dia.show();
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(filename)), "video/mp4");
                getContext().startActivity(intent);
            }
        } else if (v == mMediaButton) {
            if (isImage()) {
                notifyQuestionListeners(QuestionInteractionEvent.TAKE_PHOTO_EVENT);
            } else {
                notifyQuestionListeners(QuestionInteractionEvent.TAKE_VIDEO_EVENT);
            }
        } else if (v == mDownloadBtn) {
            mDownloadBtn.setVisibility(GONE);
            mProgressBar.setVisibility(VISIBLE);

            MediaSyncTask downloadTask = new MediaSyncTask(getContext(), new File(mMedia.getFilename()), this);
            downloadTask.execute();
        }
    }

    /**
     * display the completion icon and install the response in the question
     * object
     */
    @Override
    public void questionComplete(Bundle mediaData) {
        String result = mediaData != null ? mediaData.getString(ConstantUtil.MEDIA_FILE_KEY) : null;
        if (result != null) {
            mMedia = new Media();
            mMedia.setFilename(result);

            captureResponse();
            displayThumbnail();

            if (isImage()) {
                if (ImageUtil.getLocation(result) == null) {
                    mLocationListener.start();
                }
                displayLocationInfo();
            }
        }
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

        mMedia = MediaValue.deserialize(resp.getValue());

        displayThumbnail();
        String filename = mMedia.getFilename();
        if (TextUtils.isEmpty(filename)) {
            return;
        }
        // We now check whether the file is found in the local filesystem, and update the path if it's not
        File file = new File(filename);
        if (!file.exists() && isReadOnly()) {
            // Looks like the image is not present in the filesystem (i.e. remote URL)
            // Update response, matching the local path. Note: In the future, media responses should
            // not leak filesystem paths, for these are not guaranteed to be homogeneous in all devices.
            file = new File(FileUtil.getFilesDir(FileUtil.FileType.MEDIA), file.getName());
            mMedia.setFilename(file.getAbsolutePath());
            captureResponse();
        }
        displayLocationInfo();
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mMedia = null;
        mImageView.setImageDrawable(null);
        hideDownloadOptions();
        mLocationInfo.setVisibility(GONE);
        mLocationListener.stop();
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        QuestionResponse response = null;
        if (mMedia != null && !TextUtils.isEmpty(mMedia.getFilename())) {
            response = new QuestionResponse(MediaValue.serialize(mMedia),
                    isImage() ? ConstantUtil.IMAGE_RESPONSE_TYPE : ConstantUtil.VIDEO_RESPONSE_TYPE,
                    getQuestion().getId());
            response.setFilename(mMedia.getFilename());
        }
        setResponse(response);
    }

    @Override
    public void onDestroy() {
        if (mLocationListener.isListening()) {
            mLocationListener.stop();
        }
    }

    private void displayThumbnail() {
        hideDownloadOptions();

        String filename = mMedia != null ? mMedia.getFilename() : null;
        if (TextUtils.isEmpty(filename)) {
            return;
        }
        if (!new File(filename).exists()) {
            mImageView.setImageResource(R.drawable.blurry_image);
            mDownloadBtn.setVisibility(VISIBLE);
        } else if (isImage()) {
            // Image thumbnail
            ImageUtil.displayImage(mImageView, filename);
        } else {
            // Video thumbnail
            mImageView.setImageBitmap(ThumbnailUtils.createVideoThumbnail(
                    filename, MediaStore.Video.Thumbnails.MINI_KIND));
        }
    }

    private boolean isImage() {
        return ConstantUtil.PHOTO_QUESTION_TYPE.equals(mMediaType);
    }

    @Override
    public void onResourceDownload(boolean done) {
        if (!done) {
            Toast.makeText(getContext(), R.string.error_img_preview, Toast.LENGTH_SHORT).show();
        }
        displayThumbnail();
        displayLocationInfo();
    }

    @Override
    public void onLocationReady(double latitude, double longitude, double altitude, float accuracy) {
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
            // Add location to EXIF too
            ImageUtil.setLocation(mMedia.getFilename(), latitude, longitude);

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

    private void displayLocationInfo() {
        String filename = mMedia != null ? mMedia.getFilename() : null;
        if (TextUtils.isEmpty(filename) || !new File(filename).exists()) {
            mLocationInfo.setVisibility(GONE);
            return;
        }

        mLocationInfo.setVisibility(VISIBLE);
        float[] location = ImageUtil.getLocation(filename);
        if (location != null) {
            mLocationInfo.setText(R.string.image_location_saved);
        } else if (mLocationListener.isListening()) {
            mLocationInfo.setText(R.string.image_location_reading);
        } else {
            mLocationInfo.setText(R.string.image_location_unknown);
        }
    }

}
