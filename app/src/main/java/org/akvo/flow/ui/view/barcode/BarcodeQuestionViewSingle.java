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

package org.akvo.flow.ui.view.barcode;

import android.content.Context;
import android.os.Bundle;

import org.akvo.flow.R;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.utils.entity.Question;

public class BarcodeQuestionViewSingle extends QuestionView implements
        ScanButtonListener, BarcodeQuestionInput.BarcodeEditListener {

    private BarcodeQuestionInput questionInput;

    public BarcodeQuestionViewSingle(Context context, Question q, SurveyListener surveyListener, int repetition) {
        super(context, q, surveyListener, repetition);
        init();
    }

    private void init() {
        setQuestionView(R.layout.barcode_question_view_single);
        boolean isQuestionLocked = mQuestion.isLocked();
        if (isQuestionLocked) {
            questionInput = new SingleLockedBarcodeQuestionInput(getContext());
        } else {
            questionInput = new SingleBarcodeQuestionInput(getContext());
            questionInput.setBarcodeEditListener(this);
        }
        addView(questionInput);
        questionInput.setScanButtonListener(this);
        questionInput.initViews();
    }

    @Override
    public void onQuestionResultReceived(Bundle barcodeData) {
        if (barcodeData != null) {
            String value = barcodeData.getString(ConstantUtil.BARCODE_CONTENT);
            questionInput.setBarcodeText(value);
            captureResponse();
        }
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        String answer = resp != null ? resp.getValue() : null;
        questionInput.setBarcodeText(answer);
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        questionInput.setBarcodeText("");
    }

    /**
     * pulls the data out of the fields and saves it as a response object,
     * possibly suppressing listeners
     */
    public void captureResponse(boolean suppressListeners) {
        String value = questionInput.getBarcode();
        setResponse(suppressListeners, getQuestion(), value, ConstantUtil.VALUE_RESPONSE_TYPE);
    }

    @Override
    public void onScanBarcodeTap() {
        notifyQuestionListeners(QuestionInteractionEvent.SCAN_BARCODE_EVENT);
    }

    @Override
    public void onTextEdited(String text) {
        captureResponse();
    }
}
