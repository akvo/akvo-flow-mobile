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

package org.akvo.flow.ui.view;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.activity.SurveyActivity;
import org.akvo.flow.activity.SurveyViewActivity;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceStatus;
import org.akvo.flow.domain.Question;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ViewUtil;

import java.util.ArrayList;
import java.util.List;

public class SubmitTab extends ListView implements OnClickListener {
    private long mSurveyInstanceId;
    private String mLanguage;
    private String[] mLanguages;
    private SurveyDbAdapter mDatabase;

    private TextView mHeaderView;
    private Button mSubmitButton;

    public SubmitTab(Context context, long surveyInstanceId, String defaultLang, String[] languages,
            SurveyDbAdapter database) {
        super(context);

        mSurveyInstanceId = surveyInstanceId;
        mLanguage = defaultLang;
        mLanguages = languages;
        mDatabase = database;

        mHeaderView = new TextView(context);
        mHeaderView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        mHeaderView.setTextColor(Color.RED);
        mHeaderView.setTextSize(18);
        mSubmitButton = new Button(context);
        mSubmitButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        mSubmitButton.setText(context.getString(R.string.submitbutton));
        mSubmitButton.setOnClickListener(this);

        addHeaderView(mHeaderView);
        addFooterView(mSubmitButton);

        refresh(new ArrayList<Question>());
    }

    public void refresh(List<Question> invalidQuestions) {
        QuestionListAdapter adapter = new QuestionListAdapter(invalidQuestions);
        setAdapter(adapter);

        if (invalidQuestions.isEmpty()) {
            mHeaderView.setText(R.string.submittext);
            mSubmitButton.setEnabled(true);
        } else {
            mHeaderView.setText(R.string.error_responses);
            mSubmitButton.setEnabled(false);
        }
    }

    public void onClick(View v) {
        // TODO: This a temporary (and dreadful) hack. Provide a SurveyListener interface instead
        final SurveyActivity activity = (SurveyActivity)getContext();

        //activity.saveSessionDuration();

        // if we have no missing responses, submit the survey
        mDatabase.updateSurveyStatus(mSurveyInstanceId, SurveyInstanceStatus.SUBMITTED);
        // send a broadcast message indicating new data is available
        Intent i = new Intent(ConstantUtil.DATA_AVAILABLE_INTENT);
        getContext().sendBroadcast(i);

        ViewUtil.showConfirmDialog(R.string.submitcompletetitle, R.string.submitcompletetext,
                getContext(), false,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (dialog != null) {
                            activity.finish();
                        }
                    }
                });
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
            QuestionView qv = new QuestionHeaderView(getContext(), mQuestions.get(position),
                    mLanguage, mLanguages, true);
            // force the view to be visible (if the question has
            // dependencies, it'll be hidden by default)
            qv.setVisibility(View.VISIBLE);
            return qv;
        }

    }

}
