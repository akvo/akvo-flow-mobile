/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.domain.interactor.offline;

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.UserRepository;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;

public class SaveSelectedOfflineArea {

    public static final String AREA_ID_PARAM = "area-id";

    private final ThreadExecutor threadExecutor;
    private final PostExecutionThread postExecutionThread;
    private final CompositeDisposable disposables;
    private final UserRepository userRepository;

    @Inject
    protected SaveSelectedOfflineArea(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, UserRepository userRepository) {
        this.threadExecutor = threadExecutor;
        this.postExecutionThread = postExecutionThread;
        this.disposables = new CompositeDisposable();
        this.userRepository = userRepository;
    }

    public void execute(DisposableCompletableObserver observer, Map<String, Object> parameters) {
        final Completable completable = buildUseCaseObservable(parameters)
                .subscribeOn(Schedulers.from(threadExecutor))
                .observeOn(postExecutionThread.getScheduler());
        addDisposable(completable.subscribeWith(observer));
    }

    public void dispose() {
        if (!disposables.isDisposed()) {
            disposables.clear();
        }
    }

    protected <T> Completable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || !parameters.containsKey(AREA_ID_PARAM)) {
            return Completable.error(new IllegalArgumentException("missing area id"));
        } else {
            long offlineAreaId = (Long) parameters.get(AREA_ID_PARAM);
            return userRepository.saveSelectedOfflineArea(offlineAreaId);
        }
    }

    private void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }
}
