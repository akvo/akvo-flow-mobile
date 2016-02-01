/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.ui.view;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
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
        LocationListener, MediaSyncTask.DownloadListener {
    private Button mMediaButton;
    private ImageView mImage;
    private ProgressBar mProgressBar;
    private View mDownloadBtn;
    private TextView mLocationInfo;
    private String mMediaType;
    private LocationManager mLocationManager;
    private boolean mListeningLocation;

    public MediaQuestionView(Context context, Question q, SurveyListener surveyListener,
            String type) {
        super(context, q, surveyListener);
        mMediaType = type;
        mLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        init();
    }

    private void init() {
        setQuestionView(R.layout.media_question_view);

        mMediaButton = (Button)findViewById(R.id.media_btn);
        mImage = (ImageView)findViewById(R.id.image);
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

        mImage.setOnClickListener(this);
        mDownloadBtn.setOnClickListener(this);

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
        if (v == mImage) {
            String filename = getResponse() != null ? getResponse().getValue() : null;
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

            MediaSyncTask downloadTask = new MediaSyncTask(getContext(), new File(getResponse().getValue()), this);
            downloadTask.execute();
        }
    }

    /**
     * display the completion icon and install the response in the question
     * object
     */
    @Override
    public void questionComplete(Bundle mediaData) {
        if (mediaData != null) {
            final String result = mediaData.getString(ConstantUtil.MEDIA_FILE_KEY);
            setResponse(new QuestionResponse(result,
                    isImage() ? ConstantUtil.IMAGE_RESPONSE_TYPE : ConstantUtil.VIDEO_RESPONSE_TYPE,
                    getQuestion().getId()));
            displayThumbnail();

            if (isImage()) {
                float[] location = ImageUtil.getLocation(result);
                if (location == null && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                    mListeningLocation = true;
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

        displayThumbnail();
        String filename = getResponse() != null ? getResponse().getValue() : null;
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
            setResponse(new QuestionResponse(file.getAbsolutePath(),
                    isImage() ? ConstantUtil.IMAGE_RESPONSE_TYPE : ConstantUtil.VIDEO_RESPONSE_TYPE,
                    getQuestion().getId()));
        }
        displayLocationInfo();
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mImage.setImageDrawable(null);
        hideDownloadOptions();
        mLocationInfo.setVisibility(GONE);
        mLocationManager.removeUpdates(this);
        mListeningLocation = false;
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
    }

    @Override
    public void onPause() {
        // Remove updates from LocationManager, to allow this object being GC
        mLocationManager.removeUpdates(this);
        if (mListeningLocation) {
            mListeningLocation = false;
            displayLocationInfo();
        }
    }

    private void displayThumbnail() {
        hideDownloadOptions();

        String filename = getResponse() != null ? getResponse().getValue() : null;
        if (TextUtils.isEmpty(filename)) {
            return;
        }
        if (!new File(filename).exists()) {
            mImage.setImageResource(R.drawable.blurry_image);
            mDownloadBtn.setVisibility(VISIBLE);
        } else if (isImage()) {
            // Image thumbnail
            ImageUtil.displayImage(mImage, filename);
        } else {
            // Video thumbnail
            mImage.setImageBitmap(ThumbnailUtils.createVideoThumbnail(
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
    public void onLocationChanged(Location location) {
        float currentAccuracy = location.getAccuracy();
        // if accuracy is 0 then the gps has no idea where we're at
        if (currentAccuracy > 0 && currentAccuracy <= 20f && mListeningLocation) {
            mLocationManager.removeUpdates(this);
            mListeningLocation = false;
            if (getResponse() != null && !TextUtils.isEmpty(getResponse().getValue())) {
                double lat = location.getLatitude(), lon = location.getLongitude();
                ImageUtil.setLocation(getResponse().getValue(), lat, lon);
                displayLocationInfo();
            }
        }
    }

    private void displayLocationInfo() {
        String filename = getResponse() != null ? getResponse().getValue() : null;
        if (TextUtils.isEmpty(filename) || !new File(filename).exists()) {
            mLocationInfo.setVisibility(GONE);
            return;
        }

        mLocationInfo.setVisibility(VISIBLE);
        float[] location = ImageUtil.getLocation(filename);
        if (location != null) {
            mLocationInfo.setText(R.string.image_location_saved);
        } else if (mListeningLocation) {
            mLocationInfo.setText(R.string.image_location_reading);
        } else {
            mLocationInfo.setText(R.string.image_location_unknown);
        }
    }

    public void onProviderDisabled(String provider) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

}
