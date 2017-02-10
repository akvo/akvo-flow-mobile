/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.response.value.Signature;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.serialization.response.value.SignatureValue;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ImageUtil;

public class SignatureQuestionView extends QuestionView implements View.OnFocusChangeListener {
    private EditText mName;
    private ImageView mImage;

    private Signature mSignature;

    public SignatureQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.signature_question_view);

        mSignature = new Signature();

        mName = (EditText)findViewById(R.id.name);
        mImage = (ImageView)findViewById(R.id.image);
        Button signButton = (Button)findViewById(R.id.sign_btn);

        if (isReadOnly()) {
            mName.setEnabled(false);
            signButton.setEnabled(false);
        } else {
            signButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    notifyQuestionListeners(QuestionInteractionEvent.ADD_SIGNATURE_EVENT);
                }
            });
            mName.setOnFocusChangeListener(this);
        }
    }

    /**
     * display the completion icon and install the response in the question
     * object
     */
    @Override
    public void questionComplete(Bundle data) {
        if (data != null) {
            mSignature.setImage(data.getString(ConstantUtil.SIGNATURE_IMAGE));
            captureResponse();
            displayResponse();
        }
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);

        if (getResponse() == null || TextUtils.isEmpty(getResponse().getValue())) {
            return;
        }

        mSignature = SignatureValue.deserialize(getResponse().getValue());
        displayResponse();
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mSignature = new Signature();
        displayResponse();
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        mSignature.setName(mName.getText().toString());
        String value = SignatureValue.serialize(mSignature);
        setResponse(new QuestionResponse(value, ConstantUtil.SIGNATURE_RESPONSE_TYPE,
                getQuestion().getId()));
    }

    private void displayResponse() {
        mName.setText(mSignature.getName());
        if (!TextUtils.isEmpty(mSignature.getImage())) {
            // TODO: Resize image?
            mImage.setImageBitmap(ImageUtil.decodeBase64(mSignature.getImage()));
            mImage.setVisibility(VISIBLE);
        } else {
            mImage.setImageDrawable(null);
            mImage.setVisibility(GONE);
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            captureResponse();
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
