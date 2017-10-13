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
import org.akvo.flow.domain.interactor.GetUserSettings;
import org.akvo.flow.domain.interactor.SaveAppLanguage;
import org.akvo.flow.domain.interactor.SaveEnableMobileData;
import org.akvo.flow.domain.interactor.GetSavedDataPoints;
import org.akvo.flow.domain.interactor.SaveImage;
import org.akvo.flow.domain.interactor.SaveImageSize;
import org.akvo.flow.domain.interactor.SaveKeepScreenOn;
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
}
