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

import org.akvo.flow.domain.entity.TransmissionResult;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.domain.util.Constants;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class UploadSurveyDataPoints extends UseCase {

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;

    @Inject
    protected UploadSurveyDataPoints(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, SurveyRepository surveyRepository,
            UserRepository userRepository) {
        super(threadExecutor, postExecutionThread);
        this.surveyRepository = surveyRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || parameters.get(Constants.KEY_SURVEY_ID) == null) {
            return Observable.error(new IllegalArgumentException("missing surveyId"));
        }
        final String surveyId = (String) parameters.get(Constants.KEY_SURVEY_ID);
        return userRepository.getDeviceId()
                .concatMap(new Function<String, Observable<Set<TransmissionResult>>>() {
                    @Override
                    public Observable<Set<TransmissionResult>> apply(final String deviceId) {
                        return surveyRepository.processTransmissions(deviceId, surveyId);
                    }
                });
    }
}
