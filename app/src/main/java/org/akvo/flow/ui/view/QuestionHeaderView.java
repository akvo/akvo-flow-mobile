/*
 *  Copyright (C) 2010-2014 Stichting Akvo (Akvo Foundation)
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
import android.view.View;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.event.SurveyListener;

public class QuestionHeaderView extends QuestionView implements View.OnClickListener {
    private boolean mDisplayShortcut;

    public QuestionHeaderView(Context context, Question q, SurveyListener surveyListener) {
        this(context, q, surveyListener, false);
    }

    public QuestionHeaderView(Context context, Question q, SurveyListener surveyListener,
            boolean displayShortcut) {
        super(context, q, surveyListener);
        mDisplayShortcut = displayShortcut;
        init();
    }

    private void init() {
        setQuestionView(R.layout.invalid_question_view);
        if (mDisplayShortcut) {
            findViewById(R.id.open_btn).setOnClickListener(this);
        } else {
            findViewById(R.id.open_btn).setVisibility(GONE);
        }
    }

    @Override
    public void onClick(View v) {
        mSurveyListener.openQuestion(getQuestion().getId());
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
    }
}
