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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;

public class CaddisflyQuestionView extends QuestionView implements View.OnClickListener {
    private EditText mEditText;

    public CaddisflyQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.caddisfly_question_view);

        mEditText = (EditText)findViewById(R.id.input_et);

        Button externalSourceBtn = (Button)findViewById(R.id.button);
        externalSourceBtn.setOnClickListener(this);
        externalSourceBtn.setEnabled(!mSurveyListener.isReadOnly());
    }

    @Override
    public void setResponse(QuestionResponse resp) {
        String value = resp != null ? resp.getValue() : null;
        mEditText.setText(value);
        super.setResponse(resp);
    }

    /**
     * pulls the data out of the fields and saves it as a response object,
     * possibly suppressing listeners
     */
    @Override
    public void captureResponse(boolean suppressListeners) {
        setResponse(new QuestionResponse(mEditText.getText().toString(),
                ConstantUtil.CADDISFLY_RESPONSE_TYPE, getQuestion().getId()),
                suppressListeners);

        checkMandatory();// Mandatory question must be answered
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        String val = resp != null ? resp.getValue() : null;
        mEditText.setText(val);
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        mEditText.setText("");
        super.resetQuestion(fireEvent);
    }

    @Override
    public void displayError(String error) {
        // Display the error within the EditText (instead of question text)
        mEditText.setError(error);
    }

    @Override
    public void questionComplete(Bundle data) {
        if (data != null) {
            String value = data.getString(ConstantUtil.EXTERNAL_SOURCE_RESPONSE);
            mEditText.setText(value);
        }
        captureResponse();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button) {
            notifyQuestionListeners(QuestionInteractionEvent.EXTERNAL_SOURCE_EVENT);
        }
    }

}
