/*
 *  Copyright (C) 2010-2014 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;
import android.content.Intent;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ImageUtil;

import java.io.File;

/**
 * Question type that supports taking a picture/video/audio recording with the
 * device's on-board camera.
 * 
 * @author Christopher Fagiani
 */
public class MediaQuestionView extends QuestionView implements OnClickListener {
    private Button mMediaButton;
    private ImageView mImage;
    private String mMediaType;

    public MediaQuestionView(Context context, Question q, SurveyListener surveyListener,
            String type) {
        super(context, q, surveyListener);
        mMediaType = type;
        init();
    }

    private void init() {
        setQuestionView(R.layout.media_question_view);

        mMediaButton = (Button)findViewById(R.id.media_btn);
        mImage = (ImageView)findViewById(R.id.completed_iv);

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
        mImage.setVisibility(View.INVISIBLE);
    }

    /**
     * handle the action button click
     */
    public void onClick(View v) {
        if (v == mImage) {
            String filename = getResponse() != null ? getResponse().getValue() : null;
            if (TextUtils.isEmpty(filename) || !(new File(filename).exists())) {
                Toast.makeText(getContext(), R.string.error_img_preview, Toast.LENGTH_SHORT).show();
                return;
            }
            String type = isImage() ? ConstantUtil.IMAGE_MIME : ConstantUtil.VIDEO_MIME;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(filename)), type);
            getContext().startActivity(intent);
        } else if (v == mMediaButton) {
            if (isImage()) {
                notifyQuestionListeners(QuestionInteractionEvent.TAKE_PHOTO_EVENT);
            } else {
                notifyQuestionListeners(QuestionInteractionEvent.TAKE_VIDEO_EVENT);
            }
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
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mImage.setVisibility(View.GONE);
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
    }

    private void displayThumbnail() {
        String filename = getResponse() != null ? getResponse().getValue() : null;
        if (TextUtils.isEmpty(filename)) {
            return;
        }
        if (!new File(filename).exists()) {
            // Looks like the image is not present in the filesystem (i.e. remote URL)
            // TODO: Handle image downloads
            mImage.setImageResource(R.drawable.checkmark);
        } else if (isImage()) {
            // Image thumbnail
            ImageUtil.displayImage(mImage, filename);
        } else {
            // Video thumbnail
            mImage.setImageBitmap(ThumbnailUtils.createVideoThumbnail(
                    filename, MediaStore.Video.Thumbnails.MINI_KIND));
        }
        mImage.setVisibility(View.VISIBLE);
    }

    private boolean isImage() {
        return ConstantUtil.PHOTO_QUESTION_TYPE.equals(mMediaType);
    }

}
