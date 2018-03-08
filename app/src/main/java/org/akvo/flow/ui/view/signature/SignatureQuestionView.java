/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.serialization.response.value.SignatureValue;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ImageUtil;
import org.akvo.flow.util.files.SignatureFileBrowser;
import org.akvo.flow.util.image.ImageLoader;
import org.akvo.flow.util.image.ImageLoaderListener;
import org.akvo.flow.util.image.ImageTarget;
import org.akvo.flow.util.image.PicassoImageLoader;
import org.akvo.flow.util.image.PicassoImageTarget;

import java.io.File;

import javax.inject.Inject;

import timber.log.Timber;

import static org.akvo.flow.util.files.SignatureFileBrowser.RESIZED_SUFFIX;

public class SignatureQuestionView extends QuestionView {

    @Inject
    SignatureFileBrowser signatureFileBrowser;

    private EditText mName;
    private ImageView mImage;
    private Button signButton;
    private TextView nameLabel;
    private ImageLoader imageLoader;
    private Signature mSignature;

    private final ImageTarget imageTarget = new PicassoImageTarget() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap) {
            setUpImage(bitmap);
            updateSignButton();
            if (bitmap != null) {
                mSignature.setImage(ImageUtil.encodeBase64(bitmap));
            }
            captureResponse();
        }
    };

    public SignatureQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.signature_question_view);
        initialiseInjector();

        mSignature = new Signature();

        mName = (EditText) findViewById(R.id.signature_name);
        nameLabel = (TextView) findViewById(R.id.signature_name_label);
        mImage = (ImageView) findViewById(R.id.signature_image);
        signButton = (Button) findViewById(R.id.sign_btn);
        imageLoader = new PicassoImageLoader((Activity) getContext());

        if (isReadOnly()) {
            signButton.setVisibility(GONE);
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

    private void initialiseInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    @Override
    public void questionComplete(Bundle data) {
        if (data != null) {
            final String name = data.getString(ConstantUtil.SIGNATURE_NAME_EXTRA);
            mSignature.setName(name);
            setUpName(name);
            File imageFile = signatureFileBrowser
                    .getSignatureImageFile(RESIZED_SUFFIX, mQuestion.getId(),
                            mSurveyListener.getDatapointId());
            imageLoader.clearImage(imageFile);
            //noinspection unchecked
            imageLoader.loadFromFile(imageFile, imageTarget);
        }
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);

        QuestionResponse response = getResponse();
        String value = response == null ? null : response.getValue();
        if (response == null || TextUtils.isEmpty(value)) {
            return;
        }

        mSignature = SignatureValue.deserialize(value);
        final String name = mSignature == null ? "" : mSignature.getName();
        String base64ImageString = mSignature == null ? "" : mSignature.getImage();
        if (!TextUtils.isEmpty(base64ImageString)) {
            setUpName(name);
            imageLoader.loadFromBase64String(base64ImageString, mImage, new ImageLoaderListener() {
                @Override
                public void onImageReady() {
                    mImage.setVisibility(VISIBLE);
                    updateSignButton();
                }

                @Override
                public void onImageError() {
                    Timber.e("Error loading base64 string as image");
                }
            });
        } else {
            resetResponse(name);
        }

    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mSignature = new Signature();
        resetResponse("");
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

    private void resetResponse(String name) {
        setUpName(name);
        setUpImage(null);
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
