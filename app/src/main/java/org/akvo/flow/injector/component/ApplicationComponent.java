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

package org.akvo.flow.injector.component;

import android.content.Context;

import com.google.gson.Gson;
import com.squareup.sqlbrite2.BriteDatabase;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.broadcast.BootReceiver;
import org.akvo.flow.broadcast.DataTimeoutReceiver;
import org.akvo.flow.data.net.RestApi;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.FileRepository;
import org.akvo.flow.domain.repository.SetupRepository;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.injector.module.ApplicationModule;
import org.akvo.flow.injector.module.ViewModule;
import org.akvo.flow.presentation.BaseActivity;
import org.akvo.flow.service.BootstrapService;
import org.akvo.flow.service.DataFixService;
import org.akvo.flow.service.DataPointUploadService;
import org.akvo.flow.service.FileChangeTrackingService;
import org.akvo.flow.service.SurveyDownloadService;
import org.akvo.flow.service.UnPublishDataService;
import org.akvo.flow.util.logging.LoggingHelper;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        ApplicationModule.class, ViewModule.class
})
public interface ApplicationComponent {

    SurveyRepository surveyRepository();

    BriteDatabase provideDatabase();

    void inject(FlowApp app);

    void inject(BaseActivity baseActivity);

    Context context();

    LoggingHelper loggingHelper();

    ThreadExecutor threadExecutor();

    PostExecutionThread postExecutionThread();

    FileRepository fileRepository();

    UserRepository userRepository();

    SetupRepository setupRepository();

    Gson gson();

    RestApi restApi();

    void inject(FileChangeTrackingService fileChangeTrackingService);

    void inject(SurveyDownloadService surveyDownloadService);

    void inject(BootstrapService bootstrapService);

    void inject(DataFixService dataFixService);

    void inject(DataTimeoutReceiver dataTimeoutReceiver);

    void inject(BootReceiver bootReceiver);

    void inject(UnPublishDataService unPublishDataService);

    void inject(DataPointUploadService uploadService);
}
