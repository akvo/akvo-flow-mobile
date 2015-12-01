/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
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
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ImageUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class SignatureQuestionView extends QuestionView implements View.OnFocusChangeListener {
    private static final String TAG = SignatureQuestionView.class.getSimpleName();
    private EditText mName;
    private ImageView mImage;

    private String mResponseName;
    private String mResponseImage;

    public SignatureQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.signature_question_view);

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
            mResponseImage = data.getString(ConstantUtil.SIGNATURE_IMAGE);
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

        try {
            JSONObject jResponse = new JSONObject(getResponse().getValue());
            mResponseName = jResponse.optString(Attr.NAME);
            mResponseImage = jResponse.optString(Attr.IMAGE);
            displayResponse();
        } catch (JSONException e) {
            Log.e(TAG, "Response is not JSON-formatted: " + e.getMessage());
        }
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mResponseName = null;
        mResponseImage = null;
        displayResponse();
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        mResponseName = mName.getText().toString();
        try {
            JSONObject jResponse = new JSONObject();
            jResponse.put(Attr.NAME, mResponseName);
            jResponse.put(Attr.IMAGE, mResponseImage);

            setResponse(new QuestionResponse(jResponse.toString(), ConstantUtil.SIGNATURE_RESPONSE_TYPE,
                    getQuestion().getId()));
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void displayResponse() {
        mName.setText(mResponseName);
        if (!TextUtils.isEmpty(mResponseImage)) {
            // TODO: Resize image?
            mImage.setImageBitmap(ImageUtil.decodeBase64(mResponseImage));
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
        boolean valid = super.isValid();
        if (valid && getResponse() != null) {
            valid = !TextUtils.isEmpty(mResponseName) && !TextUtils.isEmpty(mResponseImage);
        }
        if (!valid) {
            setError(getResources().getString(R.string.error_question_mandatory));
        }
        return valid;
    }

    interface Attr {
        String NAME = "name";
        String IMAGE = "image";
    }

}
