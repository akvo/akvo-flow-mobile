/*
 *  Copyright (C) 2016-2018 Stichting Akvo (Akvo Foundation)
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
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.presentation.form.caddisfly.CaddisflyPresenter;
import org.akvo.flow.presentation.form.caddisfly.CaddisflyView;
import org.akvo.flow.ui.adapter.CaddisflyResultsAdapter;
import org.akvo.flow.ui.model.caddisfly.CaddisflyJsonMapper;
import org.akvo.flow.ui.model.caddisfly.CaddisflyTestResult;
import org.akvo.flow.util.ConstantUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import timber.log.Timber;

public class CaddisflyQuestionView extends QuestionView implements View.OnClickListener,
        CaddisflyView {

    @Inject
    CaddisflyPresenter presenter;

    @Inject
    CaddisflyJsonMapper caddisflyJsonMapper;

    @Inject
    SnackBarManager snackBarManager;

    private String mValue;
    private String mImage;
    private CaddisflyResultsAdapter caddisflyResultsAdapter;
    private List<CaddisflyTestResult> caddisflyTestResults = new ArrayList<>();

    public CaddisflyQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.caddisfly_question_view);

        initialiseInjector();

        RecyclerView resultsRv = (RecyclerView) findViewById(R.id.caddisfly_results_recycler_view);
        resultsRv.setLayoutManager(new LinearLayoutManager(resultsRv.getContext()));
        caddisflyResultsAdapter = new CaddisflyResultsAdapter(
                new ArrayList<CaddisflyTestResult>());
        resultsRv.setAdapter(caddisflyResultsAdapter);
        Button mButton = (Button) findViewById(R.id.caddisfly_button);
        if (isReadOnly()) {
            mButton.setVisibility(GONE);
        } else {
            mButton.setOnClickListener(this);
        }
        displayResponseView();
        presenter.setView(this);
    }

    private void initialiseInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    private void displayResponseView() {
        caddisflyResultsAdapter.setCaddisflyTestResults(caddisflyTestResults);
    }

    @Override
    public void captureResponse(boolean suppressListeners) {
        Question question = getQuestion();
        QuestionResponse r = new QuestionResponse.QuestionResponseBuilder()
                .setValue(mValue)
                .setType(ConstantUtil.CADDISFLY_RESPONSE_TYPE)
                .setQuestionId(question.getQuestionId())
                .setIteration(question.getIteration())
                .setFilename(mImage)
                .createQuestionResponse();
        setResponse(r);
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        mValue = resp != null ? resp.getValue() : null;
        caddisflyTestResults = caddisflyJsonMapper.transform(mValue);
        displayResponseView();
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mValue = null;
        caddisflyTestResults = caddisflyJsonMapper.transform(mValue);
        displayResponseView();
    }

    @Override
    public void questionComplete(Bundle data) {
        if (data != null) {
            mValue = data.getString(ConstantUtil.CADDISFLY_RESPONSE);
            caddisflyTestResults = caddisflyJsonMapper.transform(mValue);
            // Get optional image and store it as part of the response
            String image = data.getString(ConstantUtil.CADDISFLY_IMAGE);

            Timber.d("caddisflyTestComplete - Response: %s . Image: %s", mValue, image);

            File src = !TextUtils.isEmpty(image) ? new File(image) : null;
            if (src != null && src.exists()) {
                presenter.onImageReady(src);
            } else {
                captureResponse();
            }
            displayResponseView();
        } else {
            captureResponse();
        }
    }

    @Override
    public void onClick(View view) {
        Question q = getQuestion();
        Bundle data = new Bundle();
        data.putString(ConstantUtil.CADDISFLY_RESOURCE_ID, q.getCaddisflyRes());
        data.putString(ConstantUtil.CADDISFLY_QUESTION_ID, q.getId());
        data.putString(ConstantUtil.CADDISFLY_QUESTION_TITLE, q.getText());
        data.putString(ConstantUtil.CADDISFLY_DATAPOINT_ID, mSurveyListener.getDatapointId());
        data.putString(ConstantUtil.CADDISFLY_FORM_ID, mSurveyListener.getFormId());
        data.putString(ConstantUtil.CADDISFLY_LANGUAGE, Locale.getDefault().getLanguage());
        notifyQuestionListeners(QuestionInteractionEvent.CADDISFLY, data);
    }

    @Override
    public void showErrorGettingMedia() {
        snackBarManager.displaySnackBar(this, R.string.error_getting_media, getContext());
    }

    @Override
    public void updateResponse() {
        mImage = null;
        captureResponse();
    }

    @Override
    public void updateResponse(String copiedImagePath) {
        mImage = copiedImagePath;
        captureResponse();
    }
}
