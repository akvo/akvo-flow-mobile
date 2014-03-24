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
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
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
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.freetext_question_view, this, true);

        setupQuestion();

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
            if (ConstantUtil.NUMERIC_VALIDATION_TYPE.equalsIgnoreCase(rule
                    .getValidationType())) {
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

    /**
     * pulls the data out of the fields and saves it as a response object
     */
    @Override
    public void captureResponse() {
        captureResponse(false);
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
    public void captureResponse(boolean suppressListeners) {
        setResponse(new QuestionResponse(mEditText.getText().toString(),
                ConstantUtil.VALUE_RESPONSE_TYPE, getQuestion().getId()),
                suppressListeners);
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

    /**
     * captures the response and runs validation on loss of focus
     */
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        // we need to listen to loss of focus
        // and make sure input is valid
        if (!hasFocus) {
            ValidationRule currentRule = getQuestion().getValidationRule();
            EditText textEdit = (EditText) view;
            if (textEdit.getText() != null
                    && textEdit.getText().toString().trim().length() > 0) {
                if (currentRule != null) {
                    try {
                        String validatedText = currentRule
                                .performValidation(textEdit.getText()
                                        .toString());
                        textEdit.setText(validatedText);
                        // now capture the response
                        captureResponse();
                    } catch (ValidationException e) {
                        // if we failed validation, display
                        // a message to the user
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                getContext());
                        builder.setTitle(R.string.validationerrtitle);
                        TextView tipText = new TextView(getContext());
                        if (ValidationException.TOO_LARGE.equals(e.getType())) {
                            String baseText = getResources().getString(
                                    R.string.toolargeerr);
                            tipText.setText(baseText
                                    + currentRule.getMaxValString());
                        } else if (ValidationException.TOO_SMALL.equals(e
                                .getType())) {
                            String baseText = getResources().getString(
                                    R.string.toosmallerr);
                            tipText.setText(baseText
                                    + currentRule.getMinValString());
                        } else {
                            tipText.setText(R.string.baddatatypeerr);
                        }
                        builder.setView(tipText);
                        builder.setPositiveButton(R.string.okbutton,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        if (dialog != null) {
                                            dialog.dismiss();
                                        }
                                    }
                                });
                        builder.show();
                        resetQuestion(false); // Enforce validation by clearing
                                              // field
                    }
                } else {
                    captureResponse();
                }
            }
        }
    }
    
}
