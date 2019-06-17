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

import org.akvo.flow.domain.repository.FileRepository;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;

/**
 * This is a single threaded UseCase to be used with IntentServices whose onHandleIntent method runs
 * on a worker thread
 */
public class MakeDataPrivate {

    private final FileRepository fileRepository;
    private final CompositeDisposable disposables;

    @Inject
    protected MakeDataPrivate(FileRepository fileRepository) {
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

    private Completable buildUseCaseObservable() {
        return fileRepository.moveFiles();
    }

    private void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }
}
