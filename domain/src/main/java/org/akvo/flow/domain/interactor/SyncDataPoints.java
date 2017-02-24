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
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.domain.util.ConnectivityStateManager;

import java.util.Map;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

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
        if (parameters == null || !parameters.containsKey(KEY_SURVEY_GROUP_ID)) {
            return Observable.error(new IllegalArgumentException("missing survey group id"));
        }
        if (!connectivityStateManager.isConnectionAvailable()) {
            return Observable.just(new SyncResult(SyncResult.ResultCode.ERROR_NO_NETWORK, 0));
        }
        return userRepository.mobileSyncAllowed().concatMap(new Func1<Boolean,
                Observable<SyncResult>>() {
            @Override
            public Observable<SyncResult> call(Boolean syncAllowed) {
                if (!syncAllowed && !connectivityStateManager.isWifiConnected()) {
                    return Observable.just(new SyncResult(
                            SyncResult.ResultCode.ERROR_SYNC_NOT_ALLOWED_OVER_3G, 0));
                } else {
                    return surveyRepository.syncRemoteDataPoints(
                            (Long) parameters.get(KEY_SURVEY_GROUP_ID)).map(
                            new Func1<Integer, SyncResult>() {
                                @Override
                                public SyncResult call(Integer integer) {
                                    return new SyncResult(SyncResult.ResultCode.SUCCESS, integer);
                                }
                            });
                }
            }
        });
    }
}
