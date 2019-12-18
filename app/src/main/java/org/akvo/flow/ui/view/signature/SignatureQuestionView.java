/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.response.value.Signature;
import org.akvo.flow.domain.util.ImageSize;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.serialization.response.value.SignatureValue;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ImageUtil;
import org.akvo.flow.util.files.SignatureFileBrowser;
import org.akvo.flow.util.image.GlideImageLoader;
import org.akvo.flow.util.image.ImageLoader;

import java.io.File;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;

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

    public SignatureQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.signature_question_view);
        initialiseInjector();

        mSignature = new Signature();

        mName = findViewById(R.id.signature_name);
        nameLabel = findViewById(R.id.signature_name_label);
        mImage = findViewById(R.id.signature_image);
        signButton = findViewById(R.id.sign_btn);
        imageLoader = new GlideImageLoader((Activity) getContext());

        if (isReadOnly()) {
            signButton.setVisibility(GONE);
        } else {
            signButton.setOnClickListener(v -> {
                String name = mSignature.getName();
                Bundle data = new Bundle();
                data.putString(ConstantUtil.SIGNATURE_NAME_EXTRA, name);
                data.putString(ConstantUtil.SIGNATURE_QUESTION_ID_EXTRA, mQuestion.getId());
                data.putString(ConstantUtil.SIGNATURE_DATAPOINT_ID_EXTRA,
                        mSurveyListener.getDataPointId());
                notifyQuestionListeners(QuestionInteractionEvent.ADD_SIGNATURE_EVENT, data);
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
    public void onQuestionResultReceived(Bundle data) {
        if (data != null) {
            final String name = data.getString(ConstantUtil.SIGNATURE_NAME_EXTRA);
            mSignature.setName(name);
            setUpName(name);
            File imageFile = signatureFileBrowser
                    .getSignatureImageFile(RESIZED_SUFFIX, mQuestion.getId(),
                            mSurveyListener.getDataPointId());
            imageLoader.loadFromFile(mImage, imageFile,
                    bitmap -> ((AppCompatActivity) getContext()).runOnUiThread(() -> {
                        mImage.setVisibility(VISIBLE);
                        updateSignButton();
                        if (bitmap != null) {
                            mSignature.setImage(ImageUtil.encodeBase64(bitmap));
                        }
                        captureResponse();
                    }), new ImageSize(320, 240));
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
            mImage.setVisibility(VISIBLE);
            imageLoader
                    .loadFromBase64String(base64ImageString, mImage, bitmap -> updateSignButton());
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
        setResponse(getQuestion(), value, ConstantUtil.SIGNATURE_RESPONSE_TYPE);
    }

    private void resetResponse(String name) {
        setUpName(name);
        mImage.setImageDrawable(null);
        mImage.setVisibility(GONE);
        updateSignButton();
    }

    private void updateSignButton() {
        if (!TextUtils.isEmpty(mName.getText().toString()) && mImage.getVisibility() == VISIBLE) {
            signButton.setText(R.string.modify_signature);
        } else {
            signButton.setText(R.string.add_signature);
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
