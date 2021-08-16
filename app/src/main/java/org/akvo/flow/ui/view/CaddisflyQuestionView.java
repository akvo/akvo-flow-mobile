/*
 *  Copyright (C) 2016-2019 Stichting Akvo (Akvo Foundation)
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

import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.form.caddisfly.CaddisflyPresenter;
import org.akvo.flow.presentation.form.caddisfly.CaddisflyView;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.adapter.CaddisflyResultsAdapter;
import org.akvo.flow.ui.model.caddisfly.CaddisflyJsonMapper;
import org.akvo.flow.ui.model.caddisfly.CaddisflyTestResult;
import org.akvo.flow.uicomponents.SnackBarManager;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.StoragePermissionsHelper;
import org.akvo.flow.utils.entity.Question;

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

    @Inject
    StoragePermissionsHelper storagePermissionsHelper;

    @Inject
    Navigator navigator;

    private String mValue;
    private String mImage;
    private CaddisflyResultsAdapter caddisflyResultsAdapter;
    private List<CaddisflyTestResult> caddisflyTestResults = new ArrayList<>();

    public CaddisflyQuestionView(Context context, Question q, SurveyListener surveyListener, int repetition) {
        super(context, q, surveyListener, repetition);
        init();
    }

    private void init() {
        setQuestionView(R.layout.caddisfly_question_view);

        initialiseInjector();

        RecyclerView resultsRv = findViewById(R.id.caddisfly_results_recycler_view);
        resultsRv.setLayoutManager(new LinearLayoutManager(resultsRv.getContext()));
        caddisflyResultsAdapter = new CaddisflyResultsAdapter(new ArrayList<>());
        resultsRv.setAdapter(caddisflyResultsAdapter);
        Button mButton = findViewById(R.id.caddisfly_button);
        mButton.setOnClickListener(this);
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
                .setIteration(repetition)
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
        caddisflyTestResults = new ArrayList<>();
        displayResponseView();
    }

    @Override
    public void onQuestionResultReceived(Bundle data) {
        if (data != null) {
            mValue = data.getString(ConstantUtil.CADDISFLY_RESPONSE);
            caddisflyTestResults = caddisflyJsonMapper.transform(mValue);
            // Get optional image and store it as part of the response
            String image = data.getString(ConstantUtil.CADDISFLY_IMAGE);

            Timber.d("caddisflyTestComplete - Response: %s . Image: %s", mValue, image);

            File src = !TextUtils.isEmpty(image) ? new File(image) : null;
            if (src != null && src.exists() && !src.isDirectory()) {
                presenter.onImageReady(src);
            } else {
                captureResponse();
            }
            displayResponseView();
        }
    }

    @Override
    public void onClick(View view) {
        if (storagePermissionsHelper.isStorageAllowed()) {
            launchCaddisflyTest();
        } else {
            requestStoragePermissions();
        }
    }

    private void requestStoragePermissions() {
        final FormActivity activity = (FormActivity) getContext();
        activity.requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                ConstantUtil.STORAGE_PERMISSION_CODE, getQuestion().getQuestionId());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == ConstantUtil.STORAGE_PERMISSION_CODE) {
            if (storagePermissionsHelper.storagePermissionsGranted(permissions, grantResults)) {
                launchCaddisflyTest();
            } else {
                storagePermissionNotGranted();
            }
        }
    }

    private void storagePermissionNotGranted() {
        final View.OnClickListener retryListener = v -> {
            if (storagePermissionsHelper
                    .userPressedDoNotShowAgain((FormActivity) getContext())) {
                navigator.navigateToAppSystemSettings(getContext());
            } else {
                requestStoragePermissions();
            }
        };
        snackBarManager
                .displaySnackBarWithAction(this,
                        R.string.storage_permission_missing,
                        R.string.action_retry, retryListener, getContext());
    }

    private void launchCaddisflyTest() {
        Question q = getQuestion();
        Bundle data = new Bundle();
        data.putString(ConstantUtil.CADDISFLY_RESOURCE_ID, q.getCaddisflyRes());
        data.putString(ConstantUtil.CADDISFLY_QUESTION_ID, q.getQuestionId());
        data.putString(ConstantUtil.CADDISFLY_QUESTION_TITLE, q.getText());
        data.putString(ConstantUtil.CADDISFLY_DATAPOINT_ID, mSurveyListener.getDataPointId());
        data.putString(ConstantUtil.CADDISFLY_FORM_ID, mSurveyListener.getFormId());
        data.putString(ConstantUtil.CADDISFLY_LANGUAGE, Locale.getDefault().getLanguage());
        String serverBase = BuildConfig.SERVER_BASE;
        serverBase = serverBase.replaceFirst("https://", "");
        serverBase = serverBase.replaceFirst("http://", "");
        serverBase = serverBase.replace(".appspot.com", "");
        data.putString(ConstantUtil.CADDISFLY_INSTANCE_NAME, serverBase);
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
