/*
 * Copyright (C) 2016-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.injector.module;

import org.akvo.flow.domain.MobileUploadAllowed;
import org.akvo.flow.domain.interactor.ClearAllData;
import org.akvo.flow.domain.interactor.ClearResponses;
import org.akvo.flow.domain.interactor.CopyFile;
import org.akvo.flow.domain.interactor.CopyVideo;
import org.akvo.flow.domain.interactor.DeleteSurvey;
import org.akvo.flow.domain.interactor.GetAllSurveys;
import org.akvo.flow.domain.interactor.GetIsDeviceSetUp;
import org.akvo.flow.domain.interactor.GetPublishDataTime;
import org.akvo.flow.domain.interactor.datapoints.GetSavedDataPoints;
import org.akvo.flow.domain.interactor.GetUserSettings;
import org.akvo.flow.domain.interactor.MobileUploadSet;
import org.akvo.flow.domain.interactor.PublishData;
import org.akvo.flow.domain.interactor.SaveAppLanguage;
import org.akvo.flow.domain.interactor.SaveEnableMobileData;
import org.akvo.flow.domain.interactor.SaveImage;
import org.akvo.flow.domain.interactor.SaveImageSize;
import org.akvo.flow.domain.interactor.SaveKeepScreenOn;
import org.akvo.flow.domain.interactor.SaveResizedImage;
import org.akvo.flow.domain.interactor.SaveSelectedSurvey;
import org.akvo.flow.domain.interactor.SurveyDeviceNotifications;
import org.akvo.flow.domain.interactor.UnSyncedTransmissionsExist;
import org.akvo.flow.domain.interactor.UploadSurveyDataPoints;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.interactor.apk.GetApkData;
import org.akvo.flow.domain.interactor.apk.GetApkDataPreferences;
import org.akvo.flow.domain.interactor.apk.SaveApkUpdateNotified;
import org.akvo.flow.domain.interactor.forms.DownloadForm;
import org.akvo.flow.domain.interactor.forms.ReloadForms;
import org.akvo.flow.domain.interactor.setup.SaveSetup;
import org.akvo.flow.domain.interactor.users.CreateUser;
import org.akvo.flow.domain.interactor.users.DeleteUser;
import org.akvo.flow.domain.interactor.users.EditUser;
import org.akvo.flow.domain.interactor.users.GetSelectedUser;
import org.akvo.flow.domain.interactor.users.GetUsers;
import org.akvo.flow.domain.interactor.users.SelectUser;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

@Module
public class ViewModule {

    @Provides
    @Named("saveImage")
    UseCase provideSaveImageUseCase(SaveImage saveImage) {
        return saveImage;
    }

    @Provides
    @Named("getSavedDataPoints")
    UseCase provideGetSavedDataPointsUseCase(GetSavedDataPoints getSavedDataPoints) {
        return getSavedDataPoints;
    }

    @Provides
    @Named("getUserSettings")
    UseCase provideGetUserSettings(GetUserSettings getUserSettings) {
        return getUserSettings;
    }

    @Provides
    @Named("saveAppLanguage")
    UseCase provideSaveAppLanguage(SaveAppLanguage saveAppLanguage) {
        return saveAppLanguage;
    }

    @Provides
    @Named("saveEnableMobileData")
    UseCase provideSaveEnableMobileData(SaveEnableMobileData saveEnableMobileData) {
        return saveEnableMobileData;
    }

    @Provides
    @Named("saveImageSize")
    UseCase provideSaveImageSize(SaveImageSize saveImageSize) {
        return saveImageSize;
    }

    @Provides
    @Named("saveKeepScreenOn")
    UseCase provideSaveKeepScreenOn(SaveKeepScreenOn saveKeepScreenOn) {
        return saveKeepScreenOn;
    }

    @Provides
    @Named("getAllSurveys")
    UseCase provideGetAllSurveys(GetAllSurveys getAllSurveys) {
        return getAllSurveys;
    }

    @Provides
    @Named("deleteSurvey")
    UseCase provideDeleteSurvey(DeleteSurvey deleteSurvey) {
        return deleteSurvey;
    }

    @Provides
    @Named("saveSelectedSurvey")
    UseCase provideSaveSelectedSurvey(SaveSelectedSurvey saveSelectedSurvey) {
        return saveSelectedSurvey;
    }

    @Provides
    @Named("getUsers")
    UseCase provideGetUsers(GetUsers getUsers) {
        return getUsers;
    }

    @Provides
    @Named("editUser")
    UseCase provideEditUser(EditUser editUser) {
        return editUser;
    }

    @Provides
    @Named("deleteUser")
    UseCase provideDeleteUser(DeleteUser deleteUser) {
        return deleteUser;
    }

    @Provides
    @Named("selectUser")
    UseCase provideSelectedUser(SelectUser selectUser) {
        return selectUser;
    }

    @Provides
    @Named("createUser")
    UseCase provideCreateUser(CreateUser createUser) {
        return createUser;
    }

    @Provides
    @Named("getSelectedUser")
    UseCase provideGetSelectedUser(GetSelectedUser getSelectedUser) {
        return getSelectedUser;
    }

    @Provides
    @Named("copyResizedImage")
    UseCase provideSaveResizedImage(SaveResizedImage saveResizedImage) {
        return saveResizedImage;
    }

    @Provides
    @Named("copyVideo")
    UseCase provideCopyVideo(CopyVideo copyVideo) {
        return copyVideo;
    }

    @Provides
    @Named("getPublishDataTime")
    UseCase provideGetPublishDataTime(GetPublishDataTime getPublishDataTime) {
        return getPublishDataTime;
    }

    @Provides
    @Named("publishData")
    UseCase provideMakeDataPublic(PublishData publishData) {
        return publishData;
    }

    @Provides
    @Named("unSyncedTransmissionsExist")
    UseCase provideUnSyncedTransmissionsExist(UnSyncedTransmissionsExist transmissionsExist) {
        return transmissionsExist;
    }

    @Provides
    @Named("clearResponses")
    UseCase provideClearResponses(ClearResponses clearResponses) {
        return clearResponses;
    }

    @Provides
    @Named("clearAllData")
    UseCase provideClearAllData(ClearAllData clearAllData) {
        return clearAllData;
    }

    @Provides
    @Named("getIsDeviceSetUp")
    UseCase provideIsDeviceSetup(GetIsDeviceSetUp isDeviceSetUp) {
        return isDeviceSetUp;
    }

    @Provides
    @Named("saveSetup")
    UseCase provideSaveConfig(SaveSetup saveSetup) {
        return saveSetup;
    }

    @Provides
    @Named("copyFile")
    UseCase provideCopyFile(CopyFile copyFile) {
        return copyFile;
    }

    @Provides
    @Named("uploadSync")
    UseCase provideUploadSync(UploadSurveyDataPoints uploadSurveyDataPoints) {
        return uploadSurveyDataPoints;
    }

    @Provides
    @Named("checkDeviceNotification")
    UseCase provideDeviceNotificationSync(SurveyDeviceNotifications surveyDeviceNotifications) {
        return surveyDeviceNotifications;
    }

    @Provides
    @Named("mobileUploadSet")
    UseCase provideMobileUploadSet(MobileUploadSet mobileUploadSet) {
        return mobileUploadSet;
    }

    @Provides
    @Named("mobileUploadAllowed")
    UseCase provideAllowedToConnect(MobileUploadAllowed mobileUploadAllowed) {
        return mobileUploadAllowed;
    }

    @Provides
    @Named("getApkData")
    UseCase provideGetApkData(GetApkData getApkData) {
        return getApkData;
    }

    @Provides
    @Named("GetApkDataPreferences")
    UseCase provideGetApkDataPreferences(GetApkDataPreferences getApkDataPreferences) {
        return getApkDataPreferences;
    }

    @Provides
    @Named("SaveApkUpdateNotified")
    UseCase provideSaveApkUpdateNotified(SaveApkUpdateNotified saveApkUpdateNotified) {
        return saveApkUpdateNotified;
    }

    @Provides
    @Named("downloadForm")
    UseCase provideDownloadForm(DownloadForm downloadForm) {
        return downloadForm;
    }

    @Provides
    @Named("reloadForms")
    UseCase provideReloadForms(ReloadForms reloadForms) {
        return reloadForms;
    }
}
