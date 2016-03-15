/*
 *  Copyright (C) 2016 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;

import java.io.File;

public class CaddisflyQuestionView extends QuestionView implements View.OnClickListener {
    private static final String TAG = CaddisflyQuestionView.class.getSimpleName();
    private Button mButton;
    private View mResponseView;
    private String mValue;
    private String mImage;

    public CaddisflyQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.caddisfly_question_view);
        mResponseView = findViewById(R.id.response_view);
        mButton = (Button)findViewById(R.id.button);
        mButton.setOnClickListener(this);
        displayResponseView();
    }

    private void displayResponseView() {
        mResponseView.setVisibility(TextUtils.isEmpty(mValue) ? GONE : VISIBLE);
        mButton.setEnabled(!mSurveyListener.isReadOnly());
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        QuestionResponse r = new QuestionResponse(mValue, ConstantUtil.CADDISFLY_RESPONSE_TYPE,
                getQuestion().getId());
        r.setFilename(mImage);
        setResponse(r);
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        mValue = resp != null ? resp.getValue() : null;
        displayResponseView();
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mValue = null;
        displayResponseView();
    }

    @Override
    public void questionComplete(Bundle data) {
        if (data != null) {
            mValue = data.getString(ConstantUtil.CADDISFLY_RESPONSE);

            // Get optional image and store it as part of the response
            String image = data.getString(ConstantUtil.CADDISFLY_IMAGE);

            Log.d(TAG, "caddisflyTestComplete - Response: " + mValue + ". Image: " + image);

            File src = !TextUtils.isEmpty(image) ? new File(image) : null;
            if (src != null && src.exists()) {
                // Move the image into the FLOW directory
                File dst = new File(FileUtil.getFilesDir(FileUtil.FileType.MEDIA), src.getName());

                if (!src.renameTo(dst)) {
                    Log.e(TAG, String.format("Could not move file %s to %s",
                            src.getAbsoluteFile(), dst.getAbsoluteFile()));
                } else {
                    mImage = dst.getAbsolutePath();
                }
            }

            displayResponseView();
        }
        captureResponse();
    }

    @Override
    public void onClick(View view) {
        Question q = getQuestion();
        Bundle data = new Bundle();
        data.putString(ConstantUtil.CADDISFLY_RESOURCE_ID, q.getCaddisflyRes());
        data.putString(ConstantUtil.CADDISFLY_QUESTION_ID, q.getId());
        data.putString(ConstantUtil.CADDISFLY_QUESTION_TITLE, q.getText());
        data.putString(ConstantUtil.CADDISFLY_DATAPOINT_ID, mSurveyListener.getDatapointId());
        data.putString(ConstantUtil.CADDISFLY_FORM_ID, mSurveyListener.getFormId());
        data.putString(ConstantUtil.CADDISFLY_LANGUAGE, FlowApp.getApp().getAppLanguageCode());
        notifyQuestionListeners(QuestionInteractionEvent.CADDISFLY, data);
    }

}
