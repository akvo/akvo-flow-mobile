/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.domain.entity.DownloadResult;
import org.akvo.flow.domain.exception.AssignmentRequiredException;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.DataPointRepository;
import org.akvo.flow.domain.util.ConnectivityStateManager;

import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class DownloadDataPoints {

    public static final String KEY_SURVEY_ID = "survey_id";

    private final ThreadExecutor threadExecutor;
    private final PostExecutionThread postExecutionThread;
    private final CompositeDisposable disposables;

    private final DataPointRepository dataPointRepository;
    private final ConnectivityStateManager connectivityStateManager;

    @Inject
    protected DownloadDataPoints(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, DataPointRepository dataPointRepository,
            ConnectivityStateManager connectivityStateManager) {
        this.threadExecutor = threadExecutor;
        this.postExecutionThread = postExecutionThread;
        this.disposables = new CompositeDisposable();
        this.dataPointRepository = dataPointRepository;
        this.connectivityStateManager = connectivityStateManager;
    }

    public void execute(DisposableSingleObserver<DownloadResult> singleObserver,
            Map<String, Object> parameters) {
        addDisposable(buildUseCaseObservable(parameters)
                .subscribeOn(Schedulers.from(threadExecutor))
                .observeOn(postExecutionThread.getScheduler())
                .subscribeWith(singleObserver));
    }

    public void dispose() {
        if (!disposables.isDisposed()) {
            disposables.clear();
        }
    }

    protected Single<DownloadResult> buildUseCaseObservable(final Map<String, Object> parameters) {
        if (parameters == null || parameters.get(KEY_SURVEY_ID) == null) {
            return Single.error(new IllegalArgumentException("Missing survey group id"));
        }
        if (!connectivityStateManager.isConnectionAvailable()) {
            return Single.just(new DownloadResult(DownloadResult.ResultCode.ERROR_NO_NETWORK, 0));
        }
        return syncDataPoints(parameters);
    }

    private Single<DownloadResult> syncDataPoints(@NonNull Map<String, Object> parameters) {
        return dataPointRepository.downloadDataPoints((Long) parameters.get(KEY_SURVEY_ID))
                .map(integer -> new DownloadResult(DownloadResult.ResultCode.SUCCESS, integer))
                .onErrorResumeNext((Function<Throwable, Single<DownloadResult>>) throwable -> {
                    if (throwable instanceof AssignmentRequiredException) {
                        return Single.just(new DownloadResult(
                                DownloadResult.ResultCode.ERROR_ASSIGNMENT_MISSING, 0));
                    }
                    return Single.error(throwable);
                });
    }

    private void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }
}
