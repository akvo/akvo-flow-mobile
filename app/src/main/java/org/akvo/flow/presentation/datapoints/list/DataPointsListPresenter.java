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

package org.akvo.flow.presentation.datapoints.list;

import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.entity.DownloadResult;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.DownloadDataPoints;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.interactor.datapoints.GetSavedDataPoints;
import org.akvo.flow.domain.util.Constants;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPoint;
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPointMapper;
import org.akvo.flow.util.ConstantUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import io.reactivex.observers.DisposableSingleObserver;
import timber.log.Timber;

import static org.akvo.flow.domain.entity.DownloadResult.ResultCode.SUCCESS;

public class DataPointsListPresenter implements Presenter {

    private final UseCase getSavedDataPoints;
    private final DownloadDataPoints downloadDataPoints;
    private final UseCase checkDeviceNotification;
    private final UseCase upload;
    private final ListDataPointMapper mapper;

    private DataPointsListView view;
    private SurveyGroup surveyGroup;
    private int orderBy = ConstantUtil.ORDER_BY_DATE;
    private Double latitude;
    private Double longitude;

    @Inject DataPointsListPresenter(@Named("getSavedDataPoints") UseCase getSavedDataPoints,
            ListDataPointMapper mapper, DownloadDataPoints downloadDataPoints,
            @Named("checkDeviceNotification") UseCase checkDeviceNotification,
            @Named("uploadSync") UseCase upload) {
        this.getSavedDataPoints = getSavedDataPoints;
        this.mapper = mapper;
        this.downloadDataPoints = downloadDataPoints;
        this.checkDeviceNotification = checkDeviceNotification;
        this.upload = upload;
    }

    public void setView(@NonNull DataPointsListView view) {
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

    void loadDataPoints() {
        getSavedDataPoints.dispose();
        if (surveyGroup != null) {
            Map<String, Object> params = new HashMap<>(8);
            params.put(GetSavedDataPoints.KEY_SURVEY_GROUP_ID, surveyGroup.getId());
            params.put(GetSavedDataPoints.KEY_ORDER_BY, orderBy);
            params.put(GetSavedDataPoints.KEY_LATITUDE, latitude);
            params.put(GetSavedDataPoints.KEY_LONGITUDE, longitude);
            getSavedDataPoints.execute(new DefaultObserver<List<DataPoint>>() {

                @Override
                public void onError(Throwable e) {
                    Timber.e(e, "Error loading saved datapoints");
                    view.displayData(Collections.EMPTY_LIST);
                    view.showNoDataPoints(surveyGroup.isMonitored());
                }

                @Override
                public void onNext(List<DataPoint> dataPoints) {
                    List<ListDataPoint> mapDataPoints = mapper.transform(dataPoints);
                    view.displayData(mapDataPoints);
                    if (mapDataPoints.isEmpty()) {
                        view.showNoDataPoints(surveyGroup.isMonitored());
                    }
                }
            }, params);
        } else {
            noSurveySelected();
        }
    }

    void getFilteredDataPoints(String filter) {
        getSavedDataPoints.dispose();
        if (surveyGroup != null) {
            Map<String, Object> params = new HashMap<>(8);
            params.put(GetSavedDataPoints.KEY_SURVEY_GROUP_ID, surveyGroup.getId());
            params.put(GetSavedDataPoints.KEY_ORDER_BY, orderBy);
            params.put(GetSavedDataPoints.KEY_LATITUDE, latitude);
            params.put(GetSavedDataPoints.KEY_LONGITUDE, longitude);
            params.put(GetSavedDataPoints.KEY_FILTER, filter);
            getSavedDataPoints.execute(new DefaultObserver<List<DataPoint>>() {

                @Override
                public void onError(Throwable e) {
                    Timber.e(e, "Error loading saved datapoints");
                    view.displayData(Collections.EMPTY_LIST);
                    view.displayNoSearchResultsFound();
                }

                @Override
                public void onNext(List<DataPoint> dataPoints) {
                    List<ListDataPoint> listDataPoints = mapper.transform(dataPoints);
                    view.displayData(listDataPoints);
                    if (listDataPoints.isEmpty()) {
                        view.displayNoSearchResultsFound();
                    }
                }
            }, params);
        } else {
            noSurveySelected();
        }
    }

    @Override
    public void destroy() {
        getSavedDataPoints.dispose();
        downloadDataPoints.dispose();
        checkDeviceNotification.dispose();
        upload.dispose();
    }

    void onDownloadPressed() {
        if (surveyGroup != null) {
            view.showLoading();
            downloadDataPoints(surveyGroup.getId());
        }
    }

    private void downloadDataPoints(final long surveyGroupId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(DownloadDataPoints.KEY_SURVEY_ID, surveyGroupId);
        downloadDataPoints.execute(new DisposableSingleObserver<DownloadResult>() {
            @Override
            public void onSuccess(DownloadResult result) {
                view.hideLoading();
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

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error syncing %s", surveyGroupId);
                view.hideLoading();
                view.showErrorSync();
            }
        }, params);
    }

    void onOrderByClick(int order) {
        if (orderBy != order) {
            if (order == ConstantUtil.ORDER_BY_DISTANCE && (latitude == null
                    || longitude == null)) {
                // Warn user that the location is unknown
                view.showErrorMissingLocation();
                return;
            }
            this.orderBy = order;
            loadDataPoints();
        }
    }

    void onLocationReady(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    void onOrderByClicked() {
        view.showOrderByDialog(orderBy);
    }

    void onNewSurveySelected(SurveyGroup surveyGroup) {
        getSavedDataPoints.dispose();
        downloadDataPoints.dispose();
        view.hideLoading();
        onDataReady(surveyGroup);
        loadDataPoints();
    }

    private void noSurveySelected() {
        view.displayData(Collections.EMPTY_LIST);
        view.showNoSurveySelected();
    }

    public void onUploadPressed() {
        if (surveyGroup != null) {
            view.showLoading();
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
                view.hideLoading();
                Timber.e(e);
            }

            @Override
            public void onComplete() {
                view.hideLoading();
            }
        }, params);
    }
}
