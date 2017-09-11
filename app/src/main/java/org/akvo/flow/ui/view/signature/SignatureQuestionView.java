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

package org.akvo.flow.ui.view.signature;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.response.value.Signature;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.serialization.response.value.SignatureValue;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ImageUtil;
import org.akvo.flow.util.MediaFileHelper;
import org.akvo.flow.util.image.GlideImageLoader;
import org.akvo.flow.util.image.ImageLoader;
import org.akvo.flow.util.image.ImageLoaderListener;

import static org.akvo.flow.util.MediaFileHelper.RESIZED_SUFFIX;

public class SignatureQuestionView extends QuestionView {

    private EditText mName;
    private ImageView mImage;
    private Button signButton;
    private TextView nameLabel;

    private Signature mSignature;
    private ImageLoader imageLoader;
    private MediaFileHelper mediaFileHelper;

    public SignatureQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.signature_question_view);

        mSignature = new Signature();

        mName = (EditText)findViewById(R.id.name);
        nameLabel = (TextView)findViewById(R.id.name_label);
        mImage = (ImageView)findViewById(R.id.image);
        signButton = (Button)findViewById(R.id.sign_btn);
        Context context = getContext();
        imageLoader = new GlideImageLoader(context);
        mediaFileHelper = new MediaFileHelper(context);

        if (isReadOnly()) {
            signButton.setEnabled(false);
        } else {
            signButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = mSignature.getName();
                    Bundle data = new Bundle();
                    data.putString(ConstantUtil.SIGNATURE_NAME_EXTRA, name);
                    data.putString(ConstantUtil.SIGNATURE_QUESTION_ID_EXTRA, mQuestion.getId());
                    data.putString(ConstantUtil.SIGNATURE_DATAPOINT_ID_EXTRA,
                            mSurveyListener.getDatapointId());
                    notifyQuestionListeners(QuestionInteractionEvent.ADD_SIGNATURE_EVENT, data);
                }
            });
        }
    }

    @Override
    public void questionComplete(Bundle data) {
        if (data != null) {
            final String name = data.getString(ConstantUtil.SIGNATURE_NAME_EXTRA);
            mSignature.setName(name);
            imageLoader.loadFromFile(mediaFileHelper.getImageFile(RESIZED_SUFFIX, mQuestion.getId(),
                    mSurveyListener.getDatapointId()), new ImageLoaderListener() {
                @Override
                public void onImageReady(@Nullable Bitmap bitmap) {
                    displayResponse(name, bitmap);
                    if (bitmap != null) {
                        mSignature.setImage(ImageUtil.encodeBase64(bitmap));
                    }
                    captureResponse();
                }
            });
        }
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);

        if (getResponse() == null || TextUtils.isEmpty(getResponse().getValue())) {
            return;
        }

        mSignature = SignatureValue.deserialize(getResponse().getValue());
        String name = mSignature == null ? "" : mSignature.getName();
        String base64ImageString = mSignature == null ? "" : mSignature.getImage();
        if (!TextUtils.isEmpty(base64ImageString)) {
            setUpName(name);
            imageLoader.loadFromBase64String(base64ImageString, new ImageLoaderListener() {
                @Override
                public void onImageReady(@Nullable Bitmap bitmap) {
                    setUpImage(bitmap);
                    updateSignButton();
                }
            });
        } else {
            displayResponse(name, null);
        }

    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mSignature = new Signature();
        displayResponse("", null);
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        String value = SignatureValue.serialize(mSignature);
        QuestionResponse questionResponse = new QuestionResponse.QuestionResponseBuilder()
                .setValue(value)
                .setType(ConstantUtil.SIGNATURE_RESPONSE_TYPE)
                .setQuestionId(getQuestion().getId())
                .createQuestionResponse();
        setResponse(questionResponse);
    }

    private void displayResponse(String name, Bitmap imageBitmap) {
        setUpName(name);
        setUpImage(imageBitmap);
        updateSignButton();
    }

    private void updateSignButton() {
        if (!TextUtils.isEmpty(mName.getText().toString()) && mImage.getVisibility() == VISIBLE) {
            signButton.setText(R.string.modify_signature);
        } else {
            signButton.setText(R.string.add_signature);
        }
    }

    private void setUpImage(Bitmap imageBitmap) {
        if (imageBitmap != null) {
            mImage.setImageBitmap(imageBitmap);
            mImage.setVisibility(VISIBLE);
        } else {
            mImage.setImageDrawable(null);
            mImage.setVisibility(GONE);
        }
    }

    private void setUpName(String name) {
        if (!TextUtils.isEmpty(name)) {
            mName.setText(name);
            mName.setVisibility(VISIBLE);
            nameLabel.setVisibility(VISIBLE);
        } else {
            mName.setVisibility(GONE);
            nameLabel.setVisibility(GONE);
        }
    }

    @Override
    public boolean isValid() {
        if (!super.isValid() || !mSignature.isValid()) {
            setError(getResources().getString(R.string.error_question_mandatory));
            return false;
        }
        return true;
    }
}
