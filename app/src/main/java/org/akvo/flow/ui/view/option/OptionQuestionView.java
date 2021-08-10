/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.view.option;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.serialization.response.value.OptionValue;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.utils.entity.AltText;
import org.akvo.flow.utils.entity.Option;

import java.util.HashMap;
import java.util.List;

/**
 * Question type that supports the selection of a single option from a list of
 * choices (i.e. a radio button group).
 *
 * @author Christopher Fagiani
 */
public abstract class OptionQuestionView extends QuestionView {

    private static final String OTHER_CODE = "OTHER";

    final String otherOptionText;

    @Nullable
    List<Option> mOptions;

    private volatile boolean mSuppressListeners = false;

    private TextView mOtherText;
    private String mLatestOtherText;

    OptionQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        otherOptionText = getResources().getString(R.string.othertext);
        init();
    }

    void init() {
        // Just inflate the header. Options will be added dynamically
        setQuestionView(R.layout.question_header);
        mOptions = mQuestion.getOptions();

        mSuppressListeners = true;

        appendOtherOption();

        initOptionViews();

        appendOtherView();

        mSuppressListeners = false;
    }

    abstract void initOptionViews();

    private void appendOtherOption() {
        if (mQuestion.isAllowOther()) {
            Option other = new Option(null, null, true, new HashMap<>());
            // Only add code if codes are defined
            if (mOptions != null) {
                if (!mOptions.isEmpty() && !TextUtils.isEmpty(mOptions.get(0).getCode())) {
                    other.setCode(OTHER_CODE);
                }
                mOptions.add(other);
            }
        }
    }

    private void appendOtherView() {
        if (mQuestion.isAllowOther()) {
            mOtherText = new TextView(getContext());
            mOtherText.setId(R.id.other_option_text);
            mOtherText.setLayoutParams(
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            addView(mOtherText);
        }
    }

    /**
     * forms the text for an option based on the visible languages
     */
    Spanned formOptionText(Option option) {
        boolean isFirst = true;
        StringBuilder text = new StringBuilder();
        final String[] languages = getLanguages();
        for (int i = 0; i < languages.length; i++) {
            if (getDefaultLang().equalsIgnoreCase(languages[i])) {
                if (!isFirst) {
                    text.append(" / ");
                } else {
                    isFirst = false;
                }
                text.append(TextUtils.htmlEncode(option.getText()));

            } else {
                AltText txt = option.getAltText(languages[i]);
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
    void handleSelection(int checkedId, boolean isChecked) {
        if (mSuppressListeners || mOptions == null) {
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
    abstract List<Option> getSelection();

    /**
     * displays a pop-up dialog where the user can enter in a specific value for
     * the "OTHER" option in a freeText view.
     */
    private void displayOtherDialog(final int otherId) {
        LinearLayout main = new LinearLayout(getContext());
        main.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        main.setOrientation(LinearLayout.VERTICAL);
        final EditText inputView = new EditText(getContext());
        inputView.setSingleLine();
        if (!TextUtils.isEmpty(mLatestOtherText)) {
            inputView.append(mLatestOtherText);
        }
        inputView.setId(R.id.other_option_input);
        main.addView(inputView);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.otherinstructions);
        builder.setView(main);
        builder.setPositiveButton(R.string.okbutton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mLatestOtherText = inputView.getText().toString().trim();
                        if (mOptions != null && mOptions.size() > otherId) {
                            mOptions.get(otherId).setText(mLatestOtherText);
                        }
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
        if (mOptions != null) {
            for (Option selectedOption : selectedOptions) {
                for (int i = 0; i < mOptions.size(); i++) {
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
                        checkMatchingItem(i);
                        break; // TODO: Break outer loop in single-choice responses?
                    }
                }
            }
        }
        mSuppressListeners = false;
    }

    abstract void checkMatchingItem(int i);

    /**
     * clears the selected option
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mSuppressListeners = true;
        resetViews();
        mSuppressListeners = false;
    }

    abstract void resetViews();

    @Override
    public void captureResponse(boolean suppressListeners) {
        String response = null;
        List<Option> values = getSelection();
        if (!values.isEmpty()) {
            response = OptionValue.serialize(values);
        }
        setResponse(suppressListeners, getQuestion(), response, ConstantUtil.OPTION_RESPONSE_TYPE);
    }
}
