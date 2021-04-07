/*
 * Copyright (C) 2016-2020 Stichting Akvo (Akvo Foundation)
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
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.squareup.sqlbrite2.BriteDatabase;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.broadcast.BootReceiver;
import org.akvo.flow.broadcast.DataTimeoutReceiver;
import org.akvo.flow.data.entity.time.TimeMapper;
import org.akvo.flow.database.SurveyLanguagesDataSource;
import org.akvo.flow.domain.executor.CoroutineDispatcher;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.SchedulerCreator;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.ApkRepository;
import org.akvo.flow.domain.repository.DataPointRepository;
import org.akvo.flow.domain.repository.FileRepository;
import org.akvo.flow.domain.repository.FormInstanceRepository;
import org.akvo.flow.domain.repository.FormRepository;
import org.akvo.flow.domain.repository.MissingAndDeletedRepository;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.TimeRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.injector.module.ApplicationModule;
import org.akvo.flow.injector.module.ViewModule;
import org.akvo.flow.service.ApkUpdateWorker;
import org.akvo.flow.service.bootstrap.BootstrapWorker;
import org.akvo.flow.service.DataFixWorker;
import org.akvo.flow.service.DataPointUploadWorker;
import org.akvo.flow.service.FileChangeTrackingWorker;
import org.akvo.flow.service.SurveyDownloadWorker;
import org.akvo.flow.service.UnPublishDataService;
import org.akvo.flow.service.time.TimeCheckWorker;
import org.akvo.flow.util.logging.LoggingHelper;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        ApplicationModule.class, ViewModule.class
})
public interface ApplicationComponent {

    SurveyRepository surveyRepository();

    FormInstanceRepository formInstanceRepository();

    BriteDatabase provideDatabase();

    ApkRepository apkRepository();

    Context context();

    LoggingHelper loggingHelper();

    ThreadExecutor threadExecutor();

    PostExecutionThread postExecutionThread();

    SchedulerCreator schedulerCreator();

    CoroutineDispatcher coroutineDispatcher();

    FileRepository fileRepository();

    UserRepository userRepository();

    FormRepository formRepository();

    DataPointRepository dataPointRepository();

    MissingAndDeletedRepository missingAndDeletedRepository();

    TimeRepository timeRepository();

    SQLiteOpenHelper openHelper();

    Gson gson();

    TimeMapper timeMapper();

    SurveyLanguagesDataSource provideSurveyLanguageDataSource();

    void inject(FileChangeTrackingWorker fileChangeTrackingWorker);

    void inject(SurveyDownloadWorker surveyDownloadWorker);

    void inject(BootstrapWorker bootstrapService);

    void inject(DataFixWorker dataFixWorker);

    void inject(DataTimeoutReceiver dataTimeoutReceiver);

    void inject(BootReceiver bootReceiver);

    void inject(UnPublishDataService unPublishDataService);

    void inject(DataPointUploadWorker dataPointUploadWorker);

    void inject(ApkUpdateWorker apkUpdateWorker);

    void inject(TimeCheckWorker apkUpdateWorker);

    void inject(FlowApp app);
}
