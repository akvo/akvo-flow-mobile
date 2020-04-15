/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.settings;

import org.akvo.flow.domain.entity.UserSettings;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.SaveEnableMobileData;
import org.akvo.flow.domain.interactor.SaveImageSize;
import org.akvo.flow.domain.interactor.SaveKeepScreenOn;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.interactor.forms.DownloadForm;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.util.logging.LoggingHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class PreferencePresenter implements Presenter {

    private final UseCase getUserSettings;
    private final UseCase saveEnableMobileData;
    private final UseCase saveImageSize;
    private final UseCase saveKeepScreenOn;
    private final UseCase unSyncedTransmissionsExist;
    private final UseCase clearResponses;
    private final UseCase clearAllData;
    private final UseCase downloadForm;
    private final UseCase reloadForms;
    private final ViewUserSettingsMapper mapper;
    private final LoggingHelper helper;

    private PreferenceView view;

    @Inject
    public PreferencePresenter(@Named("getUserSettings") UseCase getUserSettings,
            @Named("saveEnableMobileData") UseCase saveEnableMobileData,
            @Named("saveImageSize") UseCase saveImageSize,
            @Named("saveKeepScreenOn") UseCase saveKeepScreenOn,
            @Named("unSyncedTransmissionsExist") UseCase unSyncedTransmissionsExist,
            @Named("clearResponses") UseCase clearResponses,
            @Named("clearAllData") UseCase clearAllData,
            @Named("downloadForm") UseCase downloadForm,
            @Named("reloadForms") UseCase reloadForms,
            ViewUserSettingsMapper mapper, LoggingHelper helper) {
        this.getUserSettings = getUserSettings;
        this.saveEnableMobileData = saveEnableMobileData;
        this.saveImageSize = saveImageSize;
        this.saveKeepScreenOn = saveKeepScreenOn;
        this.unSyncedTransmissionsExist = unSyncedTransmissionsExist;
        this.clearResponses = clearResponses;
        this.clearAllData = clearAllData;
        this.downloadForm = downloadForm;
        this.reloadForms = reloadForms;
        this.mapper = mapper;
        this.helper = helper;
    }

    public void setView(PreferenceView view) {
        this.view = view;
    }

    public void loadPreferences() {
        view.showLoading();
        getUserSettings.execute(new DefaultObserver<UserSettings>() {
            @Override
            public void onNext(UserSettings userSettings) {
                ViewUserSettings viewUserSettings = mapper
                        .transform(userSettings);
                view.hideLoading();
                view.displaySettings(viewUserSettings);
            }
        }, null);
    }

    public void saveEnableMobileData(boolean enable) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(SaveEnableMobileData.PARAM_ENABLE_MOBILE_DATA, enable);
        saveEnableMobileData.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }
        }, params);
    }

    public void saveImageSize(int size) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(SaveImageSize.PARAM_IMAGE_SIZE, size);
        saveImageSize.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }
        }, params);
    }

    public void saveKeepScreenOn(boolean enable) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(SaveKeepScreenOn.PARAM_KEEP_SCREEN_ON, enable);
        saveKeepScreenOn.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }
        }, params);
    }

    @Override
    public void destroy() {
        getUserSettings.dispose();
        saveEnableMobileData.dispose();
        saveImageSize.dispose();
        saveKeepScreenOn.dispose();
        unSyncedTransmissionsExist.dispose();
        clearAllData.dispose();
        clearResponses.dispose();
        downloadForm.dispose();
        reloadForms.dispose();
    }

    public void deleteCollectedData() {
        unSyncedTransmissionsExist.execute(new DefaultObserver<Boolean>(){
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.showDeleteCollectedData();
            }

            @Override
            public void onNext(Boolean exist) {
                if (exist != null && exist) {
                    view.showDeleteCollectedDataWithPending();
                } else {
                    view.showDeleteCollectedData();
                }
            }
        }, null);
    }

    public void deleteAllData() {
        unSyncedTransmissionsExist.execute(new DefaultObserver<Boolean>(){
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.showDeleteAllData();
            }

            @Override
            public void onNext(Boolean exist) {
                if (exist != null && exist) {
                    view.showDeleteAllDataWithPending();
                } else {
                    view.showDeleteAllData();
                }
            }
        }, null);
    }

    public void deleteResponsesConfirmed() {
        clearResponses.execute(new ClearDataObserver(), null);
    }

    public void deleteAllConfirmed() {
        helper.clearUser();
        clearAllData.execute(new ClearDataObserver(), null);
    }

    public void downloadForm(String formId) {
        view.showLoading();
        Map<String, Object> params = new HashMap<>(2);
        params.put(DownloadForm.FORM_ID_PARAM, formId);
        downloadForm.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.hideLoading();
                view.showDownloadFormsError(1);
            }

            @Override
            public void onNext(Boolean aBoolean) {
                view.hideLoading();
                view.showDownloadFormsSuccess(1);
            }
        }, params);
    }

    public void reloadForms() {
        view.showLoading();
        reloadForms.execute(new DefaultObserver<Integer>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.hideLoading();
                view.showDownloadFormsError(5); //random number for now
            }

            @Override
            public void onNext(Integer numberOfForms) {
                view.hideLoading();
                view.showDownloadFormsSuccess(numberOfForms);
            }
        }, null);
    }

    private class ClearDataObserver extends DefaultObserver<Boolean> {
        @Override
        public void onError(Throwable e) {
            view.showClearDataError();
        }

        @Override
        public void onNext(Boolean cleared) {
            if (cleared) {
                view.showClearDataSuccess();
                view.dismiss();
            } else {
                view.showClearDataError();
            }
        }
    }
}
