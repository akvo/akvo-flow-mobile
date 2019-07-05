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

package org.akvo.flow.domain.interactor;

import org.akvo.flow.domain.entity.FormInstanceMetadata;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.FileRepository;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.domain.util.TextValueCleaner;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;

public class ExportSurveyInstance {

    public static final String SURVEY_INSTANCE_ID_PARAM = "survey_instance_id";

    private final UserRepository userRepository;
    private final TextValueCleaner valueCleaner;
    private final SurveyRepository surveyRepository;
    private final FileRepository fileRepository;
    private final ThreadExecutor threadExecutor;
    private final PostExecutionThread postExecutionThread;
    private final CompositeDisposable disposables;

    @Inject
    protected ExportSurveyInstance(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, UserRepository userRepository,
            TextValueCleaner valueCleaner, SurveyRepository surveyRepository,
            FileRepository fileRepository) {
        this.threadExecutor = threadExecutor;
        this.postExecutionThread = postExecutionThread;
        this.disposables = new CompositeDisposable();
        this.userRepository = userRepository;
        this.valueCleaner = valueCleaner;
        this.surveyRepository = surveyRepository;
        this.fileRepository = fileRepository;
    }

    public void execute(DisposableCompletableObserver observer, Map<String, Object> parameters) {
        final Completable observable = buildUseCaseObservable(parameters)
                .subscribeOn(Schedulers.from(threadExecutor))
                .observeOn(postExecutionThread.getScheduler());
        addDisposable(observable.subscribeWith(observer));
    }

    public void dispose() {
        if (!disposables.isDisposed()) {
            disposables.clear();
        }
    }

    private void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }

    protected <T> Completable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || parameters.get(SURVEY_INSTANCE_ID_PARAM) == null) {
            return Completable.error(new IllegalArgumentException("Missing survey instance id"));
        }
        final Long surveyInstanceId = (Long) parameters.get(SURVEY_INSTANCE_ID_PARAM);
        return userRepository.getDeviceId()
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String deviceId) {
                        return valueCleaner.cleanVal(deviceId);
                    }
                })
                .flatMapCompletable(new Function<String, Completable>() {
                    @Override
                    public Completable apply(final String deviceId) {
                        return createInstanceZipFile(surveyInstanceId, deviceId);
                    }
                });
    }

    private Completable createInstanceZipFile(@NonNull final Long instanceId,
            final String deviceId) {
        return surveyRepository.setInstanceStatusToRequested(instanceId)
                .andThen(surveyRepository.getFormInstanceData(instanceId, deviceId)
                                .flatMapCompletable(
                                        new Function<FormInstanceMetadata, CompletableSource>() {
                                            @Override
                                            public CompletableSource apply(
                                                    FormInstanceMetadata metadata) {
                                                return exportSurveyInstance(metadata, instanceId);
                                            }
                                        }));
    }

    private Completable exportSurveyInstance(final FormInstanceMetadata metadata,
            @NonNull final Long instanceId) {
        return fileRepository
                .createDataZip(metadata.getZipFileName(), metadata.getFormInstanceData())
                .andThen(insertToDataBase(metadata, instanceId));
    }

    private Completable insertToDataBase(FormInstanceMetadata metadata, @NonNull Long instanceId) {
        Set<String> filenames = new HashSet<>(metadata.getMediaFileNames());
        filenames.add(metadata.getZipFileName());
        return surveyRepository.createTransmissions(instanceId, metadata.getFormId(), filenames);
    }
}
