/*
 *  Copyright (C) 2010-2018,2021 Stichting Akvo (Akvo Foundation)
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
import android.view.View;

import org.akvo.flow.R;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.utils.entity.Question;

public class QuestionHeaderView extends QuestionView implements View.OnClickListener {

    private final boolean mDisplayShortcut;

    public QuestionHeaderView(Context context, Question q, SurveyListener surveyListener, int repetition) {
        this(context, q, surveyListener, false, repetition);
    }

    public QuestionHeaderView(Context context, Question q, SurveyListener surveyListener,
                              boolean displayShortcut, int repetition) {
        super(context, q, surveyListener, repetition);
        mDisplayShortcut = displayShortcut;
        init();
    }

    private void init() {
        setQuestionView(R.layout.invalid_question_view);
        View openButton = findViewById(R.id.invalid_question_open_btn);
        if (mDisplayShortcut) {
            openButton.setOnClickListener(this);
        } else {
            openButton.setVisibility(GONE);
        }
    }

    @Override
    public void onClick(View v) {
        mSurveyListener.openQuestion(getQuestion().getQuestionId());
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        //EMPTY
    }
}
