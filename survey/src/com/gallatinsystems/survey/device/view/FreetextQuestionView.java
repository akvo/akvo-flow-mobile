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

package com.gallatinsystems.survey.device.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.domain.Question;
import com.gallatinsystems.survey.device.domain.QuestionResponse;
import com.gallatinsystems.survey.device.domain.ValidationRule;
import com.gallatinsystems.survey.device.exception.ValidationException;
import com.gallatinsystems.survey.device.util.ConstantUtil;

/**
 * Question that supports free-text input via the keyboard
 * 
 * @author Christopher Fagiani
 */
public class FreetextQuestionView extends QuestionView implements
        OnFocusChangeListener {
    private boolean isDoubleEntry;
    
    private EditText freetextEdit;
    private EditText doubleEntryEdit;
    
    /**
     * Tag to recognize doubleEntryEdit in the focus callback
     */
    private Object doubleEntryTag = new Object();

    public FreetextQuestionView(Context context, Question q,
            String defaultLang, String[] langCodes, boolean readOnly) {
        super(context, q, defaultLang, langCodes, readOnly);
        this.isDoubleEntry = q.isDoubleEntry();
        init();
    }

    protected void init() {
        Context context = getContext();
        TableRow tr = new TableRow(context);
        freetextEdit = createQuestionView();
        tr.addView(freetextEdit);
        addView(tr);
        
        if (isDoubleEntry) {
            TableRow repeatTitle = new TableRow(context);
            TextView title = new TextView(context);
            title.setWidth(getMaxTextWidth());
            title.setText(R.string.repeat_answer);
            repeatTitle.addView(title);
            addView(repeatTitle);
            
            TableRow tr2 = new TableRow(context);
            doubleEntryEdit = createQuestionView();
            doubleEntryEdit.setTag(doubleEntryTag);
            tr2.addView(doubleEntryEdit);
            addView(tr2);
        }
    }
    
    private EditText createQuestionView() {
        EditText editText = new EditText(getContext());
        editText.setWidth(DEFAULT_WIDTH);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        if (readOnly) {
            editText.setFocusable(false);
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
                editText.setKeyListener(MyDigitKeyListener);
            }
        }
        editText.setOnFocusChangeListener(this);
        editText.setWidth(screenWidth - 50);
        
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(maxLength);
        editText.setFilters(FilterArray);
        
        return editText;
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
        if (resp != null && freetextEdit != null) {
            freetextEdit.setText(resp.getValue());
            if (isDoubleEntry && doubleEntryEdit != null) {
                doubleEntryEdit.setText(resp.getValue());
            }
        }
        super.setResponse(resp);
    }

    /**
     * pulls the data out of the fields and saves it as a response object,
     * possibly suppressing listeners
     */
    public void captureResponse(boolean suppressListeners) {
        String answer = "";
        if (checkDoubleEntry()) {
            answer = freetextEdit.getText().toString();
        }
        setResponse(new QuestionResponse(answer, ConstantUtil.VALUE_RESPONSE_TYPE, getQuestion().getId()),
                suppressListeners);
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        if (resp != null) {
            freetextEdit.setText(resp.getValue());
            if (isDoubleEntry) {
                doubleEntryEdit.setText(resp.getValue());
            }
        }
    }
    
    @Override
    public void resetQuestion(boolean fireEvent) {
        resetQuestion(true, fireEvent);
    }
    
    private void resetQuestion(boolean clearFields, boolean fireEvent) {
        if (clearFields) {
            freetextEdit.setText("");
            if (isDoubleEntry) {
                doubleEntryEdit.setText("");
            }
        }
        super.resetQuestion(fireEvent);
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
                        resetQuestion(true, false); // Enforce validation by clearing
                                              // field
                    }
                } 
                
                if (checkDoubleEntry()) {
                    captureResponse();
                } else if (doubleEntryTag.equals(view.getTag())
                        || (getResponse() != null && !TextUtils.isEmpty(getResponse().getValue()))) {
                    // Warn only if the focus callback is from the second EditText
                    // or we already had a response
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    String message = getContext().getString(R.string.unmatchedwarning)
                            + " " + getQuestion().getText();
                    builder.setTitle(R.string.validationerrtitle)
                            .setMessage(message)
                            .setPositiveButton(R.string.okbutton,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        if (dialog != null) {
                                            dialog.dismiss();
                                        }
                                    }
                                })
                            .show();
                    resetQuestion(false, true);
                }
            }
        }
    }
    
    public boolean checkDoubleEntry() {
        if (!isDoubleEntry) {
            // No double entry required. Return true
            return true;
        }
        String text1 = freetextEdit.getText().toString();
        String text2 = doubleEntryEdit.getText().toString();
        return text1.equals(text2);
    }
    
}
