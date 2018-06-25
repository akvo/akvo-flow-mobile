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

import android.support.annotation.Nullable;

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;

import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Abstract class for a Use Case (Interactor in terms of Clean Architecture).
 * This use case allows to be run at the same thread as it was called (as with IntentServices whose
 * onHandleIntent method runs on a worker thread) or an executor and post execution thread can be
 * passed as prams.
 */
public abstract class ThreadAwareUseCase {

    @Nullable
    private final ThreadExecutor threadExecutor;

    @Nullable
    private final PostExecutionThread postExecutionThread;

    private final CompositeDisposable disposables;

    protected ThreadAwareUseCase(@Nullable ThreadExecutor threadExecutor,
            @Nullable PostExecutionThread postExecutionThread) {
        this.threadExecutor = threadExecutor;
        this.postExecutionThread = postExecutionThread;
        this.disposables = new CompositeDisposable();
    }

    /**
     * Builds an {@link Observable} which will be used when executing the current {@link UseCase}.
     */
    protected abstract <T> Observable buildUseCaseObservable(Map<String, T> parameters);

    @SuppressWarnings("unchecked")
    public <T> void execute(DisposableObserver<T> observer, Map<String, Object> parameters) {
        Observable<T> observable = buildUseCaseObservable(parameters);
        if (threadExecutor != null) {
            observable = observable.subscribeOn(Schedulers.from(threadExecutor));
        }
        if (postExecutionThread != null) {
            observable = observable.observeOn(postExecutionThread.getScheduler());
        }
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
}
