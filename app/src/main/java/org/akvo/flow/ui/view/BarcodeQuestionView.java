/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;

/**
 * Question to handle scanning of a barcode. This question relies on the zxing
 * library being installed on the device.
 * 
 * @author Christopher Fagiani
 */
public class BarcodeQuestionView extends QuestionView implements OnClickListener,
        OnFocusChangeListener {
    //private List<EditText> mInputFields;
    private EditText mInputText;
    private ImageButton mAddBtn;
    private Button mScanBtn;
    private LinearLayout mInputContainer;
    private boolean mMultiple;

    public BarcodeQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.barcode_question_view);

        mMultiple = getQuestion().isAllowMultiple();

        mInputContainer = (LinearLayout)findViewById(R.id.input_ll);
        mScanBtn = (Button)findViewById(R.id.scan_btn);
        mAddBtn = (ImageButton)findViewById(R.id.add_btn);
        mInputText = (EditText)findViewById(R.id.input_text);

        if (!mMultiple || isReadOnly()) {
            mAddBtn.setVisibility(View.GONE);
        }
        mScanBtn.setEnabled(!isReadOnly());
        mInputText.setEnabled(!isReadOnly());

        mInputText.setOnFocusChangeListener(this);
        mScanBtn.setOnClickListener(this);
        mAddBtn.setOnClickListener(this);
    }

    private void addValue(String text) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        final View view = inflater.inflate(R.layout.barcode_item, mInputContainer, false);
        EditText et = (EditText)view.findViewById(R.id.input);
        et.setText(text);
        view.findViewById(R.id.delete).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mInputContainer.removeView(view);
            }
        });
        mInputContainer.addView(view);
    }

    /**
     * handle the action button click
     */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scan_btn:
                notifyQuestionListeners(QuestionInteractionEvent.SCAN_BARCODE_EVENT);
                break;
            case R.id.add_btn:
                final String value = mInputText.getText().toString();
                if (!TextUtils.isEmpty(value)) {
                    addValue(value);
                    mInputText.setText("");
                }
                break;
        }
    }

    @Override
    public void questionComplete(Bundle barcodeData) {
        if (barcodeData != null) {
            String value = barcodeData.getString(ConstantUtil.BARCODE_CONTENT);
            if (mMultiple) {
                addValue(value);
            } else {
                mInputText.setText(value);
            }
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
        if (resp != null && resp.getValue() != null) {
            //mBarcodeText.setText(resp.getValue());
        }
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mInputContainer.removeAllViews();
        mInputText.setText("");
    }

    /**
     * captures the response and runs validation on loss of focus
     */
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        // we need to listen to loss of focus
        // and make sure input is valid
        if (!hasFocus) {
            captureResponse(false);
        }
    }

    /**
     * pulls the data out of the fields and saves it as a response object,
     * possibly suppressing listeners
     */
    public void captureResponse(boolean suppressListeners) {
        /*
        setResponse(new QuestionResponse(mBarcodeText.getText().toString(),
                ConstantUtil.VALUE_RESPONSE_TYPE, getQuestion().getId()),
                suppressListeners);
                */
    }

}
