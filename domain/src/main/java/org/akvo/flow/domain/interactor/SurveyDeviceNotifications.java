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

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.MissingAndDeletedRepository;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.domain.util.Constants;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class SurveyDeviceNotifications extends UseCase {

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;
    private final MissingAndDeletedRepository missingAndDeletedRepository;

    @Inject
    protected SurveyDeviceNotifications(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, SurveyRepository surveyRepository,
            UserRepository userRepository,
            MissingAndDeletedRepository missingAndDeletedRepository) {
        super(threadExecutor, postExecutionThread);
        this.surveyRepository = surveyRepository;
        this.userRepository = userRepository;
        this.missingAndDeletedRepository = missingAndDeletedRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || parameters.get(Constants.KEY_SURVEY_ID) == null) {
            return Observable.error(new IllegalArgumentException("missing surveyId"));
        }
        final String surveyId = (String) parameters.get(Constants.KEY_SURVEY_ID);
        return userRepository.getDeviceId()
                .concatMap(new Function<String, Observable<Set<String>>>() {
                    @Override
                    public Observable<Set<String>> apply(final String deviceId) {
                        return surveyRepository.getFormIds(surveyId)
                                .concatMap(new Function<List<String>, Observable<Set<String>>>() {
                                    @Override
                                    public Observable<Set<String>> apply(List<String> strings) {
                                        return missingAndDeletedRepository
                                                .downloadMissingAndDeleted(strings, deviceId);
                                    }
                                });
                    }
                });
    }
}
