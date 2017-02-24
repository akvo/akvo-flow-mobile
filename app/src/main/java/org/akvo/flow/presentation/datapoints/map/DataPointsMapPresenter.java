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

package org.akvo.flow.presentation.datapoints.map;

import android.support.annotation.Nullable;

import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.interactor.DefaultSubscriber;
import org.akvo.flow.domain.interactor.GetSavedDataPoints;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.datapoints.map.entity.MapDataPoint;
import org.akvo.flow.presentation.datapoints.map.entity.MapDataPointMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class DataPointsMapPresenter implements Presenter{

    private final UseCase getSavedDataPoints;
    private final MapDataPointMapper mapper;
    private final UseCase syncDataPoints;

    @Nullable
    private DataPointsMapView view;
    private SurveyGroup surveyGroup;

    @Inject
    public DataPointsMapPresenter(@Named("getSavedDataPoints") UseCase getSavedDataPoints,
            MapDataPointMapper mapper, @Named("syncDataPoints") UseCase syncDataPoints) {
        this.getSavedDataPoints = getSavedDataPoints;
        this.mapper = mapper;
        this.syncDataPoints = syncDataPoints;
    }

    public void setView(DataPointsMapView view) {
        this.view = view;
    }

    public void onDataReady(SurveyGroup surveyGroup) {
        this.surveyGroup = surveyGroup;
        if (surveyGroup != null) {
            view.displayMenu(surveyGroup.isMonitored());
        }
    }

    public void onViewReady() {
        refresh();
    }

    public void refresh() {
        view.showProgress();
        Map<String, Long> params = new HashMap<>(2);
        if (surveyGroup != null) {
            params.put(GetSavedDataPoints.KEY_SURVEY_GROUP_ID, surveyGroup.getId());
        }
        getSavedDataPoints.execute(new DefaultSubscriber<List<DataPoint>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error loading saved datapoints");
                //TODO: show error?
                view.hideProgress();
            }

            @Override
            public void onNext(List<DataPoint> dataPoints) {
                Timber.d("Found datapoints : %d "+dataPoints.size());
                List<MapDataPoint> mapDataPoints = mapper.transform(dataPoints);
                Timber.d("Datapoints with location : %d "+mapDataPoints.size());
                view.hideProgress();
                view.displayData(mapDataPoints);
            }
        }, params);
    }

    @Override
    public void onViewDestroyed() {
        getSavedDataPoints.unSubscribe();
        syncDataPoints.unSubscribe();
    }

    public void onSyncRecordsPressed() {
        if (surveyGroup != null) {
            view.showProgress();
            syncRecords(surveyGroup.getId());
        }
    }

    public void syncRecords(final long surveyGroupId) {
        Map<String, Long> params = new HashMap<>(2);
        params.put(GetSavedDataPoints.KEY_SURVEY_GROUP_ID, surveyGroupId);
        syncDataPoints.execute(new DefaultSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error syncing %s", surveyGroupId);
            }

            @Override
            public void onNext(Integer integer) {
                //TODO: show snackbar with number
            }
        }, params);
    }
}
