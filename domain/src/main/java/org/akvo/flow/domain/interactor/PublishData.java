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

import org.akvo.flow.domain.exception.FullStorageException;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.FileRepository;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

public class PublishData extends UseCase {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;

    @Inject
    protected PublishData(ThreadExecutor threadExecutor, PostExecutionThread postExecutionThread,
            FileRepository fileRepository, UserRepository userRepository,
            SurveyRepository surveyRepository) {
        super(threadExecutor, postExecutionThread);
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.surveyRepository = surveyRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        return surveyRepository.getAllTransmissionFileNames()
                .concatMap(new Function<List<String>, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(List<String> fileNames) {
                        return fileRepository.publishFiles(fileNames)
                                .concatMap(new Function<Boolean, ObservableSource<Boolean>>() {
                                    @Override
                                    public ObservableSource<Boolean> apply(Boolean published) {
                                        if (published) {
                                            userRepository.setPublishDataTime();
                                        }
                                        return Observable.just(published);
                                    }
                                });
                    }
                })
                .onErrorResumeNext(new Function<Throwable, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(Throwable throwable) {
                        return cleanUpOnError(throwable);
                    }
                });

    }

    private ObservableSource<Boolean> cleanUpOnError(final Throwable throwable) {
        return fileRepository.unPublishData()
                .flatMap(new Function<Boolean, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(Boolean aBoolean) {
                        return userRepository.clearPublishDataTime()
                                .concatMap(new Function<Boolean, ObservableSource<Boolean>>() {
                                    @Override
                                    public ObservableSource<Boolean> apply(Boolean aBoolean) {
                                        return checkError(throwable);
                                    }
                                });
                    }
                });
    }

    private ObservableSource<Boolean> checkError(final Throwable throwable) {
        if (throwable instanceof IOException) {
            return fileRepository.isExternalStorageFull()
                    .concatMap(new Function<Boolean, ObservableSource<Boolean>>() {
                        @Override
                        public ObservableSource<Boolean> apply(Boolean full) {
                            if (full) {
                                return Observable.error(new FullStorageException(throwable));
                            }
                            return Observable.error(throwable);
                        }
                    });
        }
        return Observable.error(throwable);
    }
}
