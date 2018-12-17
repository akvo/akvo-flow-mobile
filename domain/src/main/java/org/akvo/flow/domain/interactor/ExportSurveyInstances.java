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
import org.akvo.flow.domain.repository.FileRepository;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.domain.util.TextValueCleaner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;

public class ExportSurveyInstances {

    private final UserRepository userRepository;
    private final TextValueCleaner valueCleaner;
    private final SurveyRepository surveyRepository;
    private final FileRepository fileRepository;
    private final CompositeDisposable disposables;

    @Inject
    protected ExportSurveyInstances(UserRepository userRepository,
            TextValueCleaner valueCleaner, SurveyRepository surveyRepository,
            FileRepository fileRepository) {
        this.userRepository = userRepository;
        this.valueCleaner = valueCleaner;
        this.surveyRepository = surveyRepository;
        this.fileRepository = fileRepository;
        this.disposables = new CompositeDisposable();
    }

    @SuppressWarnings("unchecked")
    public <T> void execute(DisposableObserver<T> observer) {
        final Observable<T> observable = buildUseCaseObservable();
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

    private Observable buildUseCaseObservable() {
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
                        return createInstancesZipFiles(deviceId);
                    }
                });
    }

    private Observable<Boolean> createInstancesZipFiles(final String deviceId) {
        return surveyRepository.getPendingSurveyInstances()
                .concatMap(new Function<List<Long>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(List<Long> instanceIds) {
                        return Observable.fromIterable(instanceIds)
                                .concatMap(new Function<Long, Observable<Boolean>>() {
                                    @Override
                                    public Observable<Boolean> apply(Long instanceId) {
                                        return getFormInstanceData(instanceId, deviceId);
                                    }
                                })
                                .toList()
                                .toObservable()
                                .map(new Function<List<Boolean>, Boolean>() {
                                    @Override
                                    public Boolean apply(List<Boolean> ignored) {
                                        return true;
                                    }
                                });
                    }
                });
    }

    private Observable<Boolean> getFormInstanceData(@NonNull final Long instanceId,
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
                                                return createTransmissions(metadata, instanceId);
                                            }
                                        });
                    }
                });
    }

    private Observable<Boolean> createTransmissions(final FormInstanceMetadata metadata,
            @NonNull final Long instanceId) {
        return fileRepository
                .createDataZip(metadata.getZipFileName(), metadata.getFormInstanceData())
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
