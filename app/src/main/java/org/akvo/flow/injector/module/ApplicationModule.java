/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.injector.module;

import android.content.Context;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.executor.JobExecutor;
import org.akvo.flow.data.repository.ApkDataRepository;
import org.akvo.flow.data.repository.ExceptionDataRepository;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.ApkRepository;
import org.akvo.flow.domain.repository.ExceptionRepository;
import org.akvo.flow.thread.UIThread;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

    private final FlowApp application;

    public ApplicationModule(FlowApp application) {
        this.application = application;
    }

    @Provides
    @Singleton
    Context provideContext() {
        return application;
    }

    @Provides
    @Singleton
    ThreadExecutor provideThreadExecutor(JobExecutor jobExecutor) {
        return jobExecutor;
    }

    @Provides
    @Singleton
    PostExecutionThread providePostExecutionThread(UIThread uiThread) {
        return uiThread;
    }

    @Provides
    @Singleton
    ApkRepository provideApkRepository(ApkDataRepository apkDataRepository) {
        return apkDataRepository;
    }

    @Provides
    @Singleton
    ExceptionRepository provideExceptionRepository(ExceptionDataRepository exceptionDataRepository) {
        return exceptionDataRepository;
    }
}
