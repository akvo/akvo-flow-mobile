/*
 *  Copyright (C) 2010-2015,2018-2019 Stichting Akvo (Akvo Foundation)
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.akvo.flow.R;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.utils.entity.Question;

import java.util.ArrayList;
import java.util.List;

public class SubmitTab extends ListView implements OnClickListener {

    @VisibleForTesting
    public static final String FOOTER = "FOOTER";

    private final QuestionListAdapter adapter;

    private final SurveyListener mListener;

    private final TextView mHeaderView;
    private final Button mSubmitButton;

    public SubmitTab(Context context) {
        super(context);
        if (!(context instanceof SurveyListener)) {
            throw new IllegalArgumentException("Activity must implement SurveyListener");
        }
        mListener = (SurveyListener) context;
        setId(R.id.submit_tab);
        final int listPadding = (int) getResources().getDimension(R.dimen.form_left_right_padding);
        setPadding(listPadding, listPadding, listPadding, listPadding);

        mHeaderView = (TextView) inflate(context, R.layout.submit_tab_header);

        mSubmitButton = (Button) inflate(context, R.layout.submit_tab_footer);
        mSubmitButton.setOnClickListener(this);
        setFooterDividersEnabled(false);
        setHeaderDividersEnabled(false);
        addHeaderView(mHeaderView);
        addFooterView(mSubmitButton, FOOTER, true);
        adapter = new QuestionListAdapter();
        setAdapter(adapter);
        refresh(adapter.questions);
    }

    private View inflate(Context context, int layoutResId) {
        return LayoutInflater.from(context).inflate(layoutResId, this, false);
    }

    public void refresh(List<Question> invalidQuestions) {
        adapter.setQuestions(invalidQuestions);
        if (!invalidQuestions.isEmpty()) {
            mHeaderView.setText(R.string.error_responses);
            mSubmitButton.setEnabled(false);
        } else if (mListener.getResponses().isEmpty()) {
            mHeaderView.setText(R.string.error_empty_form);
            mSubmitButton.setEnabled(false);
        } else {
            mHeaderView.setText(R.string.submit_tab_description);
            mSubmitButton.setEnabled(true);
        }
    }

    public void onClick(View v) {
        mListener.onSurveySubmit();
    }

    static class QuestionListAdapter extends BaseAdapter {

        private final List<Question> questions = new ArrayList<>();

        QuestionListAdapter() {
        }

        void setQuestions(@Nullable List<Question> questions) {
            this.questions.clear();
            if (questions != null) {
                this.questions.addAll(questions);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return questions.size();
        }

        @Override
        public Object getItem(int position) {
            return questions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            QuestionView qv;
            Question question = questions.get(position);
            if (convertView == null) {
                Context context = parent.getContext();
                if (!(context instanceof SurveyListener)) {
                    throw new IllegalArgumentException("Activity must implement SurveyListener");
                }
                SurveyListener listener = (SurveyListener) context;
                qv = new QuestionHeaderView(context, question, listener, true, 0); //TODO: will this work?
            } else {
                qv = (QuestionHeaderView) convertView;
                qv.mQuestion = question;
                qv.displayContent();
            }
            // force the view to be visible (if the question has
            // dependencies, it'll be hidden by default)
            qv.setTag(question.getQuestionId());
            qv.setVisibility(View.VISIBLE);
            return qv;
        }
    }

}
