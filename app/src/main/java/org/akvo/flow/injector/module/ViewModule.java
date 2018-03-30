/*
 * Copyright (C) 2016-2018 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.domain.interactor.AllowedToConnect;
import org.akvo.flow.domain.interactor.CopyVideo;
import org.akvo.flow.domain.interactor.UnSyncedTransmissionsExist;
import org.akvo.flow.domain.interactor.users.CreateUser;
import org.akvo.flow.domain.interactor.DeleteSurvey;
import org.akvo.flow.domain.interactor.users.DeleteUser;
import org.akvo.flow.domain.interactor.users.EditUser;
import org.akvo.flow.domain.interactor.GetAllSurveys;
import org.akvo.flow.domain.interactor.GetPublishDataTime;
import org.akvo.flow.domain.interactor.GetSavedDataPoints;
import org.akvo.flow.domain.interactor.GetUserSettings;
import org.akvo.flow.domain.interactor.users.GetSelectedUser;
import org.akvo.flow.domain.interactor.users.GetUsers;
import org.akvo.flow.domain.interactor.MakeDataPublic;
import org.akvo.flow.domain.interactor.SaveAppLanguage;
import org.akvo.flow.domain.interactor.SaveEnableMobileData;
import org.akvo.flow.domain.interactor.SaveImage;
import org.akvo.flow.domain.interactor.SaveImageSize;
import org.akvo.flow.domain.interactor.SaveKeepScreenOn;
import org.akvo.flow.domain.interactor.SaveSelectedSurvey;
import org.akvo.flow.domain.interactor.users.SelectUser;
import org.akvo.flow.domain.interactor.SaveResizedImage;
import org.akvo.flow.domain.interactor.UseCase;

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
    @Named("allowedToConnect")
    UseCase provideAllowedToConnect(AllowedToConnect allowedToConnect) {
        return allowedToConnect;
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
    @Named("saveResizedImage")
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
    @Named("makeDataPublic")
    UseCase provideMakeDataPublic(MakeDataPublic makeDataPublic) {
        return makeDataPublic;
    }

    @Provides
    @Named("unSyncedTransmissionsExist")
    UseCase provideUnSyncedTransmissionsExist(UnSyncedTransmissionsExist transmissionsExist) {
        return transmissionsExist;
    }
}
