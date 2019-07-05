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

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableCompletableObserver;
import org.akvo.flow.domain.entity.InstanceIdUuid;
import org.akvo.flow.domain.repository.FileRepository;
import org.akvo.flow.domain.repository.SurveyRepository;
import timber.log.Timber;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

public class CheckSubmittedFiles {

    private final SurveyRepository surveyRepository;
    private final FileRepository fileRepository;
    private final CompositeDisposable disposables;

    @Inject
    protected CheckSubmittedFiles(SurveyRepository surveyRepository,
            FileRepository fileRepository) {
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

    private void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }

    private Completable buildUseCaseObservable() {
        return surveyRepository.getSubmittedInstances()
                .flatMapCompletable(new Function<List<InstanceIdUuid>, CompletableSource>() {
                    @Override
                    public CompletableSource apply(List<InstanceIdUuid> instanceIdUuids) {
                        return checkExistingFiles(instanceIdUuids);
                    }
                });
    }

    private Completable checkExistingFiles(List<InstanceIdUuid> instanceIdUuids) {
        return Observable.fromIterable(instanceIdUuids)
                .flatMapCompletable(new Function<InstanceIdUuid, Completable>() {
                    @Override
                    public Completable apply(InstanceIdUuid instanceIdUuid) {
                        return updateMissingFileInstances(instanceIdUuid);
                    }
                });
    }

    private Completable updateMissingFileInstances(final InstanceIdUuid instanceIdUuid) {
        return fileRepository.getInstancesWithIncorrectZip(instanceIdUuid)
                .flatMapCompletable(new Function<File, Completable>() {
                    @Override
                    public Completable apply(File file) {
                        return updateInstanceStatus(instanceIdUuid);
                    }
                });
    }

    private Completable updateInstanceStatus(InstanceIdUuid instanceIdUuid) {
        long surveyInstanceId = instanceIdUuid.getId();
        Timber.d("Exported file for survey instance %s not found. It's status " +
                        "will be set to 'submit requested', and will be reprocessed",
                surveyInstanceId);
        return surveyRepository.setInstanceStatusToRequested(surveyInstanceId);
    }
}
