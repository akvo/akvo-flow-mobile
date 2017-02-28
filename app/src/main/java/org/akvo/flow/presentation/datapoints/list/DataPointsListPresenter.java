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

package org.akvo.flow.presentation.datapoints.list;

import android.support.annotation.Nullable;

import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.entity.SyncResult;
import org.akvo.flow.domain.interactor.DefaultSubscriber;
import org.akvo.flow.domain.interactor.GetSavedDataPoints;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPoint;
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPointMapper;
import org.akvo.flow.util.ConstantUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

import static org.akvo.flow.domain.entity.SyncResult.ResultCode.SUCCESS;

public class DataPointsListPresenter implements Presenter {

    private final UseCase getSavedDataPoints;
    private final ListDataPointMapper mapper;
    private final UseCase syncDataPoints;

    @Nullable
    private DataPointsListView view;
    private SurveyGroup surveyGroup;
    private int orderBy = ConstantUtil.ORDER_BY_DATE;
    private Double latitude;
    private Double longitude;

    @Inject
    DataPointsListPresenter(@Named("getSavedDataPoints") UseCase getSavedDataPoints,
            ListDataPointMapper mapper, @Named("syncDataPoints") UseCase syncDataPoints) {
        this.getSavedDataPoints = getSavedDataPoints;
        this.mapper = mapper;
        this.syncDataPoints = syncDataPoints;
    }

    public void setView(DataPointsListView view) {
        this.view = view;
    }

    void onDataReady(SurveyGroup surveyGroup) {
        this.surveyGroup = surveyGroup;
        boolean monitored = surveyGroup == null? false : surveyGroup.isMonitored();
        view.displayMenu(monitored);
    }

    void refresh() {
        if (surveyGroup != null) {
            Map<String, Object> params = new HashMap<>(8);
            params.put(GetSavedDataPoints.KEY_SURVEY_GROUP_ID, surveyGroup.getId());
            params.put(GetSavedDataPoints.KEY_ORDER_BY, orderBy);
            params.put(GetSavedDataPoints.KEY_LATITUDE, latitude);
            params.put(GetSavedDataPoints.KEY_LONGITUDE, longitude);
            getSavedDataPoints.execute(new DefaultSubscriber<List<DataPoint>>() {
                @Override
                public void onError(Throwable e) {
                    Timber.e(e, "Error loading saved datapoints");
                }

                @Override
                public void onNext(List<DataPoint> dataPoints) {
                    List<ListDataPoint> mapDataPoints = mapper.transform(dataPoints);
                    view.displayData(mapDataPoints);
                    if (mapDataPoints.isEmpty()) {
                        view.showNoDataPoints();
                    }
                }
            }, params);
        } else {
            view.displayData(Collections.EMPTY_LIST);
            view.showNoSurveySelected();
        }
    }

    @Override
    public void destroy() {
        getSavedDataPoints.unSubscribe();
        syncDataPoints.unSubscribe();
    }

    void onSyncRecordsPressed() {
        if (surveyGroup != null) {
            view.showLoading();
            syncRecords(surveyGroup.getId());
        }
    }

    private void syncRecords(final long surveyGroupId) {
        Map<String, Long> params = new HashMap<>(2);
        params.put(GetSavedDataPoints.KEY_SURVEY_GROUP_ID, surveyGroupId);
        syncDataPoints.execute(new DefaultSubscriber<SyncResult>() {

            @Override
            public void onCompleted() {
                Timber.d("onCompleted");
                view.hideLoading();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error syncing %s", surveyGroupId);
                view.hideLoading();
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
                            view.showSyncNotAllowed();
                            break;
                        case ERROR_NO_NETWORK:
                            view.showNoNetwork();
                            break;
                        //TODO: add assignment missing
                        default:
                            view.showErrorSync();
                            break;
                    }
                }
            }
        }, params);
    }

    void onOrderByClick(int order) {
        if (orderBy != order) {
            if (orderBy == ConstantUtil.ORDER_BY_DISTANCE && (latitude == null || longitude == null)) {
                // Warn user that the location is unknown
                view.showErrorMissingLocation();
                return;
            }
            this.orderBy = order;
            refresh();
        }
    }

    void onLocationReady(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    void onOrderByClicked() {
        view.showOrderByDialog(orderBy);
    }
}
