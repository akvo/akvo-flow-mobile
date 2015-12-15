/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;

public class GeoshapeQuestionView extends QuestionView implements OnClickListener {
    private View mResponseView;
    private Button mMapBtn;

    private String mValue;

    public GeoshapeQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.geoshape_question_view);
        mResponseView = findViewById(R.id.response_view);
        mMapBtn = (Button)findViewById(R.id.capture_shape_btn);
        mMapBtn.setOnClickListener(this);
        if (isReadOnly()) {
            mMapBtn.setText(R.string.view_shape);
        }
        displayResponseView();
    }

    private void displayResponseView() {
        mResponseView.setVisibility(TextUtils.isEmpty(mValue) ? GONE : VISIBLE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.capture_shape_btn:
                Bundle data = new Bundle();
                data.putBoolean(ConstantUtil.EXTRA_ALLOW_POINTS, getQuestion().isAllowPoints());
                data.putBoolean(ConstantUtil.EXTRA_ALLOW_LINE, getQuestion().isAllowLine());
                data.putBoolean(ConstantUtil.EXTRA_ALLOW_POLYGON, getQuestion().isAllowPolygon());
                data.putBoolean(ConstantUtil.EXTRA_MANUAL_INPUT, !getQuestion().isLocked());
                data.putBoolean(ConstantUtil.READONLY_KEY, isReadOnly());
                if (!TextUtils.isEmpty(mValue)) {
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
            displayResponseView();
            captureResponse();
        }
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        mValue = resp.getValue();
        displayResponseView();

        if (isReadOnly() && !TextUtils.isEmpty(mValue)) {
            mMapBtn.setVisibility(VISIBLE);
        }
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mValue = null;
        displayResponseView();
        if (isReadOnly()) {
            mMapBtn.setVisibility(GONE);
        }
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        setResponse(new QuestionResponse(mValue, ConstantUtil.VALUE_RESPONSE_TYPE,
                getQuestion().getId()), suppressListeners);
    }

}
