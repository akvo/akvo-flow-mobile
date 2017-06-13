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

package org.akvo.flow.ui.view.barcode;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.ui.view.ResponseInputWatcher;
import org.akvo.flow.util.ConstantUtil;

public class BarcodeQuestionViewSingle extends QuestionView implements
        ScanButton.ScanButtonListener {

    private EditText mInputText;

    public BarcodeQuestionViewSingle(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.barcode_question_view_single);

        mInputText = (EditText) findViewById(R.id.input_text);

        ScanButton mScanBtn = (ScanButton) findViewById(R.id.scan_btn);
        mScanBtn.setEnabled(!isReadOnly());
        mScanBtn.setListener(this);

        boolean isQuestionLocked = mQuestion.isLocked();

        if (!isQuestionLocked) {
            View manualInputSeparator = findViewById(R.id.manual_input_separator);
            manualInputSeparator.setVisibility(VISIBLE);
            mInputText.setVisibility(VISIBLE);
            setUpTextWatcher();
        }
        boolean enableTextInput = !isQuestionLocked && !isReadOnly();
        mInputText.setEnabled(enableTextInput);
    }

    private void setUpTextWatcher() {
        boolean isReadOnly = isReadOnly();
        if (!isReadOnly) {
            mInputText.addTextChangedListener(new ResponseInputWatcher(this));
        }
    }

    @Override
    public void questionComplete(Bundle barcodeData) {
        if (barcodeData != null) {
            String value = barcodeData.getString(ConstantUtil.BARCODE_CONTENT);
            mInputText.setText(value);
            captureResponse();
        }
    }

    /**
     * restores the data and turns on the complete icon if the content is
     * non-null
     */
    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        String answer = resp != null ? resp.getValue() : null;
        if (!TextUtils.isEmpty(answer)) {
            mInputText.setText(answer);
        } else {
            mInputText.setText("");
        }
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mInputText.setText("");
    }

    /**
     * pulls the data out of the fields and saves it as a response object,
     * possibly suppressing listeners
     */
    public void captureResponse(boolean suppressListeners) {
        StringBuilder builder = new StringBuilder();
        String value = mInputText.getText().toString();
        if (!TextUtils.isEmpty(value)) {
            builder.append(value);
        }
        setResponse(new QuestionResponse(builder.toString(), ConstantUtil.VALUE_RESPONSE_TYPE,
                        getQuestion().getId()),
                suppressListeners);
    }

    @Override
    public void onScanBarcodeTap() {
        notifyQuestionListeners(QuestionInteractionEvent.SCAN_BARCODE_EVENT);
    }
}
