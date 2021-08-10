/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.akvo.flow.R;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.utils.entity.Option;
import org.akvo.flow.utils.entity.Question;

import java.util.ArrayList;
import java.util.List;

public class OptionQuestionViewSingle extends OptionQuestionView {

    private static final int INVALID_RADIO_BUTTON_ID = -1;

    @Nullable
    private RadioGroup mOptionGroup;

    public OptionQuestionViewSingle(Context context, Question q,
            SurveyListener surveyListener) {
        super(context, q, surveyListener);
    }

    @Override
    void initOptionViews() {
        mOptionGroup = new RadioGroup(getContext());
        mOptionGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                handleSelection(checkedId, true);
            }
        });
        addView(mOptionGroup);

        if (mOptions != null) {
            for (int i = 0; i < mOptions.size(); i++) {
                Option option = mOptions.get(i);
                View view;
                view = newRadioButton(option, i);
                mOptionGroup.addView(view);
                view.setEnabled(!isReadOnly());
                view.setId(i); // View ID will match option position within the array
            }
        }
    }

    private View newRadioButton(Option option, int i) {
        RadioButton rb = new RadioButton(getContext());
        RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        if (i != 0) {
            params.topMargin = getDimension(R.dimen.small_padding);
        }
        rb.setLayoutParams(params);
        rb.setLongClickable(true);
        rb.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onClearAnswer();
                return true;
            }
        });
        if (option.isOther()) {
            rb.setText(otherOptionText);
        } else {
            rb.setText(formOptionText(option), TextView.BufferType.SPANNABLE);
        }

        return rb;
    }

    @Override
    public void notifyOptionsChanged() {
        super.notifyOptionsChanged();
        if (mOptionGroup != null && mOptions != null) {
            for (int i = 0; i < mOptionGroup.getChildCount(); i++) {
                // make sure we have a corresponding option (i.e. not the OTHER option)
                Option option = mOptions.get(i);
                if (!option.isOther()) {
                    ((RadioButton) (mOptionGroup.getChildAt(i)))
                            .setText(formOptionText(option));
                }
            }
        }
    }

    @Override
    protected void checkMatchingItem(int i) {
        if (mOptionGroup != null) {
            mOptionGroup.check(i);
        }
    }

    @Override
    void resetViews() {
        if (mOptionGroup != null) {
            mOptionGroup.clearCheck();
        }
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        if (mOptionGroup != null && mOptionGroup.getChildCount() > 0) {
            for (int i = 0; i < mOptionGroup.getChildCount(); i++) {
                ((RadioButton) (mOptionGroup.getChildAt(i))).setTextSize(size);
            }
        }
    }

    @Override
    List<Option> getSelection() {
        List<Option> options = new ArrayList<>();
        if (mOptionGroup != null && mOptions != null) {
            int checked = mOptionGroup.getCheckedRadioButtonId();
            if (checked != INVALID_RADIO_BUTTON_ID) {
                options.add(mOptions.get(checked));
            }
        }
        return options;
    }
}
