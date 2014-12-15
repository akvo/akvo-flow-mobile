/*
 *  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
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
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;

public class PlotQuestionView extends QuestionView implements OnClickListener {
    private EditText mInputText;
    private Button mMapBtn;

    private String mValue;

    public PlotQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.plot_question_view);

        mInputText = (EditText)findViewById(R.id.input_text);
        mMapBtn = (Button)findViewById(R.id.plotting_btn);

        if (isReadOnly()) {
            mMapBtn.setVisibility(View.GONE);
        }

        mMapBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.plotting_btn:
                Bundle data = null;
                if (!TextUtils.isEmpty(mValue)) {
                    data = new Bundle();
                    data.putString(ConstantUtil.GEOSHAPE_RESULT, mValue);
                }
                notifyQuestionListeners(QuestionInteractionEvent.PLOTTING_EVENT, data);
                break;
        }
    }

    @Override
    public void questionComplete(Bundle data) {
        if (data != null) {
            mValue = data.getString(ConstantUtil.GEOSHAPE_RESULT);
            mInputText.setText(mValue);
            captureResponse();
        }
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        mValue = resp.getValue();
        mInputText.setText(mValue);
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mInputText.setText("");
        mValue = null;
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        setResponse(new QuestionResponse(mValue, ConstantUtil.VALUE_RESPONSE_TYPE,
                getQuestion().getId()), suppressListeners);
    }

}
