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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class ExportSurveyInstances extends UseCase {

    private final UserRepository userRepository;
    private final TextValueCleaner valueCleaner;
    private final SurveyRepository surveyRepository;
    private final FileRepository fileRepository;

    @Inject
    protected ExportSurveyInstances(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, UserRepository userRepository,
            TextValueCleaner valueCleaner,
            SurveyRepository surveyRepository,
            FileRepository fileRepository) {
        super(threadExecutor, postExecutionThread);
        this.userRepository = userRepository;
        this.valueCleaner = valueCleaner;
        this.surveyRepository = surveyRepository;
        this.fileRepository = fileRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
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
                        return surveyRepository.getPendingSurveyInstances()
                                .concatMap(new Function<List<Long>, Observable<Boolean>>() {
                                    @Override
                                    public Observable<Boolean> apply(List<Long> instanceIds) {
                                        return Observable.fromIterable(instanceIds)
                                                .concatMap(
                                                        new Function<Long, Observable<Boolean>>() {
                                                            @Override
                                                            public Observable<Boolean> apply(
                                                                    Long instanceId) {
                                                                return createInstanceZipFile(
                                                                        instanceId, deviceId);
                                                            }
                                                        });
                                    }
                                });
                    }
                });
    }

    private Observable<Boolean> createInstanceZipFile(final Long instanceId, String deviceId) {
        return surveyRepository.getFormInstanceData(instanceId, deviceId)
                .concatMap(new Function<FormInstanceMetadata, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(
                            final FormInstanceMetadata formInstanceMetadata) {
                        return fileRepository.createDataZip(formInstanceMetadata.getZipFileName(),
                                formInstanceMetadata.getFormInstanceData())
                                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                                    @Override
                                    public Observable<Boolean> apply(Boolean ignored) {
                                        Set<String> filenames = new HashSet<>(
                                                formInstanceMetadata.getMediaFileNames());
                                        filenames.add(formInstanceMetadata.getZipFileName());
                                        return surveyRepository.createTransmissions(instanceId,
                                                formInstanceMetadata.getFormId(), filenames);
                                    }
                                });
                    }
                });
    }
}
