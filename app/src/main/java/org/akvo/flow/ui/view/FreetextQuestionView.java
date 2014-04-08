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

import android.content.Context;
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
    private EditText mDoubleEntryText;
    private TextView mDoubleEntryTitle;

    public FreetextQuestionView(Context context, Question q, String defaultLang,
            String[] langCodes, boolean readOnly) {
        super(context, q, defaultLang, langCodes, readOnly);
        init();
    }

    private void init() {
        setQuestionView(R.layout.freetext_question_view);

        mEditText = (EditText)findViewById(R.id.input_et);
        mDoubleEntryText = (EditText)findViewById(R.id.double_entry_et);
        mDoubleEntryTitle = (TextView)findViewById(R.id.double_entry_title);

        // Show/Hide double entry title & EditText
        mDoubleEntryTitle.setVisibility(isDoubleEntry() ? VISIBLE : GONE);
        mDoubleEntryText.setVisibility(isDoubleEntry() ? VISIBLE : GONE);

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
                mDoubleEntryText.setKeyListener(MyDigitKeyListener);
            }
        }

        InputFilter[] filters = { new InputFilter.LengthFilter(maxLength) };

        mEditText.setFilters(filters);
        mDoubleEntryText.setFilters(filters);

        mEditText.setOnFocusChangeListener(this);
        mDoubleEntryText.setOnFocusChangeListener(this);
    }

    @Override
    public void setResponse(QuestionResponse resp) {
        if (resp != null) {
            mEditText.setText(resp.getValue());
            if (isDoubleEntry()) {
                mDoubleEntryText.setText(resp.getValue());
            }
        }
        super.setResponse(resp);
    }

    /**
     * pulls the data out of the fields and saves it as a response object,
     * possibly suppressing listeners
     */
    @Override
    public void captureResponse(boolean suppressListeners) {
        ValidationRule rule = getQuestion().getValidationRule();
        try {
            validateText(rule);
        } catch (ValidationException e) {
            // if we failed validation, display a message to the user
            String error;
            if (ValidationException.TOO_LARGE.equals(e.getType())) {
                error = getResources().getString(R.string.toolargeerr) + rule.getMaxValString();
            } else if (ValidationException.TOO_SMALL.equals(e.getType())) {
                error = getResources().getString(R.string.toosmallerr) + rule.getMinValString();
            } else {
                error = getResources().getString(R.string.baddatatypeerr);
            }
            setError(error);
            return;// Die early. Don't store the value.
        }

        if (!checkDoubleEntry()) {
            setError(getResources().getString(R.string.error_answer_match));
            return;// Die early. Don't store the value.
        }

        if (TextUtils.isEmpty(mEditText.getText().toString()) && getQuestion().isMandatory()) {
            // Mandatory question must be answered
            setError(getResources().getString(R.string.error_question_mandatory));
        } else {
            setError(null);
        }

        setResponse(new QuestionResponse(mEditText.getText().toString(),
                ConstantUtil.VALUE_RESPONSE_TYPE, getQuestion().getId()),
                suppressListeners);
    }

    private boolean checkDoubleEntry() {
        if (!isDoubleEntry()) {
            return true;// No double entry required. Return true;
        }
        String text1 = mEditText.getText().toString();
        String text2 = mDoubleEntryText.getText().toString();
        return text1.equals(text2);
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        if (resp != null) {
            mEditText.setText(resp.getValue());
            if (isDoubleEntry()) {
                mDoubleEntryText.setText(resp.getValue());
            }
        }
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mEditText.setText("");
        if (isDoubleEntry()) {
            mDoubleEntryText.setText("");
        }
    }

    @Override
    public void displayError(String error) {
        // Display the error within the EditText (instead of question text)
        mEditText.setError(error);
        if (isDoubleEntry()) {
            mDoubleEntryText.setError(error);
        }
    }

    private void validateText(ValidationRule rule) throws ValidationException {
        if (rule != null) {
            String validatedText = rule.performValidation(mEditText.getText().toString());
            mEditText.setText(validatedText);
            if (isDoubleEntry()) {
                validatedText = rule.performValidation(mDoubleEntryText.getText().toString());
                mDoubleEntryText.setText(validatedText);
            }
        }
    }

    /**
     * captures the response and runs validation on loss of focus
     */
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        // we need to listen to loss of focus and make sure input is valid
        if (!hasFocus) {
            if (isDoubleEntry() && view.getId() == R.id.input_et &&
                    TextUtils.isEmpty(mDoubleEntryText.getText().toString())) {
                // On double entry questions, do not attempt to capture the response if:
                // 1) the focus lost is happening in the first field and,
                // 2) second field contains no answer yet
                return;
            }
            captureResponse();
        }
    }
    
}
