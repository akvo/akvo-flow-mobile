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
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.util.ConstantUtil;

/**
 * Question to handle scanning of a barcode. This question relies on the zxing
 * library being installed on the device.
 * 
 * @author Christopher Fagiani
 */
public class BarcodeQuestionView extends QuestionView implements OnClickListener,
        OnFocusChangeListener {
    private Button mBarcodeButton;
    private EditText mBarcodeText;

    public BarcodeQuestionView(Context context, Question q, String defaultLanguage,
            String[] langCodes, boolean readOnly) {
        super(context, q, defaultLanguage, langCodes, readOnly);
    }

    @Override
    protected void init() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.barcode_question_view, this, true);

        mBarcodeButton = (Button)findViewById(R.id.scan_btn);
        mBarcodeText = (EditText)findViewById(R.id.barcode_et);

        mBarcodeText.setOnFocusChangeListener(this);
        mBarcodeButton.setOnClickListener(this);

        if (mReadOnly) {
            mBarcodeButton.setEnabled(false);
            mBarcodeText.setEnabled(false);
        }
        // Barcode scanning crashes API 7 app, at least on Emulator
        // ECLAIR_MR1 has code 7, but as we build against 6, it does not know
        // this name yet
        if (Build.VERSION.SDK_INT <= 7) {
            // Maybe change button text as well?
            mBarcodeButton.setEnabled(false);
        }
    }

    /**
     * handle the action button click
     */
    public void onClick(View v) {
        notifyQuestionListeners(QuestionInteractionEvent.SCAN_BARCODE_EVENT);
    }

    @Override
    public void questionComplete(Bundle barcodeData) {
        if (barcodeData != null) {
            mBarcodeText.setText(barcodeData.getString(ConstantUtil.BARCODE_CONTENT));
            setResponse(new QuestionResponse(
                    barcodeData.getString(ConstantUtil.BARCODE_CONTENT),
                    ConstantUtil.VALUE_RESPONSE_TYPE, getQuestion().getId()));
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
            mBarcodeText.setText(resp.getValue());
        }
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mBarcodeText.setText("");
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
        setResponse(new QuestionResponse(mBarcodeText.getText().toString(),
                ConstantUtil.VALUE_RESPONSE_TYPE, getQuestion().getId()),
                suppressListeners);
    }

}
