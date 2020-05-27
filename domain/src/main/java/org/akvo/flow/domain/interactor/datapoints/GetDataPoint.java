/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.SchedulerCreator;
import org.akvo.flow.domain.repository.SurveyRepository;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;

public class GetDataPoint {

    public static final String PARAM_DATA_POINT_ID = "data_point_id";

    private final SurveyRepository surveyRepository;
    private final SchedulerCreator schedulerCreator;
    private final PostExecutionThread postExecutionThread;
    private final CompositeDisposable disposables;

    @Inject
    public GetDataPoint(SurveyRepository surveyRepository,
                        SchedulerCreator schedulerCreator,
            PostExecutionThread postExecutionThread) {
        this.surveyRepository = surveyRepository;
        this.schedulerCreator = schedulerCreator;
        this.postExecutionThread = postExecutionThread;
        this.disposables = new CompositeDisposable();
    }

    protected <T> Single buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || !parameters.containsKey(PARAM_DATA_POINT_ID)) {
            return Single.error(new IllegalArgumentException("Missing dataPointId"));
        }
        String dataPointId = (String) parameters.get(PARAM_DATA_POINT_ID);
        return surveyRepository.getDataPoint(dataPointId);
    }

    @SuppressWarnings("unchecked")
    public <T> void execute(DisposableSingleObserver<T> observer, Map<String, Object> parameters) {
        final Single<T> observable = buildUseCaseObservable(parameters)
                .subscribeOn(schedulerCreator.obtainScheduler())
                .observeOn(postExecutionThread.getScheduler());
        addDisposable(observable.subscribeWith(observer));
    }

    public void dispose() {
        if (!disposables.isDisposed()) {
            disposables.clear();
        }
    }

    private void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }
}
