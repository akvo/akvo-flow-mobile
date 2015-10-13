/*
 *  Copyright (C) 2010-2015 Stichting Akvo (Akvo Foundation)
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
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import org.akvo.flow.R;
import org.akvo.flow.domain.AltText;
import org.akvo.flow.domain.Option;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ViewUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Question type that supports the selection of a single option from a list of
 * choices (i.e. a radio button group).
 * 
 * @author Christopher Fagiani
 */
public class OptionQuestionView extends QuestionView {
    public static boolean promptOnChange;

    private final String OTHER_TEXT;
    private RadioGroup mOptionGroup;
    private List<CheckBox> mCheckBoxes;
    private Spinner mSpinner;
    private TextView mOtherText;
    private Map<Integer, String> mIdToValueMap;
    private volatile boolean mSuppressListeners = false;
    private String mLatestOtherText;

    public OptionQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        OTHER_TEXT = getResources().getString(R.string.othertext);
        init();
    }

    private void init() {
        // Just inflate the header. Options will be added dynamically
        setQuestionView(R.layout.question_header);

        mIdToValueMap = new HashMap<Integer, String>();
        mSuppressListeners = true;
        if (mQuestion.getOptions() != null) {
            // spinners aren't compatible with multiple selection. if the survey
            // has both allowMultiple=true and renderMode=spinner, that is an
            // error but we'll just honor the allowMultiple
            if (!mQuestion.isAllowMultiple() && ConstantUtil.SPINNER_RENDER_MODE
                    .equalsIgnoreCase(mQuestion.getRenderType())) {
                setupSpinnerType();
            } else if (!mQuestion.isAllowMultiple()) {
                setupRadioType();
            } else {
                setupCheckboxType();
            }

            if (mQuestion.isAllowOther()) {
                mOtherText = new TextView(getContext());
                mOtherText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                addView(mOtherText);
            }
        }
        mSuppressListeners = false;
    }

    private void setupSpinnerType() {
        mSpinner = new Spinner(getContext());
        mSpinner.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        initializeSpinnerOptions();
        // set the selection to the first element
        mSpinner.setSelection(0);

        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @SuppressWarnings("rawtypes")
            public void onItemSelected(AdapterView parent, View view, int position, long id) {
                if (!mSuppressListeners) {
                    //mSpinner.requestFocus();
                    // if position is greater than the size of the array then OTHER is selected
                    if (position > mQuestion.getOptions().size()) {
                        // only display the dialog if OTHER isn't
                        // already populated as the response. need this
                        // to suppress the OTHER dialog
                        if (getResponse() == null || !getResponse().getType().equals(
                                ConstantUtil.OTHER_RESPONSE_TYPE)) {
                            displayOtherDialog();
                        }
                    } else if (position == 0) {
                        if (mOtherText != null) {
                            mOtherText.setText("");
                        }
                        setResponse(new QuestionResponse("", ConstantUtil.VALUE_RESPONSE_TYPE,
                                mQuestion.getId()));
                    } else {
                        if (mOtherText != null) {
                            mOtherText.setText("");
                        }
                        setResponse(new QuestionResponse(mQuestion.getOptions()
                                    .get(position > 0 ? position - 1 : 0)
                                    .getText(),
                                ConstantUtil.VALUE_RESPONSE_TYPE,
                                mQuestion.getId()));
                    }
                }
            }

            @SuppressWarnings("rawtypes")
            public void onNothingSelected(AdapterView parent) {
                if (!mSuppressListeners) {
                    setResponse(new QuestionResponse("", ConstantUtil.VALUE_RESPONSE_TYPE,
                            mQuestion.getId()));
                }
            }
        });
        addView(mSpinner);
        if (isReadOnly()) {
            mSpinner.setEnabled(false);
        }
    }

    private void setupRadioType() {
        mOptionGroup = new RadioGroup(getContext());
        mOptionGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    //mOptionGroup.requestChildFocus(mOptionGroup.findViewById(checkedId), mOptionGroup);
                    handleSelection(checkedId, true);
                }
            });
        for (Option o : mQuestion.getOptions()) {
            RadioButton rb = new RadioButton(getContext());
            rb.setLayoutParams(new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            rb.setLongClickable(true);
            rb.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onClearAnswer();
                    return true;
                }
            });
            rb.setText(formOptionText(o), BufferType.SPANNABLE);
            mOptionGroup.addView(rb);
            mIdToValueMap.put(rb.getId(), o.getText());
        }
        if (mQuestion.isAllowOther()) {
            RadioButton rb = new RadioButton(getContext());
            rb.setLayoutParams(new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            rb.setText(OTHER_TEXT);
            mOptionGroup.addView(rb);
            mIdToValueMap.put(rb.getId(), OTHER_TEXT);
        }
        addView(mOptionGroup);
        if (isReadOnly()) {
            for (int j = 0; j < mOptionGroup.getChildCount(); j++) {
                mOptionGroup.getChildAt(j).setEnabled(false);
            }
        }
    }

    private void setupCheckboxType() {
        mCheckBoxes = new ArrayList<CheckBox>();
        List<Option> options = mQuestion.getOptions();
        for (int i = 0; i < options.size(); i++) {
            CheckBox box = new CheckBox(getContext());
            box.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            box.setId(i);
            mCheckBoxes.add(box);
            box.setText(formOptionText(options.get(i)), BufferType.SPANNABLE);
            box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    handleSelection(buttonView.getId(), isChecked);
                }
            });
            mIdToValueMap.put(box.getId(), options.get(i).getText());
            addView(box);
        }
        if (mQuestion.isAllowOther()) {
            CheckBox box = new CheckBox(getContext());
            box.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            box.setId(options.size());
            box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    handleSelection(buttonView.getId(), isChecked);
                }
            });
            box.setText(OTHER_TEXT);
            mCheckBoxes.add(box);
            mIdToValueMap.put(box.getId(), OTHER_TEXT);
            addView(box);
        }
        if (isReadOnly()) {
            for (int i = 0; i < mCheckBoxes.size(); i++) {
                mCheckBoxes.get(i).setEnabled(false);
            }
        }
    }

    @Override
    public void notifyOptionsChanged() {
        super.notifyOptionsChanged();

        if (ConstantUtil.SPINNER_RENDER_MODE.equalsIgnoreCase(mQuestion.getRenderType())) {
            initializeSpinnerOptions();
            rehydrate(getResponse());
        } else {
            List<Option> options = mQuestion.getOptions();
            if (mQuestion.isAllowMultiple()) {
                for (int i = 0; i < mCheckBoxes.size(); i++) {
                    // make sure we have a corresponding option (i.e. not the OTHER option)
                    if (i < options.size()) {
                        mCheckBoxes.get(i).setText(formOptionText(options.get(i)),
                                BufferType.SPANNABLE);
                    }
                }
            } else {
                for (int i = 0; i < mOptionGroup.getChildCount(); i++) {
                    // make sure we have a corresponding option (i.e. not the OTHER option)
                    if (i < options.size()) {
                        ((RadioButton) (mOptionGroup.getChildAt(i)))
                                .setText(formOptionText(options.get(i)));
                    }
                }
            }
        }
    }

    /**
     * sets the spinner content
     */
    private void initializeSpinnerOptions() {
        int extras = 1;
        if (mQuestion.isAllowOther()) {
            extras++;
        }
        List<Option> options = mQuestion.getOptions();
        Spanned[] optionArray = new Spanned[options.size() + extras];
        optionArray[0] = Html.fromHtml("");
        for (int i = 0; i < options.size(); i++) {
            optionArray[i + 1] = formOptionText(options.get(i));
        }
        // put the "other" option in the last slot in the array
        if (mQuestion.isAllowOther()) {
            optionArray[optionArray.length - 1] = Html.fromHtml(OTHER_TEXT);
        }
        ArrayAdapter<CharSequence> optionAdapter = new ArrayAdapter<CharSequence>(
                getContext(), android.R.layout.simple_spinner_item, optionArray);
        optionAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(optionAdapter);
    }

    /**
     * forms the text for an option based on the visible languages
     * 
     * @param opt
     * @return
     */
    private Spanned formOptionText(Option opt) {
        boolean isFirst = true;
        StringBuilder text = new StringBuilder();
        final String[] langs = getLanguages();
        for (int i = 0; i < langs.length; i++) {
            if (getDefaultLang().equalsIgnoreCase(langs[i])) {
                if (!isFirst) {
                    text.append(" / ");
                } else {
                    isFirst = false;
                }
                text.append(TextUtils.htmlEncode(opt.getText()));

            } else {
                AltText txt = opt.getAltText(langs[i]);
                if (txt != null) {
                    if (!isFirst) {
                        text.append(" / ");
                    } else {
                        isFirst = false;
                    }
                    text.append("<font color='");
                    // spinners have black backgrounds so if the text color is
                    // white, make it black so it shows up
                    if (ConstantUtil.WHITE_COLOR.equalsIgnoreCase(sColors[i])
                            && ConstantUtil.SPINNER_RENDER_MODE
                                    .equalsIgnoreCase(mQuestion.getRenderType())) {
                        text.append(ConstantUtil.BLACK_COLOR);
                    } else {
                        text.append(sColors[i]);
                    }
                    text.append("'>")
                            .append(TextUtils.htmlEncode(txt.getText()))
                            .append("</font>");
                }
            }
        }
        return Html.fromHtml(text.toString());
    }

    /**
     * prompts the user to confirm they want to change the value and, if so,
     * populates the QuestionResponse.
     * 
     * @param checkedId
     * @param isChecked
     */
    private void handleSelection(final int checkedId, final boolean isChecked) {
        if (!mSuppressListeners) {
            QuestionResponse r = getResponse();
            if (r != null && r.getValue() != null
                    && r.getValue().trim().length() > 0 && promptOnChange) {
                ViewUtil.showConfirmDialog(R.string.confirmchangetitle,
                        R.string.confirmchangetext, getContext(), true,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                handleSelectionInternal(checkedId, isChecked);
                            }
                        }, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                // if they select "Cancel", then undo the change
                                rehydrate(getResponse());
                            }
                        });
            } else {
                handleSelectionInternal(checkedId, isChecked);
            }
        }
    }

    /**
     * populates the QuestionResponse object based on the current state of the
     * selected option(s)
     * 
     * @param checkedId
     * @param isChecked
     */
    private void handleSelectionInternal(int checkedId, boolean isChecked) {
        if (OTHER_TEXT.equals(mIdToValueMap.get(checkedId))) {
            // only display the dialog if OTHER isn't already populated as
            // the response need this to suppress the OTHER dialog
            if (isChecked && (getResponse() == null || !getResponse().getType()
                    .equals(ConstantUtil.OTHER_RESPONSE_TYPE))) {
                displayOtherDialog();
            } else if (!isChecked && getResponse() != null) {
                // since they unchecked "Other", clear the display
                if (mOtherText != null) {
                    mOtherText.setText("");
                }
                mLatestOtherText = "";
                QuestionResponse r = getResponse();
                r.setType(ConstantUtil.VALUE_RESPONSE_TYPE);
                if (mQuestion.isAllowMultiple()) {
                    r.setValue(getMultipleSelections());
                } else {
                    r.setValue("");
                }
            }

        } else {
            if (!mQuestion.isAllowMultiple()
                    || (mQuestion.isAllowMultiple() && (getResponse() == null
                            || getResponse().getValue() == null || getResponse()
                            .getValue().trim().length() == 0))) {
                // if we don't allow multiple and they didn't select other, we
                // can clear the otherText
                if (mOtherText != null) {
                    mOtherText.setText("");
                }
                setResponse(new QuestionResponse(mIdToValueMap.get(checkedId),
                        ConstantUtil.VALUE_RESPONSE_TYPE, mQuestion.getId()));
            } else {
                // if there is already a response and we support multiple,
                // we have to combine
                QuestionResponse r = getResponse();
                String newResponse = getMultipleSelections();
                r.setValue(newResponse);
                r.setType(ConstantUtil.VALUE_RESPONSE_TYPE);
                notifyQuestionListeners(QuestionInteractionEvent.QUESTION_ANSWER_EVENT);
            }
        }
    }

    /**
     * Forms a delimited string containing all selected options not including OTHER
     */
    private String getMultipleSelections() {
        StringBuffer newResponse = new StringBuffer();
        int count = 0;
        if (mCheckBoxes != null) {
            for (int i = 0; i < mCheckBoxes.size(); i++) {
                if (mCheckBoxes.get(i).isChecked()) {
                    if (count > 0) {
                        newResponse.append("|");
                    }
                    if (!OTHER_TEXT.equals(mIdToValueMap.get(mCheckBoxes.get(i)
                            .getId()))) {
                        newResponse.append(mIdToValueMap.get(mCheckBoxes.get(i)
                                .getId()));
                    } else {
                        // if OTHER is selected
                        newResponse.append(mLatestOtherText);

                    }
                    count++;
                }
            }
        }
        return newResponse.toString();
    }

    /**
     * displays a pop-up dialog where the user can enter in a specific value for
     * the "OTHER" option in a freetext view.
     */
    private void displayOtherDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LinearLayout main = new LinearLayout(getContext());
        main.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        main.setOrientation(LinearLayout.VERTICAL);
        builder.setTitle(R.string.otherinstructions);
        final EditText inputView = new EditText(getContext());
        inputView.setSingleLine();
        main.addView(inputView);
        builder.setView(main);
        builder.setPositiveButton(R.string.okbutton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mLatestOtherText = inputView.getText().toString();
                        if (mLatestOtherText == null) {
                            mLatestOtherText = "";
                        } else {
                            mLatestOtherText = mLatestOtherText.trim();
                        }
                        if (getQuestion().isAllowMultiple()
                                && getResponse() != null
                                && getResponse().getValue() != null) {
                            // if we support multiple, we need to append the answer
                            String responseText = getMultipleSelections();

                            setResponse(new QuestionResponse(responseText,
                                    ConstantUtil.OTHER_RESPONSE_TYPE, mQuestion.getId()));
                        } else {
                            // if we aren't supporting multiple or we don't
                            // already have a value, just set it
                            setResponse(new QuestionResponse(mLatestOtherText,
                                    ConstantUtil.OTHER_RESPONSE_TYPE, mQuestion.getId()));
                        }
                        // update the UI with the other text
                        if (mOtherText != null) {
                            mOtherText.setText(mLatestOtherText);
                        }
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                });
        builder.setNegativeButton(R.string.cancelbutton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Reset the answer only for single-choice questions
                        if (!getQuestion().isAllowMultiple()) {
                            setResponse(new QuestionResponse("", ConstantUtil.OTHER_RESPONSE_TYPE,
                                    mQuestion.getId()));
                        }
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                });

        builder.show();
    }

    /**
     * checks off the correct option based on the response value
     */
    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);

        mSuppressListeners = true;
        if (resp != null) {
            if (mOptionGroup != null) {
                // the enhanced for loop is ok here
                for (Integer key : mIdToValueMap.keySet()) {
                    // if the response text matches the text stored for this
                    // option ID OR if the response is the "OTHER" type and the
                    // id matches the other option, select it
                    if (mIdToValueMap.get(key).equals(resp.getValue())
                            || (ConstantUtil.OTHER_RESPONSE_TYPE.equals(resp
                                    .getType()) && mIdToValueMap.get(key)
                                    .equals(OTHER_TEXT))) {
                        mOptionGroup.check(key);
                        if (mIdToValueMap.get(key).equals(OTHER_TEXT) && mQuestion.isAllowOther()) {
                            String txt = resp.getValue();
                            if (txt != null) {
                                mOtherText.setText(txt);
                            }
                        }
                        break;
                    }
                }
            } else if (mCheckBoxes != null) {
                if (resp.getValue() != null
                        && resp.getValue().trim().length() > 0) {
                    // if the response text matches the text stored for this
                    // option ID OR if the response is the "OTHER" type and the
                    // id matches the other option, select it
                    List<String> valList = Arrays.asList(resp.getValue().split(
                            "\\|"));
                    for (Integer key : mIdToValueMap.keySet()) {
                        if (valList.contains(mIdToValueMap.get(key))) {
                            mCheckBoxes.get(key.intValue()).setChecked(true);
                        } else if (ConstantUtil.OTHER_RESPONSE_TYPE.equals(resp.getType()) &&
                                OTHER_TEXT.equals(mIdToValueMap.get(key)) &&
                                mQuestion.isAllowOther()) {
                            mCheckBoxes.get(key.intValue()).setChecked(true);
                            // the last token is always the Other text (even if
                            // it's blank)
                            mLatestOtherText = valList.get(valList.size() - 1);
                            mOtherText.setText(mLatestOtherText);
                        }
                    }
                }

            }
            if (mSpinner != null && resp.getValue() != null) {
                List<Option> options = mQuestion.getOptions();
                if (ConstantUtil.OTHER_RESPONSE_TYPE.equals(resp.getType()) && mQuestion.isAllowOther()) {
                    // since OTHER is the last option and the response is of
                    // OTHER type, select the last option in the spinner which
                    // is size +1 (accounting for the initial BLANK and the
                    // OTHER options which both aren't in the options
                    // collection)
                    mSpinner.setSelection(options.size() + 1);
                    if (resp.getValue() != null) {
                        mOtherText.setText(resp.getValue());
                    }
                } else {
                    boolean found = false;
                    for (int i = 0; i < options.size(); i++) {
                        if (resp.getValue().equalsIgnoreCase(
                                options.get(i).getText())) {
                            // need to add 1 because of the initial blank option
                            mSpinner.setSelection(i + 1);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        mSpinner.setSelection(0);
                    }
                }
            }
        }
        mSuppressListeners = false;
    }

    /**
     * clears the selected option
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mSuppressListeners = true;
        if (mOptionGroup != null) {
            mOptionGroup.clearCheck();
        }
        if (mSpinner != null) {
            mSpinner.setSelection(0);
        }
        if (mCheckBoxes != null) {
            for (int i = 0; i < mCheckBoxes.size(); i++) {
                mCheckBoxes.get(i).setChecked(false);
            }
        }
        mSuppressListeners = false;
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        if (mOptionGroup != null && mOptionGroup.getChildCount() > 0) {
            for (int i = 0; i < mOptionGroup.getChildCount(); i++) {
                ((RadioButton) (mOptionGroup.getChildAt(i))).setTextSize(size);
            }
        } else if (mCheckBoxes != null && mCheckBoxes.size() > 0) {
            for (int i = 0; i < mCheckBoxes.size(); i++) {
                mCheckBoxes.get(i).setTextSize(size);
            }
        }
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
    }

}
