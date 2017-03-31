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

import java.util.Map;

import javax.inject.Inject;

import rx.Observable;

public class GetSavedDataPoints extends UseCase {

    public static final String KEY_SURVEY_GROUP_ID = "survey_group_id";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_ORDER_BY = "order_by";

    private final SurveyRepository surveyRepository;

    @Inject
    protected GetSavedDataPoints(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, SurveyRepository surveyRepository) {
        super(threadExecutor, postExecutionThread);
        this.surveyRepository = surveyRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || !parameters.containsKey(KEY_SURVEY_GROUP_ID)
                || parameters.get(KEY_SURVEY_GROUP_ID) == null) {
            return Observable.error(new IllegalArgumentException("Missing survey group id"));
        }
        Long surveyGroupId = (Long) parameters.get(KEY_SURVEY_GROUP_ID);
        Double latitude = (Double) parameters.get(KEY_LATITUDE);
        Double longitude = (Double) parameters.get(KEY_LONGITUDE);
        Integer orderBy = (Integer) parameters.get(KEY_ORDER_BY);
        return surveyRepository.getDataPoints(surveyGroupId, latitude, longitude, orderBy);
    }
}
