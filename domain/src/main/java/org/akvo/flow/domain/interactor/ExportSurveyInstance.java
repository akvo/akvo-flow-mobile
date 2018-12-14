/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

public class ExportSurveyInstance extends UseCase {

    public static final String SURVEY_INSTANCE_ID_PARAM = "survey_instance_id";

    private final UserRepository userRepository;
    private final TextValueCleaner valueCleaner;
    private final SurveyRepository surveyRepository;
    private final FileRepository fileRepository;

    @Inject
    protected ExportSurveyInstance(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, UserRepository userRepository,
            TextValueCleaner valueCleaner, SurveyRepository surveyRepository,
            FileRepository fileRepository) {
        super(threadExecutor, postExecutionThread);
        this.userRepository = userRepository;
        this.valueCleaner = valueCleaner;
        this.surveyRepository = surveyRepository;
        this.fileRepository = fileRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || parameters.get(SURVEY_INSTANCE_ID_PARAM) == null)  {
            return Observable.error(new IllegalArgumentException("Missing survey instance id"));
        }
        final Long surveyInstanceId = (Long) parameters.get(SURVEY_INSTANCE_ID_PARAM);
        return userRepository.getDeviceId()
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String deviceId) {
                        return valueCleaner.cleanVal(deviceId);
                    }
                })
                .flatMap(new Function<String, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(final String deviceId) {
                        return createInstanceZipFile(surveyInstanceId, deviceId);
                    }
                });
    }

    private Observable<Boolean> createInstanceZipFile(@NonNull final Long instanceId,
            final String deviceId) {
        return surveyRepository.setInstanceStatusToRequested(instanceId)
                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(Boolean aBoolean) {
                        return surveyRepository.getFormInstanceData(instanceId, deviceId)
                                .concatMap(
                                        new Function<FormInstanceMetadata, Observable<Boolean>>() {
                                            @Override
                                            public Observable<Boolean> apply(
                                                    final FormInstanceMetadata metadata) {
                                                return exportSurveyInstance(metadata, instanceId);
                                            }
                                        });
                    }
                });
    }

    private Observable<Boolean> exportSurveyInstance(final FormInstanceMetadata metadata,
            @NonNull final Long instanceId) {
        return fileRepository.createDataZip(metadata.getZipFileName(), metadata.getFormInstanceData())
                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(Boolean ignored) {
                        Set<String> filenames = new HashSet<>(metadata.getMediaFileNames());
                        filenames.add(metadata.getZipFileName());
                        return surveyRepository
                                .createTransmissions(instanceId, metadata.getFormId(), filenames);
                    }
                });
    }
}
