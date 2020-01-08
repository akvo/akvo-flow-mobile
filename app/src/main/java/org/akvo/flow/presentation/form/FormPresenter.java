/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.form;

import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.ExportSurveyInstance;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.observers.DisposableCompletableObserver;
import timber.log.Timber;

public class FormPresenter implements Presenter {

    private final ExportSurveyInstance exportSurveyInstance;
    private final UseCase mobileUploadSet;
    private final UseCase mobileUploadAllowed;

    private FormView view;

    @Inject
    public FormPresenter(ExportSurveyInstance exportSurveyInstance,
            @Named("mobileUploadSet") UseCase mobileUploadSet,
            @Named("mobileUploadAllowed") UseCase mobileUploadAllowed) {
        this.exportSurveyInstance = exportSurveyInstance;
        this.mobileUploadSet = mobileUploadSet;
        this.mobileUploadAllowed = mobileUploadAllowed;
    }

    @Override
    public void destroy() {
        exportSurveyInstance.dispose();
        mobileUploadSet.dispose();
        mobileUploadAllowed.dispose();
    }

    public void setView(FormView view) {
        this.view = view;
    }

    public void onSubmitPressed(final long surveyInstanceId) {
        mobileUploadSet.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onNext(Boolean mobileUploadSet) {
                if (!mobileUploadSet) {
                    view.showMobileUploadSetting(surveyInstanceId);
                } else {
                    exportInstance(surveyInstanceId);
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.showMobileUploadSetting(surveyInstanceId);
            }
        }, null);
    }

    private void exportInstance(long surveyInstanceId) {
        view.showLoading();
        Map<String, Object> params = new HashMap<>(2);
        params.put(ExportSurveyInstance.SURVEY_INSTANCE_ID_PARAM, surveyInstanceId);
        exportSurveyInstance.execute(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                view.hideLoading();
                checkConnectionSetting();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.hideLoading();
                view.showErrorExport();
            }
        }, params);
    }

    private void checkConnectionSetting() {
        mobileUploadAllowed.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.startSync(false);
                view.dismiss();
            }

            @Override
            public void onNext(Boolean isAllowed) {
                view.startSync(isAllowed);
                view.dismiss();
            }
        }, null);

    }
}
