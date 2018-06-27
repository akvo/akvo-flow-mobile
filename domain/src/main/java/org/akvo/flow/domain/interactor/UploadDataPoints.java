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
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class UploadDataPoints extends UseCase {

    public static final String KEY_SURVEY_ID = "survey_id";

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;

    @Inject
    public UploadDataPoints(@Nullable ThreadExecutor threadExecutor,
            @Nullable PostExecutionThread postExecutionThread,
            SurveyRepository surveyRepository, UserRepository userRepository) {
        super(threadExecutor, postExecutionThread);
        this.surveyRepository = surveyRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        String surveyId = parameters != null ? (String) parameters.get(KEY_SURVEY_ID) : null;
        return surveyRepository.getFormIds(surveyId)
                .concatMap(new Function<List<String>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(final List<String> forms) {
                        return userRepository.getDeviceId()
                                .concatMap(new Function<String, Observable<Boolean>>() {
                                    @Override
                                    public Observable<Boolean> apply(final String deviceId) {
                                        return surveyRepository
                                                .downloadMissingAndDeleted(forms, deviceId)
                                                .concatMap(
                                                        new Function<Boolean, Observable<Boolean>>() {
                                                            @Override
                                                            public Observable<Boolean> apply(
                                                                    Boolean ignored) {
                                                                return surveyRepository
                                                                        .processTransmissions(deviceId);
                                                            }
                                                        });
                                    }
                                });
                    }
                });
    }
}
