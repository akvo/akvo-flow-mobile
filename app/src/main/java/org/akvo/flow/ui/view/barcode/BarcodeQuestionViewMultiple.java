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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ViewUtil;

import java.util.ArrayList;

public class BarcodeQuestionViewMultiple extends QuestionView implements
        RemoveButtonListener, ScanButtonListener, BarcodeQuestionInput.AddButtonListener {

    private BarcodeQuestionAdapter barcodeQuestionAdapter;
    private BarcodeQuestionInput questionInput;

    public BarcodeQuestionViewMultiple(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.barcode_question_view_multiple);
        RecyclerView responses = (RecyclerView) findViewById(R.id.responses_recycler_view);
        responses.setLayoutManager(new LinearLayoutManager(getContext()));
        barcodeQuestionAdapter = new BarcodeQuestionAdapter(new ArrayList<String>(), this);
        responses.setAdapter(barcodeQuestionAdapter);
        boolean isQuestionLocked = mQuestion.isLocked();
        if (isQuestionLocked) {
            questionInput = new MultipleLockedBarcodeQuestionInput(getContext());
        } else {
            questionInput = new MultipleBarcodeQuestionInput(getContext());
            questionInput.setAddButtonListener(this);
        }
        addView(questionInput);
        questionInput.setScanButtonListener(this);
        questionInput.initViews();
    }

    @Override
    public void questionComplete(Bundle barcodeData) {
        if (barcodeData != null) {
            String value = barcodeData.getString(ConstantUtil.BARCODE_CONTENT);
            barcodeQuestionAdapter.addBarcode(value);
            captureResponse();
        }
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        String answer = resp != null ? resp.getValue() : null;
        if (!TextUtils.isEmpty(answer)) {
            String[] values = answer.split("\\|", -1);
            barcodeQuestionAdapter.addBarcodes(values);
        }
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        barcodeQuestionAdapter.removeBarcodes();
        questionInput.setBarcodeText("");
    }

    /**
     * pulls the data out of the fields and saves it as a response object,
     * possibly suppressing listeners
     */
    @Override
    public void captureResponse(boolean suppressListeners) {
        String value = barcodeQuestionAdapter.getBarcodes();
        QuestionResponse questionResponse = new QuestionResponse.QuestionResponseBuilder()
                .setValue(value)
                .setType(ConstantUtil.VALUE_RESPONSE_TYPE)
                .setQuestionId(getQuestion().getId())
                .createQuestionResponse();
        setResponse(questionResponse, suppressListeners);
    }

    @Override
    public void onQuestionAddTap(String text) {
        barcodeQuestionAdapter.addBarcode(text);
        questionInput.setBarcodeText("");
        captureResponse();
    }

    @Override
    public void onQuestionRemoveTap(final int position) {
        ViewUtil.showConfirmDialog(R.string.deleteresponse, R.string.clear_value_msg,
                getContext(), true,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        barcodeQuestionAdapter.removeBarcode(position);
                        captureResponse();
                    }
                });

    }

    @Override
    public void onScanBarcodeTap() {
        notifyQuestionListeners(QuestionInteractionEvent.SCAN_BARCODE_EVENT);
    }
}
