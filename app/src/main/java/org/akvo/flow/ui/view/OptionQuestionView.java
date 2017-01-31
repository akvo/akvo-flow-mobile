/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import org.akvo.flow.R;
import org.akvo.flow.domain.AltText;
import org.akvo.flow.domain.Option;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.serialization.response.value.OptionValue;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Question type that supports the selection of a single option from a list of
 * choices (i.e. a radio button group).
 *
 * @author Christopher Fagiani
 */
public class OptionQuestionView extends QuestionView {
    private static final String OTHER_CODE = "OTHER";
    private final String OTHER_TEXT;
    private RadioGroup mOptionGroup;
    private List<CheckBox> mCheckBoxes;
    private TextView mOtherText;
    private List<Option> mOptions;
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
        mOptions = mQuestion.getOptions();

        if (mOptions == null) {
            return;
        }

        mSuppressListeners = true;

        // Append 'other' option, if necessary
        if (mQuestion.isAllowOther()) {
            Option other = new Option();
            other.setIsOther(true);
            // Only add code if codes are defined
            if (!mOptions.isEmpty() && !TextUtils.isEmpty(mOptions.get(0).getCode())) {
                other.setCode(OTHER_CODE);
            }
            mOptions.add(other);
        }

        if (mQuestion.isAllowMultiple()) {
            mCheckBoxes = new ArrayList<>();
        } else {
            mOptionGroup = new RadioGroup(getContext());
            mOptionGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    handleSelection(checkedId, true);
                }
            });
            addView(mOptionGroup);
        }

        for (int i = 0; i < mOptions.size(); i++) {
            Option option = mOptions.get(i);
            View view;
            if (mQuestion.isAllowMultiple()) {
                view = newCheckbox(option);
                mCheckBoxes.add((CheckBox)view);
                addView(view);
            } else {
                view = newRadioButton(option);
                mOptionGroup.addView(view);
            }
            view.setEnabled(!isReadOnly());
            view.setId(i);// View ID will match option position within the array
        }

        if (mQuestion.isAllowOther()) {
            mOtherText = new TextView(getContext());
            mOtherText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            addView(mOtherText);
        }

        mSuppressListeners = false;
    }

    private View newRadioButton(Option option) {
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
        if (option.isOther()) {
            rb.setText(OTHER_TEXT);
        } else {
            rb.setText(formOptionText(option), BufferType.SPANNABLE);
        }

        return rb;
    }

    private View newCheckbox(Option option) {
        CheckBox box = new CheckBox(getContext());
        box.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                handleSelection(buttonView.getId(), isChecked);
            }
        });
        if (option.isOther()) {
            box.setText(OTHER_TEXT);
        } else {
            box.setText(formOptionText(option), BufferType.SPANNABLE);
        }

        return box;
    }

    @Override
    public void notifyOptionsChanged() {
        super.notifyOptionsChanged();

        if (mQuestion.isAllowMultiple()) {
            for (int i = 0; i < mCheckBoxes.size(); i++) {
                // make sure we have a corresponding option (i.e. not the OTHER option)
                if (!mOptions.get(i).isOther()) {
                    mCheckBoxes.get(i).setText(formOptionText(mOptions.get(i)), BufferType.SPANNABLE);
                }
            }
        } else {
            for (int i = 0; i < mOptionGroup.getChildCount(); i++) {
                // make sure we have a corresponding option (i.e. not the OTHER option)
                if (!mOptions.get(i).isOther()) {
                    ((RadioButton) (mOptionGroup.getChildAt(i))).setText(formOptionText(mOptions.get(i)));
                }
            }
        }
    }

    /**
     * forms the text for an option based on the visible languages
     */
    private Spanned formOptionText(Option option) {
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
                text.append(TextUtils.htmlEncode(option.getText()));

            } else {
                AltText txt = option.getAltText(langs[i]);
                if (txt != null) {
                    if (!isFirst) {
                        text.append(" / ");
                    } else {
                        isFirst = false;
                    }
                    text.append("<font color='")
                            .append(sColors[i])
                            .append("'>")
                            .append(TextUtils.htmlEncode(txt.getText()))
                            .append("</font>");
                }
            }
        }
        return Html.fromHtml(text.toString());
    }

    /**
     * populates the QuestionResponse object based on the current state of the
     * selected option(s)
     */
    private void handleSelection(int checkedId, boolean isChecked) {
        if (mSuppressListeners) {
            return;
        }

        if (mOptions.get(checkedId).isOther() && isChecked) {
            displayOtherDialog(checkedId);
            return;
        }

        captureResponse();
    }

    /**
     * Forms a delimited string containing all selected options not including OTHER
     */
    private List<Option> getSelection() {
        List<Option> options = new ArrayList<>();
        if (mQuestion.isAllowMultiple()) {
            for (CheckBox cb: mCheckBoxes) {
                if (cb.isChecked()) {
                    Option option = mOptions.get(cb.getId());
                    options.add(option);
                }
            }
        } else {
            int checked = mOptionGroup.getCheckedRadioButtonId();
            if (checked != -1) {
                options.add(mOptions.get(checked));
            }
        }

        return options;
    }

    /**
     * displays a pop-up dialog where the user can enter in a specific value for
     * the "OTHER" option in a freetext view.
     */
    private void displayOtherDialog(final int otherId) {
        LinearLayout main = new LinearLayout(getContext());
        main.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        main.setOrientation(LinearLayout.VERTICAL);
        final EditText inputView = new EditText(getContext());
        inputView.setSingleLine();
        if (!TextUtils.isEmpty(mLatestOtherText)) {
            inputView.append(mLatestOtherText);
        }
        main.addView(inputView);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.otherinstructions);
        builder.setView(main);
        builder.setPositiveButton(R.string.okbutton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mLatestOtherText = inputView.getText().toString().trim();
                        mOptions.get(otherId).setText(mLatestOtherText);
                        captureResponse();
                        // update the UI with the other text
                        if (mOtherText != null) {
                            mOtherText.setText(mLatestOtherText);
                        }
                    }
                });
        builder.setNegativeButton(R.string.cancelbutton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Deselect 'other'
                        handleSelection(otherId, false);
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

        if (resp == null || TextUtils.isEmpty(resp.getValue())) {
            return;
        }

        List<Option> selectedOptions = OptionValue.deserialize(resp.getValue());
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            return;
        }

        mSuppressListeners = true;
        for (Option selectedOption : selectedOptions) {
            for (int i=0; i<mOptions.size(); i++) {
                Option option = mOptions.get(i);
                boolean match = selectedOption.equals(option);
                if (!match && option.isOther()) {
                    // Assume this is the OTHER value. A more reliable indicator would be to check
                    // selected response's `isOther` flag, but this is not guaranteed to be present
                    // in old responses.
                    match = true;
                    mLatestOtherText = selectedOption.getText();
                    mOtherText.setText(mLatestOtherText);
                    option.setText(mLatestOtherText);
                }
                if (match) {
                    if (mQuestion.isAllowMultiple()) {
                        mCheckBoxes.get(i).setChecked(true);
                    } else {
                        mOptionGroup.check(i);
                    }
                    break;// TODO: Break outer loop in single-choice responses?
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
        String response = null;
        List<Option> values = getSelection();
        if (!values.isEmpty()) {
            response = OptionValue.serialize(values);
        }
        setResponse(new QuestionResponse(response, ConstantUtil.OPTION_RESPONSE_TYPE,
                getQuestion().getId()), suppressListeners);
    }

}
