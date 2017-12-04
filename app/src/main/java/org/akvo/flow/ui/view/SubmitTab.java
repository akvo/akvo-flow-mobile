/*
 *  Copyright (C) 2010-2015 Stichting Akvo (Akvo Foundation)
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
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.PlatformUtil;

import java.util.ArrayList;
import java.util.List;

public class SubmitTab extends ListView implements OnClickListener {
    private SurveyListener mListener;

    private TextView mHeaderView;
    private Button mSubmitButton;

    public SubmitTab(Context context, SurveyListener listener) {
        super(context);

        mListener = listener;

        mHeaderView = new TextView(context);
        mHeaderView.setId(R.id.submit_tab_header);
        mHeaderView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        final int padding = (int)PlatformUtil.dp2Pixel(context, 8);
        mHeaderView.setPadding(padding, padding, padding, padding);
        mHeaderView.setTextSize(18);
        mHeaderView.setClickable(false);
        mSubmitButton = new Button(context);
        mSubmitButton.setId(R.id.submit_tab_button);
        mSubmitButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        mSubmitButton.setTextColor(Color.WHITE);
        mSubmitButton.setBackgroundResource(R.drawable.button_primary);
        mSubmitButton.setText(context.getString(R.string.submitbutton));
        mSubmitButton.setOnClickListener(this);

        addHeaderView(mHeaderView);
        addFooterView(mSubmitButton);

        refresh(new ArrayList<Question>());
    }

    public void refresh(List<Question> invalidQuestions) {
        QuestionListAdapter adapter = new QuestionListAdapter(invalidQuestions);
        setAdapter(adapter);

        if (!invalidQuestions.isEmpty()) {
            mHeaderView.setText(R.string.error_responses);
            mSubmitButton.setEnabled(false);
        } else if (mListener.getResponses().isEmpty()) {
            mHeaderView.setText(R.string.error_empty_form);
            mSubmitButton.setEnabled(false);
        } else {
            mHeaderView.setText(R.string.submittext);
            mSubmitButton.setEnabled(true);
        }
    }

    public void onClick(View v) {
        mListener.onSurveySubmit();
    }

    class QuestionListAdapter extends BaseAdapter {
        private List<Question> mQuestions;

        public QuestionListAdapter(List<Question> questions) {
            mQuestions = questions;
        }

        @Override
        public int getCount() {
            return mQuestions.size();
        }

        @Override
        public Object getItem(int position) {
            return mQuestions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final QuestionView qv = new QuestionHeaderView(getContext(), mQuestions.get(position),
                    mListener, true);
            // force the view to be visible (if the question has
            // dependencies, it'll be hidden by default)
            qv.setTag(mQuestions.get(position).getId());
            qv.setVisibility(View.VISIBLE);
            return qv;
        }
    }

}
