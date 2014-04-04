/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.util.ConstantUtil;

/**
 * Question type that supports taking a picture/video/audio recording with the
 * device's on-board camera.
 * 
 * @author Christopher Fagiani
 */
public class MediaQuestionView extends QuestionView implements OnClickListener {
    private Button mMediaButton;
    private ImageView mCompleteIcon;
    private String mMediaType;

    public MediaQuestionView(Context context, Question q, String type, String defaultLang,
            String[] langCodes, boolean readOnly) {
        super(context, q, defaultLang, langCodes, readOnly);
        mMediaType = type;
        init();
    }

    private void init() {
        setQuestionView(R.layout.media_question_view);

        mMediaButton = (Button)findViewById(R.id.media_btn);
        mCompleteIcon = (ImageView)findViewById(R.id.completed_iv);

        if (ConstantUtil.PHOTO_QUESTION_TYPE.equals(mMediaType)) {
            mMediaButton.setText(R.string.takephoto);
        } else {
            mMediaButton.setText(R.string.takevideo);
        }
        mMediaButton.setOnClickListener(this);
        if (mReadOnly) {
            mMediaButton.setEnabled(false);
        }

        mCompleteIcon.setOnClickListener(this);
        mCompleteIcon.setVisibility(View.INVISIBLE);
    }

    /**
     * handle the action button click
     */
    public void onClick(View v) {
        if (v == mCompleteIcon && ConstantUtil.PHOTO_QUESTION_TYPE.equals(mMediaType)) {
            Dialog dia = new Dialog(getContext());
            ImageView imageView = new ImageView(getContext());
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap bm = BitmapFactory.decodeFile(getResponse().getValue(),
                    options);
            imageView.setImageBitmap(bm);
            dia.setContentView(imageView, new LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
            dia.show();
        } else if (v == mMediaButton) {
            if (ConstantUtil.PHOTO_QUESTION_TYPE.equals(mMediaType)) {
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
            mCompleteIcon.setVisibility(View.VISIBLE);
            setResponse(new QuestionResponse(
                    mediaData.getString(ConstantUtil.MEDIA_FILE_KEY),
                    ConstantUtil.PHOTO_QUESTION_TYPE.equals(mMediaType) ?
                            ConstantUtil.IMAGE_RESPONSE_TYPE
                            : ConstantUtil.VIDEO_RESPONSE_TYPE,
                    getQuestion().getId()));
        }
    }

    /**
     * restores the file path for the file and turns on the complete icon if the
     * file exists
     */
    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        if (resp != null && resp.getValue() != null) {
            mCompleteIcon.setVisibility(View.VISIBLE);
        }
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mCompleteIcon.setVisibility(View.INVISIBLE);
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
    }

}
