/*
 *  Copyright (C) 2010-2014 Stichting Akvo (Akvo Foundation)
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.ValidationRule;
import org.akvo.flow.exception.ValidationException;
import org.akvo.flow.util.ConstantUtil;

/**
 * Question that supports free-text input via the keyboard
 * 
 * @author Christopher Fagiani
 */
public class FreetextQuestionView extends QuestionView implements OnFocusChangeListener {
    private EditText mEditText;

    public FreetextQuestionView(Context context, Question q, String defaultLang,
            String[] langCodes, boolean readOnly) {
        super(context, q, defaultLang, langCodes, readOnly);
        init();
    }

    private void init() {
        setQuestionView(R.layout.freetext_question_view);

        mEditText = (EditText)findViewById(R.id.input_et);

        if (mReadOnly) {
            mEditText.setFocusable(false);
        }
        
        int maxLength = ValidationRule.DEFAULT_MAX_LENGTH;
        ValidationRule rule = getQuestion().getValidationRule();
        if (rule != null) {
            // set the maximum length
            if (rule.getMaxLength() != null) {
                maxLength = Math.min(rule.getMaxLength(), ValidationRule.DEFAULT_MAX_LENGTH);
            }
            // if the type is numeric, add numeric-specific rules
            if (ConstantUtil.NUMERIC_VALIDATION_TYPE.equalsIgnoreCase(rule.getValidationType())) {
                DigitsKeyListener MyDigitKeyListener = new DigitsKeyListener(
                        rule.getAllowSigned(), rule.getAllowDecimal());
                mEditText.setKeyListener(MyDigitKeyListener);
            }
        }
        
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(maxLength);
        mEditText.setFilters(FilterArray);
        
        mEditText.setOnFocusChangeListener(this);
    }

    @Override
    public void setResponse(QuestionResponse resp) {
        if (resp != null) {
            mEditText.setText(resp.getValue());
        }
        super.setResponse(resp);
    }

    /**
     * pulls the data out of the fields and saves it as a response object,
     * possibly suppressing listeners
     */
    @Override
    public void captureResponse(boolean suppressListeners) {
        if (validateText()) {
            setResponse(new QuestionResponse(mEditText.getText().toString(),
                    ConstantUtil.VALUE_RESPONSE_TYPE, getQuestion().getId()),
                    suppressListeners);
            setIsValid(true);
        } else {
            setIsValid(false);
        }
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        if (resp != null) {
            mEditText.setText(resp.getValue());
        }
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mEditText.setText("");
    }

    private boolean validateText() {
        ValidationRule currentRule = getQuestion().getValidationRule();
        final String answer = mEditText.getText().toString();
        if (!TextUtils.isEmpty(answer) && currentRule != null) {
            try {
                String validatedText = currentRule.performValidation(answer);
                mEditText.setText(validatedText);
            } catch (ValidationException e) {
                // if we failed validation, display a message to the user
                String error;
                if (ValidationException.TOO_LARGE.equals(e.getType())) {
                    error = getResources().getString(R.string.toolargeerr)
                            + currentRule.getMaxValString();
                } else if (ValidationException.TOO_SMALL.equals(e.getType())) {
                    error = getResources().getString(R.string.toosmallerr)
                            + currentRule.getMinValString();
                } else {
                    error = getResources().getString(R.string.baddatatypeerr);
                }
                mEditText.setError(error);
                return false;
            }
        }
        mEditText.setError(null);
        return true;
    }

    /**
     * captures the response and runs validation on loss of focus
     */
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        // we need to listen to loss of focus and make sure input is valid
        if (!hasFocus) {
            captureResponse();
        }
    }
    
}
