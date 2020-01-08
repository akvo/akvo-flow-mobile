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

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class DeleteSurvey extends UseCase {

    public static final String SURVEY_ID_PARAM = "survey_id";

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;

    @Inject
    protected DeleteSurvey(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, SurveyRepository surveyRepository,
            UserRepository userRepository) {
        super(threadExecutor, postExecutionThread);
        this.surveyRepository = surveyRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || parameters.get(SURVEY_ID_PARAM) == null) {
            return Observable.error(new IllegalArgumentException("params missing"));
        }
        final long surveyToDeleteId = (Long) parameters.get(SURVEY_ID_PARAM);
        return userRepository.getSelectedSurvey().concatMap(new Function<Long, Observable<?>>() {
            @Override
            public Observable<?> apply(Long currentSelectedSurvey) {
                if (currentSelectedSurvey == surveyToDeleteId) {
                    return userRepository.clearSelectedSurvey().concatMap(
                            new Function<Boolean, Observable<?>>() {
                                @Override
                                public Observable<?> apply(Boolean aBoolean) {
                                    return deleteSurvey(surveyToDeleteId);
                                }
                            });
                }
                return deleteSurvey(surveyToDeleteId);
            }
        });
    }

    private Observable<Boolean> deleteSurvey(long surveyToDeleteId) {
        return surveyRepository.deleteSurvey(surveyToDeleteId);
    }
}
