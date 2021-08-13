/*
 *  Copyright (C) 2010-2019 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.ui.view;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.Nullable;

import org.akvo.flow.R;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.utils.entity.Question;
import org.akvo.flow.utils.entity.ValidationException;
import org.akvo.flow.utils.entity.ValidationRule;

/**
 * Question that supports free-text input via the keyboard
 * 
 * @author Christopher Fagiani
 */
public class FreetextQuestionView extends QuestionView {

    private EditText mEditText, mDoubleEntryText;

    private boolean mCaptureResponse;

    public FreetextQuestionView(Context context, Question q, SurveyListener surveyListener, int repetition) {
        super(context, q, surveyListener, repetition);
        init();
    }

    private void init() {
        setQuestionView(R.layout.freetext_question_view);
        mEditText = findViewById(R.id.input_et);
        mDoubleEntryText = findViewById(R.id.double_entry_et);

        // Show/Hide double entry title & EditText
        if (isDoubleEntry()) {
            findViewById(R.id.double_entry_title).setVisibility(VISIBLE);
            mDoubleEntryText.setVisibility(VISIBLE);
            mEditText.setInputType(mEditText.getInputType() & ~InputType.TYPE_TEXT_FLAG_MULTI_LINE);
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

        // ResponseListener will handle both onFocusChange and TextChanged
        mCaptureResponse = true;
        ResponseListener inputListener = new ResponseListener(mEditText);
        ResponseListener extraListener = new ResponseListener(mDoubleEntryText);
        mEditText.addTextChangedListener(inputListener);
        mEditText.setOnFocusChangeListener(inputListener);
        mDoubleEntryText.addTextChangedListener(extraListener);
        mDoubleEntryText.setOnFocusChangeListener(extraListener);
    }

    @Override
    public void setResponse(QuestionResponse resp) {
        if (resp != null) {
            mCaptureResponse = false;
            mEditText.setText(resp.getValue());
            if (isDoubleEntry()) {
                mDoubleEntryText.setText(resp.getValue());
            }
            mCaptureResponse = true;
        }
        super.setResponse(resp);
    }

    /**
     * pulls the data out of the fields and saves it as a response object,
     * possibly suppressing listeners
     */
    @Override
    public void captureResponse(boolean suppressListeners) {
        Question question = getQuestion();
        ValidationRule rule = question.getValidationRule();
        try {
            if (!TextUtils.isEmpty(mEditText.getText().toString())) {
                // Do not validate void answers
                validateText(rule, mEditText);
                if (isDoubleEntry()) {
                    validateText(rule, mDoubleEntryText);
                }
            }
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

        String value = mEditText.getText().toString();
        setResponse(suppressListeners, question, value, ConstantUtil.VALUE_RESPONSE_TYPE);

        checkMandatory();// Mandatory question must be answered
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
            mCaptureResponse = false;
            mEditText.setText(resp.getValue());
            if (isDoubleEntry()) {
                mDoubleEntryText.setText(resp.getValue());
            }
            mCaptureResponse = true;
        }
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        mCaptureResponse = false;
        mEditText.setText("");
        if (isDoubleEntry()) {
            mDoubleEntryText.setText("");
        }
        mCaptureResponse = true;
        super.resetQuestion(fireEvent);
    }

    /**
     * Display the error within the EditText (instead of question text)
     *
     * @param error Error text
     */
    @Override
    public void displayError(@Nullable String error) {
        if (TextUtils.isEmpty(error)) {
            mEditText.setError(null);
            if (isDoubleEntry()) {
                mDoubleEntryText.setError(null);
            }
        } else {
            SpannableStringBuilder errorSpannable = errorMessageFormatter.getErrorSpannable(error);
            mEditText.setError(errorSpannable);
            if (isDoubleEntry()) {
                mDoubleEntryText.setError(errorSpannable);
            }
        }
    }

    private void validateText(ValidationRule rule, EditText view) throws ValidationException {
        if (rule != null) {
            final String text = view.getText().toString();
            final String validatedText = rule.performValidation(text);
            if (!text.equals(validatedText)) {
                view.setText(validatedText);// This action will trigger captureResponse again
            }
        }
    }

    class ResponseListener implements TextWatcher, OnFocusChangeListener {

        EditText mView;

        ResponseListener(EditText view) {
            mView = view;
        }

        boolean capture() {
            if (!mCaptureResponse) {
                return false;// Explicitly disabled
            }

            if (isDoubleEntry() && mView.getId() == R.id.input_et &&
                    TextUtils.isEmpty(mDoubleEntryText.getText().toString())) {
                // On double entry questions, do not attempt to capture the response if:
                // 1) the focus lost is happening in the first field and,
                // 2) second field contains no answer yet
                return false;
            }
            return true;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (capture()) {
                captureResponse();
            }
        }

        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (!hasFocus && capture()) {
                captureResponse();
            }

            if (!hasFocus) {
                InputMethodManager imm = (InputMethodManager)getContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
    
}
