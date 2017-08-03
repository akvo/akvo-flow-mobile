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

import android.support.annotation.NonNull;

import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.entity.SyncResult;
import org.akvo.flow.domain.interactor.DefaultSubscriber;
import org.akvo.flow.domain.interactor.GetSavedDataPoints;
import org.akvo.flow.domain.interactor.SyncDataPoints;
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

import static org.akvo.flow.domain.entity.SyncResult.ResultCode.SUCCESS;

public class DataPointsMapPresenter implements Presenter {

    private final UseCase getSavedDataPoints;
    private final MapDataPointMapper mapper;
    private final UseCase syncDataPoints;

    private DataPointsMapView view;
    private SurveyGroup surveyGroup;

    @Inject
    DataPointsMapPresenter(@Named("getSavedDataPoints") UseCase getSavedDataPoints,
            MapDataPointMapper mapper, @Named("syncDataPoints") UseCase syncDataPoints) {
        this.getSavedDataPoints = getSavedDataPoints;
        this.mapper = mapper;
        this.syncDataPoints = syncDataPoints;
    }

    void setView(@NonNull DataPointsMapView view) {
        this.view = view;
    }

    void onDataReady(SurveyGroup surveyGroup) {
        this.surveyGroup = surveyGroup;
        if (surveyGroup != null) {
            view.displayMenu(surveyGroup.isMonitored());
        }
    }

    void onViewReady() {
        loadDataPoints();
    }

    void loadDataPoints() {
        if (surveyGroup != null) {
            Map<String, Long> params = new HashMap<>(2);
            params.put(GetSavedDataPoints.KEY_SURVEY_GROUP_ID, surveyGroup.getId());
            getSavedDataPoints.execute(new DefaultSubscriber<List<DataPoint>>() {
                @Override
                public void onError(Throwable e) {
                    Timber.e(e, "Error loading saved datapoints");
                }

                @Override
                public void onNext(List<DataPoint> dataPoints) {
                    List<MapDataPoint> mapDataPoints = mapper.transform(dataPoints);
                    view.displayData(mapDataPoints);
                }
            }, params);
        }
    }

    @Override
    public void destroy() {
        getSavedDataPoints.unSubscribe();
        syncDataPoints.unSubscribe();
    }

    void onSyncRecordsPressed() {
        if (surveyGroup != null) {
            view.showProgress();
            syncRecords(surveyGroup.getId());
        }
    }

    private void syncRecords(final long surveyGroupId) {
        Map<String, Long> params = new HashMap<>(2);
        params.put(SyncDataPoints.KEY_SURVEY_GROUP_ID, surveyGroupId);
        syncDataPoints.execute(new DefaultSubscriber<SyncResult>() {

            @Override
            public void onCompleted() {
                view.hideProgress();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error syncing %s", surveyGroupId);
                view.hideProgress();
                view.showErrorSync();
            }

            @Override
            public void onNext(SyncResult result) {
                Timber.d("onNext datapoint sync: synced : %d", result.getNumberOfSyncedItems());

                if (result.getResultCode() == SUCCESS) {
                    if (result.getNumberOfSyncedItems() > 0) {
                        view.showSyncedResults(result.getNumberOfSyncedItems());
                    }
                } else {
                    switch (result.getResultCode()) {
                        case ERROR_SYNC_NOT_ALLOWED_OVER_3G:
                            view.showErrorSyncNotAllowed();
                            break;
                        case ERROR_NO_NETWORK:
                            view.showErrorNoNetwork();
                            break;
                        case ERROR_ASSIGNMENT_MISSING:
                            view.showErrorAssignmentMissing();
                            break;
                        default:
                            view.showErrorSync();
                            break;
                    }
                }
            }
        }, params);
    }

    public void onNewSurveySelected(SurveyGroup surveyGroup) {
        getSavedDataPoints.unSubscribe();
        syncDataPoints.unSubscribe();
        view.hideProgress();
        onDataReady(surveyGroup);
        loadDataPoints();
    }
}
