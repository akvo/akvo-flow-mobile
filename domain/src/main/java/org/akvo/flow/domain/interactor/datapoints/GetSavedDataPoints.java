/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.interactor.datapoints;

import android.text.TextUtils;

import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.repository.SurveyRepository;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class GetSavedDataPoints extends UseCase {

    public static final String KEY_SURVEY_GROUP_ID = "survey_group_id";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_ORDER_BY = "order_by";
    public static final String KEY_FILTER = "filter";

    private final SurveyRepository surveyRepository;

    @Inject
    protected GetSavedDataPoints(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, SurveyRepository surveyRepository) {
        super(threadExecutor, postExecutionThread);
        this.surveyRepository = surveyRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || parameters.get(KEY_SURVEY_GROUP_ID) == null) {
            return Observable.error(new IllegalArgumentException("Missing survey group id"));
        }
        Long surveyGroupId = (Long) parameters.get(KEY_SURVEY_GROUP_ID);
        Double latitude = (Double) parameters.get(KEY_LATITUDE);
        Double longitude = (Double) parameters.get(KEY_LONGITUDE);
        Integer orderBy = (Integer) parameters.get(KEY_ORDER_BY);
        final String filter = (String) parameters.get(KEY_FILTER);
        if (TextUtils.isEmpty(filter)) {
            return surveyRepository.getDataPoints(surveyGroupId, latitude, longitude, orderBy);
        } else {
            return getFilteredDataPoints(surveyGroupId, latitude, longitude, orderBy, filter);
        }
    }

    private Observable<List<DataPoint>> getFilteredDataPoints(Long surveyGroupId, Double latitude,
            Double longitude, Integer orderBy, @NonNull final String filter) {
        return surveyRepository.getDataPoints(surveyGroupId, latitude, longitude, orderBy)
                .flatMap(new Function<List<DataPoint>, ObservableSource<List<DataPoint>>>() {
                    @Override
                    public ObservableSource<List<DataPoint>> apply(
                            @NonNull List<DataPoint> dataPoints) throws Exception {
                        return filterDataPoints(dataPoints, filter);
                    }
                });

    }

    private Observable<List<DataPoint>> filterDataPoints(@NonNull List<DataPoint> dataPoints,
            @NonNull final String filter) {
        return Observable
                .fromIterable(dataPoints)
                .filter(new Predicate<DataPoint>() {
                    @Override
                    public boolean test(@NonNull DataPoint dataPoint) throws Exception {
                        String name = dataPoint.getName();
                        String id = dataPoint.getId();
                        return (fieldContains(name, filter)
                                || fieldContains(id, filter));
                    }
                })
                .toList()
                .toObservable();
    }

    private boolean fieldContains(@Nullable String field, @NonNull String filter) {
        if (field != null) {
            field = field.toLowerCase();
        }
        return field != null && field
                .contains(filter.toLowerCase());
    }
}
