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
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.util.ConnectivityStateManager;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class DownloadDataPoints {

    public static final String KEY_SURVEY_GROUP_ID = "survey_group_id";

    private final ThreadExecutor threadExecutor;
    private final PostExecutionThread postExecutionThread;
    private final CompositeDisposable disposables;

    private final SurveyRepository surveyRepository;
    private final ConnectivityStateManager connectivityStateManager;

    @Inject
    protected DownloadDataPoints(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, SurveyRepository surveyRepository,
            ConnectivityStateManager connectivityStateManager) {
        this.threadExecutor = threadExecutor;
        this.postExecutionThread = postExecutionThread;
        this.disposables = new CompositeDisposable();
        this.surveyRepository = surveyRepository;
        this.connectivityStateManager = connectivityStateManager;
    }

    public <T> void execute(DefaultFlowableObserver<T> defaultFlowableObserver,
            ErrorComposable errorComposable, Map<String, Object> parameters) {
        Flowable flowable = this.buildUseCaseObservable(parameters)
                .subscribeOn(Schedulers.from(threadExecutor))
                .observeOn(postExecutionThread.getScheduler());
        this.disposables.add(flowable
                .subscribe(defaultFlowableObserver, errorComposable, defaultFlowableObserver));
    }

    protected <T> Flowable buildUseCaseObservable(final Map<String, T> parameters) {
        if (parameters == null || parameters.get(KEY_SURVEY_GROUP_ID) == null) {
            return Flowable.error(new IllegalArgumentException("Missing survey group id"));
        }
        if (!connectivityStateManager.isConnectionAvailable()) {
            return Flowable.just(new DownloadResult(DownloadResult.ResultCode.ERROR_NO_NETWORK, 0));
        }
        return syncDataPoints(parameters);
    }

    private <T> Flowable<DownloadResult> syncDataPoints(Map<String, T> parameters) {
        return surveyRepository.downloadDataPoints((Long) parameters.get(KEY_SURVEY_GROUP_ID))
                .map(new Function<Integer, DownloadResult>() {
                    @Override
                    public DownloadResult apply(Integer integer) {
                        return new DownloadResult(DownloadResult.ResultCode.SUCCESS, integer);
                    }
                })
                .onErrorResumeNext(new Function<Throwable, Flowable<DownloadResult>>() {
                    @Override
                    public Flowable<DownloadResult> apply(Throwable throwable) {
                        if (throwable instanceof AssignmentRequiredException) {
                            return Flowable.just(new DownloadResult(
                                    DownloadResult.ResultCode.ERROR_ASSIGNMENT_MISSING, 0));
                        }
                        return Flowable.error(throwable);
                    }
                });
    }

    public void dispose() {
        if (!disposables.isDisposed()) {
            disposables.clear();
        }
    }
}
