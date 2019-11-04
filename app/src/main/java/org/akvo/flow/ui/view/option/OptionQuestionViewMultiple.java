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
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Option;
import org.akvo.flow.domain.Question;
import org.akvo.flow.event.SurveyListener;

import java.util.ArrayList;
import java.util.List;

public class OptionQuestionViewMultiple extends OptionQuestionView {

    @Nullable
    private List<CheckBox> mCheckBoxes;

    public OptionQuestionViewMultiple(Context context, Question q,
            SurveyListener surveyListener) {
        super(context, q, surveyListener);
    }

    @Override
    void initOptionViews() {
        mCheckBoxes = new ArrayList<>();
        if (mOptions != null) {
            for (int i = 0; i < mOptions.size(); i++) {
                Option option = mOptions.get(i);
                View view = newCheckbox(option, i);
                mCheckBoxes.add((CheckBox) view);
                addView(view);
                view.setEnabled(!isReadOnly());
                view.setId(i); // View ID will match option position within the array
            }
        }
    }

    private View newCheckbox(Option option, int i) {
        CheckBox box = new CheckBox(getContext());
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        if (i != 0) {
            params.topMargin = getDimension(R.dimen.small_padding);
        }
        box.setLayoutParams(params);
        box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                handleSelection(buttonView.getId(), isChecked);
            }
        });
        if (option.isOther()) {
            box.setText(otherOptionText);
        } else {
            box.setText(formOptionText(option), TextView.BufferType.SPANNABLE);
        }

        return box;
    }

    @Override
    public void notifyOptionsChanged() {
        super.notifyOptionsChanged();
        if (mCheckBoxes != null && mOptions != null) {
            for (int i = 0; i < mCheckBoxes.size(); i++) {
                Option option = mOptions.get(i);
                if (!option.isOther()) {
                    mCheckBoxes.get(i)
                            .setText(formOptionText(option), TextView.BufferType.SPANNABLE);
                }
            }
        }
    }

    @Override
    void checkMatchingItem(int i) {
        if (mCheckBoxes != null) {
            mCheckBoxes.get(i).setChecked(true);
        }
    }

    @Override
    void resetViews() {
        if (mCheckBoxes != null) {
            for (int i = 0; i < mCheckBoxes.size(); i++) {
                mCheckBoxes.get(i).setChecked(false);
            }
        }
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        if (mCheckBoxes != null && mCheckBoxes.size() > 0) {
            for (int i = 0; i < mCheckBoxes.size(); i++) {
                mCheckBoxes.get(i).setTextSize(size);
            }
        }
    }

    @Override
    List<Option> getSelection() {
        List<Option> options = new ArrayList<>();
        if (mCheckBoxes != null && mOptions != null) {
            for (CheckBox cb : mCheckBoxes) {
                if (cb.isChecked()) {
                    Option option = mOptions.get(cb.getId());
                    options.add(option);
                }
            }
        }
        return options;
    }
}
