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

import androidx.annotation.VisibleForTesting;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableCompletableObserver;
import org.akvo.flow.domain.entity.FormInstanceMetadata;
import org.akvo.flow.domain.repository.FileRepository;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.domain.util.TextValueCleaner;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public void execute(DisposableCompletableObserver observer) {
        final Completable observable = buildUseCaseObservable();
        addDisposable(observable.subscribeWith(observer));
    }

    public void dispose() {
        if (!disposables.isDisposed()) {
            disposables.clear();
        }
    }

    @VisibleForTesting
    Completable buildUseCaseObservable() {
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
                        return createInstancesZipFiles(deviceId);
                    }
                });
    }

    private void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }

    private Completable createInstancesZipFiles(final String deviceId) {
        return surveyRepository.getPendingSurveyInstances()
                .flatMapCompletable(new Function<List<Long>, CompletableSource>() {
                    @Override
                    public CompletableSource apply(List<Long> instanceIds) {
                        return Observable.fromIterable(instanceIds)
                                .flatMapCompletable(new Function<Long, CompletableSource>() {
                                    @Override
                                    public CompletableSource apply(Long instanceId) {
                                        return createTransmissions(instanceId, deviceId);
                                    }
                                });
                    }
                });
    }

    private Completable createTransmissions(@NonNull final Long instanceId,
            final String deviceId) {
        return surveyRepository.setInstanceStatusToRequested(instanceId)
                .andThen(surveyRepository.getFormInstanceData(instanceId, deviceId)
                        .flatMapCompletable(
                                new Function<FormInstanceMetadata, CompletableSource>() {
                                    @Override
                                    public CompletableSource apply(FormInstanceMetadata metadata) {
                                        return createTransmissions(metadata, instanceId);
                                    }
                                }));
    }

    private Completable createTransmissions(final FormInstanceMetadata metadata,
            @NonNull final Long instanceId) {
        return fileRepository
                .createDataZip(metadata.getZipFileName(), metadata.getFormInstanceData())
                .andThen(insertToDatabase(metadata, instanceId));
    }

    private Completable insertToDatabase(FormInstanceMetadata metadata,
            @NonNull Long instanceId) {
        Set<String> filenames = new HashSet<>(metadata.getMediaFileNames());
        filenames.add(metadata.getZipFileName());
        return surveyRepository
                .createTransmissions(instanceId, metadata.getFormId(), filenames);
    }
}
