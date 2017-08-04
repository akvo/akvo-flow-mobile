/*
 * Copyright (C) 2016-2017 Stichting Akvo (Akvo Foundation)
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
import org.akvo.flow.domain.interactor.CreateUser;
import org.akvo.flow.domain.interactor.DeleteSurvey;
import org.akvo.flow.domain.interactor.DeleteUser;
import org.akvo.flow.domain.interactor.EditUser;
import org.akvo.flow.domain.interactor.GetAllSurveys;
import org.akvo.flow.domain.interactor.GetSavedDataPoints;
import org.akvo.flow.domain.interactor.GetUsers;
import org.akvo.flow.domain.interactor.SaveImage;
import org.akvo.flow.domain.interactor.SaveSelectedSurvey;
import org.akvo.flow.domain.interactor.SetSelectedUser;
import org.akvo.flow.domain.interactor.SyncDataPoints;
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
    @Named("syncDataPoints")
    UseCase provideSyncDataPointsUseCase(SyncDataPoints syncDataPoints) {
        return syncDataPoints;
    }

    @Provides
    @Named("allowedToConnect")
    UseCase provideAllowedToConnect(AllowedToConnect allowedToConnect) {
        return allowedToConnect;
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
    @Named("setSelectedUser")
    UseCase provideSelectedUser(SetSelectedUser selectedUser) {
        return selectedUser;
    }

    @Provides
    @Named("createUser")
    UseCase provideCreateUser(CreateUser createUser) {
        return createUser;
    }
}
