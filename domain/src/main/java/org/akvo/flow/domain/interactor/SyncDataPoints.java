/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.domain.entity.SyncResult;
import org.akvo.flow.domain.exception.AssignmentRequiredException;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.domain.util.ConnectivityStateManager;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class SyncDataPoints extends UseCase {

    public static final String KEY_SURVEY_GROUP_ID = "survey_group_id";

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;
    private final ConnectivityStateManager connectivityStateManager;

    @Inject
    protected SyncDataPoints(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, SurveyRepository surveyRepository,
            UserRepository userRepository, ConnectivityStateManager connectivityStateManager) {
        super(threadExecutor, postExecutionThread);
        this.surveyRepository = surveyRepository;
        this.userRepository = userRepository;
        this.connectivityStateManager = connectivityStateManager;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(final Map<String, T> parameters) {
        if (parameters == null || parameters.get(KEY_SURVEY_GROUP_ID) == null) {
            return Observable.error(new IllegalArgumentException("Missing survey group id"));
        }
        if (!connectivityStateManager.isConnectionAvailable()) {
            return Observable.just(new SyncResult(SyncResult.ResultCode.ERROR_NO_NETWORK, 0));
        }
        return userRepository.mobileSyncAllowed().concatMap(new Function<Boolean,
                        Observable<SyncResult>>() {
            @Override
            public Observable<SyncResult> apply(Boolean syncAllowed) {
                if (!syncAllowed && !connectivityStateManager.isWifiConnected()) {
                    return Observable.just(new SyncResult(
                            SyncResult.ResultCode.ERROR_SYNC_NOT_ALLOWED_OVER_3G, 0));
                } else {
                    return syncDataPoints(parameters);
                }
            }
        });
    }

    private <T> Observable<SyncResult> syncDataPoints(Map<String, T> parameters) {
        return surveyRepository.syncRemoteDataPoints((Long) parameters.get(KEY_SURVEY_GROUP_ID))
                .map(new Function<Integer, SyncResult>() {
                    @Override
                    public SyncResult apply(Integer integer) {
                        return new SyncResult(SyncResult.ResultCode.SUCCESS, integer);
                    }
                })
                .onErrorResumeNext(new Function<Throwable, Observable<SyncResult>>() {
                    @Override
                    public Observable<SyncResult> apply(Throwable throwable) {
                        if (throwable instanceof AssignmentRequiredException) {
                            return Observable.just(new SyncResult(
                                    SyncResult.ResultCode.ERROR_ASSIGNMENT_MISSING, 0));
                        }
                        return Observable.error(throwable);
                    }
                });
    }
}
