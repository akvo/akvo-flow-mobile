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

import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;

/**
 * This is a single threaded UseCase to be used with IntentServices and GcmTaskService whose
 * onHandleIntent or onRunTask methods runs on a worker thread
 */
public abstract class SingleThreadUseCase {

    private final CompositeDisposable disposables;

    protected SingleThreadUseCase() {
        this.disposables = new CompositeDisposable();
    }

    @SuppressWarnings("unchecked")
    public <T> void execute(DisposableObserver<T> observer, final Map<String, String> parameters) {
        addDisposable(((Observable<T>) buildUseCaseObservable(parameters)).subscribeWith(observer));
    }

    public void dispose() {
        if (!disposables.isDisposed()) {
            disposables.clear();
        }
    }

    protected abstract <T> Observable buildUseCaseObservable(Map<String, T> parameters);

    private void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }
}
