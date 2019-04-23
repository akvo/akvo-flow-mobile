/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

import androidx.annotation.NonNull;

import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.entity.DownloadResult;
import org.akvo.flow.domain.interactor.DefaultFlowableObserver;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.DownloadDataPoints;
import org.akvo.flow.domain.interactor.ErrorComposable;
import org.akvo.flow.domain.interactor.GetSavedDataPoints;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.util.Constants;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.datapoints.map.entity.MapDataPoint;
import org.akvo.flow.presentation.datapoints.map.entity.MapDataPointMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

import static org.akvo.flow.domain.entity.DownloadResult.ResultCode.SUCCESS;

public class DataPointsMapPresenter implements Presenter {

    private final MapDataPointMapper mapper;
    private final DownloadDataPoints downloadDataPoints;
    private final UseCase getSavedDataPoints;
    private final UseCase checkDeviceNotification;
    private final UseCase upload;

    private DataPointsMapView view;
    private SurveyGroup surveyGroup;

    @Inject DataPointsMapPresenter(@Named("getSavedDataPoints") UseCase getSavedDataPoints,
            MapDataPointMapper mapper, DownloadDataPoints downloadDataPoints,
            @Named("checkDeviceNotification") UseCase checkDeviceNotification,
            @Named("uploadSync") UseCase upload) {
        this.getSavedDataPoints = getSavedDataPoints;
        this.mapper = mapper;
        this.downloadDataPoints = downloadDataPoints;
        this.checkDeviceNotification = checkDeviceNotification;
        this.upload = upload;
    }

    void setView(@NonNull DataPointsMapView view) {
        this.view = view;
    }

    void onDataReady(SurveyGroup surveyGroup) {
        this.surveyGroup = surveyGroup;
        if (surveyGroup == null) {
            view.hideMenu();
        } else {
            if (surveyGroup.isMonitored()) {
                view.showMonitoredMenu();
            } else {
                view.showNonMonitoredMenu();
            }
        }
    }

    void onViewReady() {
        loadDataPoints();
    }

    void loadDataPoints() {
        getSavedDataPoints.dispose();
        if (surveyGroup != null) {
            Map<String, Object> params = new HashMap<>(2);
            params.put(GetSavedDataPoints.KEY_SURVEY_GROUP_ID, surveyGroup.getId());
            getSavedDataPoints.execute(new DefaultObserver<List<DataPoint>>() {
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
        getSavedDataPoints.dispose();
        downloadDataPoints.dispose();
        checkDeviceNotification.dispose();
        upload.dispose();
    }

    public void onNewSurveySelected(SurveyGroup surveyGroup) {
        getSavedDataPoints.dispose();
        downloadDataPoints.dispose();
        view.hideProgress();
        onDataReady(surveyGroup);
        loadDataPoints();
    }

    void onSyncRecordsPressed() {
        if (surveyGroup != null) {
            view.showProgress();
            syncRecords(surveyGroup.getId());
        }
    }

    private void syncRecords(final long surveyGroupId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(DownloadDataPoints.KEY_SURVEY_GROUP_ID, surveyGroupId);
        downloadDataPoints.execute(new DefaultFlowableObserver<DownloadResult>() {
            @Override
            public void onComplete() {
                view.hideProgress();
            }

            @Override
            public void onNext(DownloadResult result) {
                Timber.d("onNext datapoint sync: synced : %d", result.getNumberOfSyncedItems());

                if (result.getResultCode() == SUCCESS) {
                    if (result.getNumberOfSyncedItems() > 0) {
                        view.showSyncedResults(result.getNumberOfSyncedItems());
                    } else {
                        view.showNoDataPointsToSync();
                    }
                } else {
                    switch (result.getResultCode()) {
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
        }, new ErrorComposable() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error syncing %s", surveyGroupId);
                view.hideProgress();
                view.showErrorSync();
            }
        }, params);
    }

    public void onUploadPressed() {
        if (surveyGroup != null) {
            view.showProgress();
            final Map<String, Object> params = new HashMap<>(2);
            params.put(Constants.KEY_SURVEY_ID, surveyGroup.getId() + "");
            checkDeviceNotification.execute(new DefaultObserver<Set<String>>() {
                @Override
                public void onError(Throwable e) {
                    Timber.e(e);
                    uploadDataPoints(params);
                }

                @Override
                public void onNext(Set<String> ignored) {
                    uploadDataPoints(params);
                }
            }, params);
        }
    }

    private void uploadDataPoints(Map<String, Object> params) {
        upload.execute(new DefaultObserver<Set<String>>() {
            @Override
            public void onError(Throwable e) {
                view.hideProgress();
                Timber.e(e);
            }

            @Override
            public void onComplete() {
                view.hideProgress();
            }
        }, params);
    }
}
